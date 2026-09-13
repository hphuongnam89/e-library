import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch, ApiError } from './client';

describe('api client', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    Object.defineProperty(document, 'cookie', {
      writable: true,
      value: 'XSRF-TOKEN=test-token-123',
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('performs GET request successfully', async () => {
    const mockData = { id: 1, title: 'Test Book' };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify(mockData), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    );

    const result = await apiFetch<typeof mockData>('/api/v1/test');
    expect(result).toEqual(mockData);
  });

  it('returns empty object on 204 No Content', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(null, { status: 204 })
    );

    const result = await apiFetch('/api/v1/test', { method: 'DELETE' });
    expect(result).toEqual({});
  });

  it('attaches CSRF token from cookie on POST', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify({ ok: true }), { status: 200 })
    );

    await apiFetch('/api/v1/test', {
      method: 'POST',
      body: JSON.stringify({ name: 'abc' }),
    });

    expect(fetchSpy).toHaveBeenCalledWith(
      '/api/v1/test',
      expect.objectContaining({
        headers: expect.any(Headers),
      })
    );

    const callHeaders = fetchSpy.mock.calls[0]?.[1]?.headers as Headers | undefined;
    expect(callHeaders?.get('X-XSRF-TOKEN')).toBe('test-token-123');
  });

  it('fetches CSRF token if cookie is missing on POST', async () => {
    Object.defineProperty(document, 'cookie', {
      writable: true,
      value: '',
    });

    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input: RequestInfo | URL) => {
      if (String(input).includes('/api/v1/auth/csrf')) {
        return new Response(JSON.stringify({ token: 'fetched-csrf-token' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        });
      }
      return new Response(JSON.stringify({ saved: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      });
    });

    const res = await apiFetch<{ saved: boolean }>('/api/v1/test', { method: 'POST' });
    expect(res).toEqual({ saved: true });
    expect(fetchSpy).toHaveBeenCalledTimes(2);
    expect(fetchSpy).toHaveBeenNthCalledWith(1, '/api/v1/auth/csrf', { cache: 'no-store' });
  });

  it('throws ApiError with detail message on 400 Bad Request', async () => {
    const problem = {
      status: 400,
      title: 'Bad Request',
      detail: 'Dữ liệu không hợp lệ',
    };

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify(problem), {
        status: 400,
        headers: { 'Content-Type': 'application/problem+json' },
      })
    );

    await expect(apiFetch('/api/v1/test')).rejects.toThrow(ApiError);
  });

  it('handles 401 Unauthorized with custom friendly message', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(null, { status: 401 })
    );

    await expect(apiFetch('/api/v1/test')).rejects.toThrow(
      'Vui lòng đăng nhập để thực hiện chức năng này.'
    );
  });

  it('handles 403 Forbidden with custom friendly message', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(null, { status: 403 })
    );

    await expect(apiFetch('/api/v1/test')).rejects.toThrow(
      'Bạn không có quyền truy cập chức năng này.'
    );
  });
});
