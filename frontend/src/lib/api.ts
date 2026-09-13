export interface ApiErrorData {
  status: number;
  title: string;
  detail: string;
  requestId: string | null;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LibraryOption {
  id: number;
  name: string;
}

export class ApiError extends Error {
  status: number;
  requestId: string | null;

  constructor(status: number, _title: string, detail: string, requestId: string | null) {
    super(detail);
    this.name = 'ApiError';
    this.status = status;
    this.requestId = requestId;
  }
}

function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(new RegExp('(^|;\\s*)(' + name + ')=([^;]*)'));
  return match && match[3] ? decodeURIComponent(match[3]) : null;
}

async function readError(res: Response): Promise<ApiError> {
  let title = 'Đã có lỗi xảy ra';
  let detail = `Lỗi ${res.status}`;
  let requestId: string | null = res.headers.get('X-Request-ID');
  try {
    const data = (await res.json()) as { title?: string; detail?: string; requestId?: string };
    if (typeof data.title === 'string' && data.title) title = data.title;
    if (typeof data.detail === 'string' && data.detail) detail = data.detail;
    if (typeof data.requestId === 'string' && data.requestId) requestId = data.requestId;
  } catch {
    // non-JSON error body; keep defaults
  }
  return new ApiError(res.status, title, detail, requestId);
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  signal?: AbortSignal;
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  const hasJson = options.body !== undefined;
  if (hasJson) headers['Content-Type'] = 'application/json';
  if (options.method && options.method !== 'GET') {
    let token = getCookie('XSRF-TOKEN');
    if (!token) {
      const csrfRes = await fetch('/api/v1/auth/csrf', { cache: 'no-store' });
      if (csrfRes.ok) {
        const data = (await csrfRes.json()) as { token?: string };
        token = data.token ?? null;
      }
    }
    if (token) headers['X-XSRF-TOKEN'] = token;
  }

  const res = await fetch(path, {
    method: options.method,
    headers,
    body: hasJson ? JSON.stringify(options.body) : undefined,
    cache: 'no-store',
    signal: options.signal,
  });

  if (!res.ok) throw await readError(res);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export function parsePage<T>(payload: unknown): Page<T> {
  const p = payload as Record<string, unknown>;
  const items = (p.content ?? p.items ?? []) as T[];
  const pageable = (p.pageable as Record<string, unknown> | undefined) ?? {};
  const size = (p.size as number) ?? (pageable.pageSize as number) ?? items.length;
  const page = (p.number as number) ?? (pageable.pageNumber as number) ?? 0;
  return {
    items,
    page,
    size,
    totalElements: (p.totalElements as number) ?? items.length,
    totalPages: (p.totalPages as number) ?? Math.max(1, Math.ceil(((p.totalElements as number) ?? items.length) / Math.max(1, size))),
  };
}

export function buildPageQuery(page: number, size: number, extra?: Record<string, string | number | undefined>): string {
  const params = new URLSearchParams();
  params.set('page', String(page));
  params.set('size', String(size));
  if (extra) {
    for (const [key, value] of Object.entries(extra)) {
      if (value !== undefined && value !== '') params.set(key, String(value));
    }
  }
  return params.toString();
}

export async function fetchLibraries(signal?: AbortSignal): Promise<LibraryOption[]> {
  const payload = await apiFetch<unknown>(`/api/v1/libraries?${buildPageQuery(0, 100)}`, { signal });
  return parsePage<LibraryOption>(payload).items;
}

export function formatVnd(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === '') return '—';
  const n = Number(value);
  if (!Number.isFinite(n)) return '—';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    minimumFractionDigits: 0,
  }).format(n);
}

export function formatInstant(value: string | null | undefined): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString('vi-VN', { dateStyle: 'medium', timeStyle: 'short' });
}
