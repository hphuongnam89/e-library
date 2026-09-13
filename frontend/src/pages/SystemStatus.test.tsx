import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { SystemStatus } from './SystemStatus';

const response = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });
describe('System status', () => {
  it('shows loading until a real response arrives', async () => {
    let resolve!: (value: Response) => void;
    vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(done => { resolve = done; })));
    render(<SystemStatus />);
    expect(screen.getByRole('status')).toHaveTextContent('Đang kiểm tra');
    expect(screen.getByRole('button')).toBeDisabled();
    await act(async () => { resolve(response({ status: 'UP' })); });
    expect(screen.getByRole('status')).toHaveTextContent('Dịch vụ nền tảng sẵn sàng');
  });
  it.each([
    ['unavailable service', { status: 'DOWN' }, 503],
    ['unexpected payload', { hello: 'world' }, 200],
    ['empty response', null, 200],
    ['failure carrying UP', { status: 'UP' }, 500],
  ])('never claims ready for %s', async (_label, body, status) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response(body, status)));
    render(<SystemStatus />);
    expect(await screen.findByText('Chưa kết nối được dịch vụ')).toBeInTheDocument();
  });
  it('recovers from a network error after retry', async () => {
    const fetcher = vi.fn().mockRejectedValueOnce(new TypeError('Network error'))
      .mockResolvedValueOnce(response({ status: 'UP' }));
    vi.stubGlobal('fetch', fetcher);
    render(<SystemStatus />);
    await screen.findByText('Chưa kết nối được dịch vụ');
    await userEvent.click(screen.getByRole('button', { name: 'Kiểm tra lại' }));
    expect(await screen.findByText('Dịch vụ nền tảng sẵn sàng')).toBeInTheDocument();
    expect(fetcher).toHaveBeenLastCalledWith('/api/system/health/readiness', expect.objectContaining({ cache: 'no-store' }));
  });
  it('aborts a hung request after eight seconds and shows an error', async () => {
    vi.useFakeTimers();
    vi.stubGlobal('fetch', vi.fn((_url, options: RequestInit) => new Promise((_resolve, reject) => {
      options.signal?.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')));
    })));
    render(<SystemStatus />);
    await act(async () => { await vi.advanceTimersByTimeAsync(8000); });
    expect(screen.getByRole('status')).toHaveTextContent('Chưa kết nối được dịch vụ');
    expect(screen.getByRole('button')).toBeEnabled();
  });
  it('cancels the request when navigating away', () => {
    let signal: AbortSignal | null | undefined;
    vi.stubGlobal('fetch', vi.fn((_url, options: RequestInit) => {
      signal = options.signal;
      return new Promise(() => {});
    }));
    const view = render(<SystemStatus />);
    view.unmount();
    expect(signal?.aborted).toBe(true);
  });
});
