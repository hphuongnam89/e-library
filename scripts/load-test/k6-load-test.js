import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// Custom metrics to measure the 4 SLA targets from testing-strategy.md
const catalogBrowseDuration = new Trend('catalog_browse_duration');
const pdfStreamDuration = new Trend('pdf_stream_duration');
const barcodeScanDuration = new Trend('barcode_scan_duration');
const ftsSearchDuration = new Trend('fts_search_duration');
const failureRate = new Rate('custom_failure_rate');

export const options = {
  stages: [
    { duration: '20s', target: 50 },   // Warm-up ramp
    { duration: '30s', target: 200 },  // Ramp to 200 concurrent users
    { duration: '1m', target: 500 },   // Stress ramp to 500 concurrent users
    { duration: '1m', target: 500 },   // Sustain at peak 500 users
    { duration: '20s', target: 0 },    // Cool down
  ],
  thresholds: {
    // SLA Gate 1: Page / Catalog browse < 2s (P95) under 500 concurrent users
    'catalog_browse_duration': ['p(95)<2000'],
    // SLA Gate 2: PDF reader / stream metadata < 3s (P95)
    'pdf_stream_duration': ['p(95)<3000'],
    // SLA Gate 3: Barcode scan response < 500ms (P95)
    'barcode_scan_duration': ['p(95)<500'],
    // SLA Gate 4: PostgreSQL FTS query < 200ms (P95)
    'fts_search_duration': ['p(95)<200'],
    // General SLA: HTTP failure rate under 1%
    'http_req_failed': ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const headers = {
    'Accept': 'application/json',
    'User-Agent': 'k6-load-test/1.0 (E-LIB Performance Benchmark)',
  };

  // Scenario 1: Catalog Browse & Category Navigation (< 2s P95)
  group('Catalog Browse', function () {
    const res = http.get(`${BASE_URL}/api/v1/categories/roots`, { headers });
    catalogBrowseDuration.add(res.timings.duration);
    const passed = check(res, {
      'catalog status is 200': (r) => r.status === 200 || r.status === 304,
      'catalog response under 2s': (r) => r.timings.duration < 2000,
    });
    failureRate.add(!passed);
  });

  // Scenario 2: Full-Text Search (FTS) Query (< 200ms P95)
  group('Full-Text Search (FTS)', function () {
    const searchTerms = ['lap+trinh', 'khoa+hoc', 'giao+trinh', 'cong+nghe', 'database'];
    const term = searchTerms[Math.floor(Math.random() * searchTerms.length)];
    const res = http.get(`${BASE_URL}/api/v1/book-titles?query=${term}&page=0&size=10`, { headers });
    ftsSearchDuration.add(res.timings.duration);
    const passed = check(res, {
      'fts status is 200': (r) => r.status === 200,
      'fts query under 200ms': (r) => r.timings.duration < 200,
    });
    failureRate.add(!passed);
  });

  // Scenario 3: Barcode Scan Response (< 500ms P95)
  group('Barcode Scan Lookup', function () {
    const barcodes = ['PXU-000001', 'PXU-000002', 'PXU-000003', 'LIB-000001'];
    const barcode = barcodes[Math.floor(Math.random() * barcodes.length)];
    const res = http.get(`${BASE_URL}/api/v1/book-copies?barcode=${barcode}`, { headers });
    barcodeScanDuration.add(res.timings.duration);
    const passed = check(res, {
      'barcode lookup response received': (r) => r.status === 200 || r.status === 404,
      'barcode scan under 500ms': (r) => r.timings.duration < 500,
    });
    failureRate.add(!passed);
  });

  // Scenario 4: PDF Reader Streaming / Metadata (< 3s P95)
  group('PDF Reader Streaming', function () {
    const streamHeaders = {
      ...headers,
      'Range': 'bytes=0-65535', // Simulate initial byte-range chunk fetch
    };
    const res = http.get(`${BASE_URL}/api/v1/digital-documents/1/stream`, {
      headers: streamHeaders,
    });
    pdfStreamDuration.add(res.timings.duration);
    const passed = check(res, {
      'pdf stream response received': (r) => r.status === 206 || r.status === 200 || r.status === 401 || r.status === 404,
      'pdf stream latency under 3s': (r) => r.timings.duration < 3000,
    });
    failureRate.add(!passed);
  });

  sleep(Math.random() * 0.5 + 0.2); // Random think time between 200ms - 700ms
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'scripts/load-test/k6-summary.json': JSON.stringify(data, null, 2),
  };
}

function textSummary(data) {
  let out = '\n================= E-LIB LOAD TEST BENCHMARK REPORT =================\n';
  const metrics = [
    { key: 'catalog_browse_duration', label: '1. Catalog Browse (Target: P95 < 2000ms)' },
    { key: 'pdf_stream_duration', label: '2. PDF Stream / Reader (Target: P95 < 3000ms)' },
    { key: 'barcode_scan_duration', label: '3. Barcode Scan Lookup (Target: P95 < 500ms)' },
    { key: 'fts_search_duration', label: '4. Full-Text Search (FTS) (Target: P95 < 200ms)' },
  ];

  metrics.forEach(({ key, label }) => {
    const m = data.metrics[key];
    if (m && m.values) {
      out += `\n${label}:\n`;
      out += `  - avg: ${(m.values.avg || 0).toFixed(2)}ms\n`;
      out += `  - p(90): ${(m.values['p(90)'] || 0).toFixed(2)}ms\n`;
      out += `  - p(95): ${(m.values['p(95)'] || 0).toFixed(2)}ms\n`;
      out += `  - p(99): ${(m.values['p(99)'] || 0).toFixed(2)}ms\n`;
      out += `  - max: ${(m.values.max || 0).toFixed(2)}ms\n`;
    }
  });
  out += '\n====================================================================\n';
  return out;
}
