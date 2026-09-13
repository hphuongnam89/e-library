export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  requestId?: string;
}

export class ApiError extends Error {
  status: number;
  problem?: ProblemDetail;

  constructor(status: number, message: string, problem?: ProblemDetail) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
  }
}

function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(new RegExp('(^|;\\s*)(' + name + ')=([^;]*)'));
  return match && match[3] ? decodeURIComponent(match[3]) : null;
}

export async function apiFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method || 'GET').toUpperCase();
  const headers = new Headers(options.headers || {});

  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }

  // Auto attach CSRF token for unsafe methods
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
    let xsrf = getCookie('XSRF-TOKEN');
    if (!xsrf) {
      try {
        const csrfRes = await fetch('/api/v1/auth/csrf', { cache: 'no-store' });
        if (csrfRes.ok) {
          const data = await csrfRes.json();
          xsrf = data.token;
        }
      } catch {
        // Ignore CSRF fetch error; request will proceed and fail naturally if rejected
      }
    }
    if (xsrf && !headers.has('X-XSRF-TOKEN')) {
      headers.set('X-XSRF-TOKEN', xsrf);
    }
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let problem: ProblemDetail | undefined;
    let message = `Yêu cầu thất bại với mã lỗi HTTP ${response.status}`;
    try {
      problem = await response.json();
      if (problem?.detail) {
        message = problem.detail;
      } else if (problem?.title) {
        message = problem.title;
      }
    } catch {
      // Body is not JSON
    }

    if (response.status === 401) {
      message = 'Vui lòng đăng nhập để thực hiện chức năng này.';
    } else if (response.status === 403) {
      message = 'Bạn không có quyền truy cập chức năng này.';
    }

    throw new ApiError(response.status, message, problem);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json() as Promise<T>;
}
