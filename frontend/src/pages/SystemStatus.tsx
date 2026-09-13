import { useEffect, useState } from 'react';
type Check = { state: 'loading' } | { state: 'ready' | 'error'; checkedAt: Date };
export function SystemStatus() {
  const [check, setCheck] = useState<Check>({ state: 'loading' });
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    const timeout = window.setTimeout(() => controller.abort(), 8000);
    setCheck({ state: 'loading' });
    async function load() {
      try {
        const response = await fetch('/api/system/health/readiness', {
          signal: controller.signal, cache: 'no-store', headers: { Accept: 'application/json' },
        });
        const data: unknown = await response.json();
        if (!response.ok || typeof data !== 'object' || data === null || !('status' in data) || data.status !== 'UP') throw new Error('Service is not ready');
        if (active) setCheck({ state: 'ready', checkedAt: new Date() });
      } catch {
        if (active) setCheck({ state: 'error', checkedAt: new Date() });
      } finally { window.clearTimeout(timeout); }
    }
    void load();
    return () => { active = false; window.clearTimeout(timeout); controller.abort(); };
  }, [attempt]);
  return <section className="max-w-2xl py-12 sm:py-16">
    <p className="text-sm font-semibold uppercase tracking-widest text-blue-700">Tình trạng dịch vụ</p>
    <h1 className="mt-4 text-3xl font-semibold tracking-tight sm:text-4xl">Trạng thái hệ thống</h1>
    <p className="mt-4 leading-7 text-slate-600">Kiểm tra kết nối tới dịch vụ thư viện tại thời điểm hiện tại.</p>
    <div className="mt-8 rounded-2xl border border-slate-200 bg-white p-6 sm:p-8">
      <div role="status" aria-live="polite" aria-atomic="true">
        <div className="flex items-center gap-3">
          <span aria-hidden="true" className={`h-3 w-3 shrink-0 rounded-full ${check.state === 'ready' ? 'bg-emerald-600' : check.state === 'error' ? 'bg-amber-600' : 'bg-slate-400'}`} />
          <h2 className="text-xl font-semibold">{check.state === 'loading' ? 'Đang kiểm tra kết nối…' : check.state === 'ready' ? 'Dịch vụ nền tảng sẵn sàng' : 'Chưa kết nối được dịch vụ'}</h2>
        </div>
        <p className="mt-4 leading-7 text-slate-600">{check.state === 'ready' ? 'Kiểm tra kết nối thành công. Các chức năng thư viện đang tiếp tục được phát triển.' : check.state === 'error' ? 'Dịch vụ có thể đang khởi động hoặc tạm gián đoạn. Vui lòng thử lại sau.' : 'Vui lòng chờ trong giây lát.'}</p>
        {check.state !== 'loading' && <p className="mt-3 text-sm text-slate-500">Lần kiểm tra gần nhất: {check.checkedAt.toLocaleTimeString('vi-VN')}</p>}
      </div>
      <button className="button mt-6" disabled={check.state === 'loading'} onClick={() => setAttempt(value => value + 1)}>Kiểm tra lại</button>
    </div>
  </section>;
}
