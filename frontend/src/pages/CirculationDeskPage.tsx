import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { CheckoutPanel } from '../components/circulation/CheckoutPanel';
import { ReturnPanel } from '../components/circulation/ReturnPanel';
import { BorrowTable } from '../components/circulation/BorrowTable';

type DeskMode = 'CHECKOUT' | 'RETURN' | 'ALL_BORROWS';

export function CirculationDeskPage() {
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const [mode, setMode] = useState<DeskMode>('CHECKOUT');

  const isLibrarianOrAdmin =
    isAuthenticated && (user?.role === 'LIBRARIAN' || user?.role === 'ADMIN');

  if (isAuthLoading) {
    return (
      <div className="py-20 text-center text-sm text-slate-500">
        Đang xác thực thông tin tài khoản…
      </div>
    );
  }

  if (!isLibrarianOrAdmin) {
    return (
      <section className="max-w-xl py-16 sm:py-24 mx-auto text-center">
        <div className="rounded-2xl border border-rose-200 bg-white p-8 shadow-xs">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-rose-100 text-2xl text-rose-700">
            ⛔
          </div>
          <h2 className="mt-4 text-2xl font-bold text-slate-900">Truy cập bị từ chối</h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Quầy lưu thông thư viện yêu cầu quyền <strong>Thủ thư</strong> hoặc{' '}
            <strong>Quản trị viên</strong>. Tài khoản hiện tại của bạn không có quyền truy cập khu vực này.
          </p>
          <div className="mt-6">
            <Link
              to="/"
              className="rounded-lg bg-slate-900 px-5 py-2.5 text-xs font-semibold text-white hover:bg-slate-800 transition-colors"
            >
              ← Quay về Trang chủ
            </Link>
          </div>
        </div>
      </section>
    );
  }

  return (
    <div className="py-8">
      <div className="mb-6">
        <div className="flex items-center gap-2">
          <span className="rounded-md bg-blue-100 px-2 py-0.5 text-xs font-bold text-blue-900">
            Khu vực Thủ thư
          </span>
          <p className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Nghiệp vụ lưu thông
          </p>
        </div>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
          Quầy Lưu Thông Thư Viện
        </h1>
        <p className="mt-2 text-sm text-slate-600">
          Tiếp nhận mượn sách theo lô (USB HID scanner), xử lý trả sách và ghi nhận nộp phạt trễ hạn.
        </p>
      </div>

      {/* Mode navigation */}
      <div className="border-b border-slate-200 flex flex-wrap gap-2 mb-8">
        <button
          type="button"
          onClick={() => setMode('CHECKOUT')}
          className={`px-5 py-3 text-sm font-semibold border-b-2 transition-colors flex items-center gap-2 ${
            mode === 'CHECKOUT'
              ? 'border-blue-900 text-blue-900'
              : 'border-transparent text-slate-600 hover:text-slate-900'
          }`}
        >
          <span>📥 Mượn sách theo lô (Check-out)</span>
        </button>
        <button
          type="button"
          onClick={() => setMode('RETURN')}
          className={`px-5 py-3 text-sm font-semibold border-b-2 transition-colors flex items-center gap-2 ${
            mode === 'RETURN'
              ? 'border-blue-900 text-blue-900'
              : 'border-transparent text-slate-600 hover:text-slate-900'
          }`}
        >
          <span>📤 Tiếp nhận trả sách & Thu phạt</span>
        </button>
        <button
          type="button"
          onClick={() => setMode('ALL_BORROWS')}
          className={`px-5 py-3 text-sm font-semibold border-b-2 transition-colors flex items-center gap-2 ${
            mode === 'ALL_BORROWS'
              ? 'border-blue-900 text-blue-900'
              : 'border-transparent text-slate-600 hover:text-slate-900'
          }`}
        >
          <span>📋 Quản lý lưu thông toàn hệ thống</span>
        </button>
      </div>

      {/* Content */}
      <main>
        {mode === 'CHECKOUT' && <CheckoutPanel />}
        {mode === 'RETURN' && <ReturnPanel />}
        {mode === 'ALL_BORROWS' && <BorrowTable />}
      </main>
    </div>
  );
}
