# E-LIB — Hệ thống Giám sát & Cảnh báo (Monitoring & Observability)

Tài liệu thiết kế và vận hành hệ thống giám sát, thu thập chỉ số (telemetry), thiết lập cảnh báo và quy trình xử lý sự cố cho nền tảng E-LIB.

---

## 1. Kiến trúc Giám sát (Observability Architecture)

Hệ thống kết hợp ba trụ cột quan sát:
1. **Metrics (Chỉ số thời gian thực)**:
   - Thu thập qua Spring Boot Actuator `/api/system/prometheus` (Micrometer Prometheus Registry).
   - Prometheus server định kỳ cào dữ liệu (scrape interval: 15s) từ các instances backend.
2. **Probes & Health Checks (Kiểm tra sống còn & sẵn sàng)**:
   - Liveness Probe: `GET /api/system/health/liveness` (Xác nhận tiến trình JVM đang hoạt động).
   - Readiness Probe: `GET /api/system/health/readiness` (Xác nhận kết nối Database PostgreSQL và Redis Cache đang thông suốt).
3. **Structured Logging & Tracing (Nhật ký có cấu trúc)**:
   - Tích hợp `RequestIdFilter` phát hành `X-Request-ID` cho mọi HTTP request.
   - Gắn `MDC` correlation ID xuyên suốt tầng Controller → Service → Repository → Async Job.
   - Nhật ký định dạng JSON, không ghi lộ thông tin nhạy cảm (mật khẩu, token, dữ liệu người dùng).

---

## 2. Cấu hình Prometheus Scraper

Mẫu cấu hình `prometheus.yml` để thu thập dữ liệu từ cụm dịch vụ E-LIB:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'elib-backend'
    metrics_path: '/api/system/prometheus'
    scrape_interval: 10s
    static_configs:
      - targets: ['backend:8080']
        labels:
          application: 'elib'
          environment: 'production'

  - job_name: 'elib-nginx'
    scrape_interval: 15s
    static_configs:
      - targets: ['nginx-proxy:8080']

  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
```

---

## 3. Các Chỉ số Vận hành Trọng yếu (Golden Signals)

| Nhóm chỉ số | Metric Name (Prometheus) | Ý nghĩa vận hành | Ngưỡng an toàn |
|---|---|---|---|
| **Lưu lượng (Traffic)** | `http_server_requests_seconds_count` | Tổng số lượt gọi API theo endpoint và phương thức | Theo biến thiên tải |
| **Độ trễ (Latency P95)** | `http_server_requests_seconds{quantile="0.95"}` | Thời gian phản hồi 95% requests | **< 2.0 giây** |
| **Tỷ lệ lỗi (Errors)** | `rate(http_server_requests_seconds_count{status=~"5.."}[5m])` | Tỷ lệ lỗi máy chủ HTTP 5xx | **< 1.0%** |
| **DB Pool HikariCP** | `hikaricp_connections_active` / `hikaricp_connections_max` | Tỷ lệ sử dụng connection pool PostgreSQL | **< 80%** |
| **DB Connection Wait** | `hikaricp_connections_pending` | Số luồng đang chờ mượn connection từ pool | **= 0** |
| **Redis Cache Hit** | `cache_gets_total{result="hit"} / cache_gets_total` | Tỷ lệ trúng cache của danh mục & cài đặt | **≥ 85%** |
| **Bộ nhớ JVM Heap** | `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes` | Tỷ lệ sử dụng bộ nhớ Heap | **< 85%** |
| **Dung lượng Đĩa** | `node_filesystem_avail_bytes / node_filesystem_size_bytes` | Dung lượng ổ đĩa còn trống của server | **≥ 20%** |

---

## 4. Quy tắc Cảnh báo (Prometheus Alert Rules)

```yaml
groups:
  - name: elib_critical_alerts
    rules:
      - alert: ElibServiceDown
        expr: up{job="elib-backend"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "E-LIB Backend instance is down"
          description: "Instance {{ $labels.instance }} has been unreachable for more than 1 minute."

      - alert: HighHttp5xxErrorRate
        expr: (sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))) * 100 > 1.0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High HTTP 5xx Error Rate (> 1%)"
          description: "Current error rate is {{ $value }}% for the last 5 minutes."

      - alert: HighLatencyP95Exceeded
        expr: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) > 2.0
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "P95 Response Time exceeded 2.0 seconds"
          description: "P95 latency is {{ $value }}s, breaching SLA gate."

      - alert: HikariCpPoolExhaustion
        expr: hikaricp_connections_pending > 5
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "HikariCP Database connection pool starved"
          description: "{{ $value }} threads waiting for a free DB connection."
```

---

## 5. Quy trình Ứng phó Sự cố (Incident Response Playbook)

### Phân cấp Mức độ Nghiêm trọng (Severity Levels)
1. **P1 — Critical (Khẩn cấp)**: Toàn bộ hệ thống sập, mất khả năng phục vụ mượn/trả sách, rò rỉ dữ liệu hoặc hỏng hóc cơ sở dữ liệu.
   - *Thời gian phản hồi (MTTA)*: < 15 phút.
   - *Mục tiêu khắc phục (MTTR)*: < 1 giờ.
2. **P2 — Major (Nghiêm trọng)**: Một phân hệ bị gián đoạn (ví dụ: không đọc được tài liệu PDF hoặc email nhắc hạn không gửi được), nhưng các tính năng cốt lõi khác vẫn hoạt động.
   - *MTTA*: < 30 phút.
   - *MTTR*: < 4 giờ.
3. **P3 — Minor (Nhẹ)**: Lỗi giao diện nhỏ, độ trễ tăng nhẹ không ảnh hưởng nghiêm trọng đến trải nghiệm.

### Các Bước Xử lý Khi Nhận Cảnh báo P1:
1. **Kiểm tra Probes**:
   ```bash
   curl -I http://localhost:8080/api/system/health/liveness
   curl -I http://localhost:8080/api/system/health/readiness
   ```
2. **Kiểm tra Container Status & Logs**:
   ```bash
   docker compose ps
   docker compose logs --tail=100 --timestamps backend
   docker compose logs --tail=50 db
   ```
3. **Cô lập và Khôi phục Khẩn cấp**:
   - Nếu nghẽn DB Connection Pool: Khởi động lại backend hoặc tăng tạm thời `maximum-pool-size` lên 20.
   - Nếu PostgreSQL lỗi dữ liệu hoặc crash: Kích hoạt quy trình khôi phục theo kịch bản [`scripts/restore.sh`](../scripts/restore.sh).
