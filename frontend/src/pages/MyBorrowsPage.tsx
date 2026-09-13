import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import type { BorrowDto } from '../types/circulation';
import { fetchMyBorrows } from '../api/circulation';
import { BorrowStatusBadge } from '../components/StatusBadge';
import { Pagination } from '../components/Pagination';
import { useAuth } from '../hooks/useAuth';
import { ApiError } from '../api/client';

type FilterTab = 'ALL' | 'ACTIVE' | 'RETURNED' | 'OVERDUE';

function formatDateTime(isoString: string | null): string {
  if (!isoString) return '—';
  try {
    return new Date(isoString).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return isoString;
  }
}

function formatVnd(amount: number | string | null): string {
  if (amount == null) return '0 đ';
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  if (isNaN(num)) return '0 đ';
  return num.toLocaleString('vi-VN') + ' đ';
}

export function MyBorrowsPage() {
  const { isAuthenticated, isLoading: isAuthLoading, login } = useAuth();

  const [borrows, setBorrows] = useState<BorrowDto[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<FilterTab>('ALL');

  useEffect(() => {
    if (!isAuthenticated) return;

    const controller = new AbortController();
    setIsLoading(true);
    setError(null);

    fetchMyBorrows(currentPage, 10, controller.signal)
      .then((page) => {
        setBorrows(page.content ?? []);
        setTotalPages(page.totalPages ?? 0);
        setTotalElements(page.totalElements ?? 0);
      })
      .catch((err: unknown) => {
        if (err instanceof Error && err.name === 'AbortError') return;
        if (err instanceof ApiError && err.status === 401) {
          setError('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');
        } else {
          setError(err instanceof Error ? err.message : 'Không thể tải lịch sử mượn sách.');
        }
      })
      .finally(() => setIsLoading(false));

    return () => controller.abort();
  }, [isAuthenticated, currentPage]);

  const filteredBorrows = useMemo(() => {
    return borrows.filter((item) => {
      if (activeTab === 'ACTIVE') return item.status === 'BORROWED';
      if (activeTab === 'RETURNED') return item.status === 'RETURNED';
      if (activeTab === 'OVERDUE') return item.overdue;
      return true;
    });
  }, [borrows, activeTab]);

  const overdueCount = useMemo(() => borrows.filter((b) => b.overdue).length, [borrows]);
  const activeCount = useMemo(() => borrows.filter((b) => b.status === 'BORROWED').length, [borrows]);

  if (!isAuthLoading && !isAuthenticated) {
    return (
      <section className="max-w-2xl py-16 sm:py-24 mx-auto text-center">
        <div className="rounded-2xl border border-slate-200 bg-white p-8 shadow-xs">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-blue-100 text-2xl text-blue-900">
            📚
          </div>
          <h2 className="mt-4 text-2xl font-bold text-slate-900">Lịch sử mượn sách cá nhân</h2>
          <p className="mt-3 text-sm leading-6 text-slate-600">
            Vui lòng đăng nhập bằng tài khoản sinh viên/giảng viên để theo dõi các cuốn sách bạn đang mượn và hạn trả.
          </p>
          <div className="mt-6">
            <button
              type="button"
              onClick={login}
              className="rounded-lg bg-blue-900 px-6 py-2.5 text-sm font-semibold text-white shadow-xs hover:bg-blue-800 transition-colors"
            >
              Đăng nhập Google
            </button>
          </div>
        </div>
      </section>
    );
  }

  return (
    <div className="py-8">
      <div className="mb-6">
        <p className="text-xs font-bold uppercase tracking-wider text-blue-700">Dịch vụ lưu thông</p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
          Sách & Khoản Mượn Của Tôi
        </h1>
        <p className="mt-2 text-sm text-slate-600">
          Theo dõi các cuốn sách bạn đang giữ, thời hạn trả sách và lịch sử các lần mượn trước đây.
        </p>
      </div>

      {/* Tabs */}
      <div className="border-b border-slate-200 flex flex-wrap gap-2 mb-6">
        <button
          type="button"
          onClick={() => setActiveTab('ALL')}
          className={`px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors ${
            activeTab === 'ALL'
              ? 'border-blue-900 text-blue-900'
              : 'border-transparent text-slate-600 hover:text-slate-900'
          }`}
        >
          Tất cả ({totalElements})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('ACTIVE')}
          className={`px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors ${
            activeTab === 'ACTIVE'
              ? 'border-blue-900 text-blue-900'
              : 'border-transparent text-slate-600 hover:text-slate-900'
          }`}
        >
          Đang mượn ({activeCount})
        </button>
        {overdueCount > 0 && (
          <button
            type="button"
            onClick={() => setActiveTab('OVERDUE')}
            className={`px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors flex items-center gap-1.5 ${
              activeTab === 'OVERDUE'
                ? 'border-red-600 text-red-700'
                : 'border-transparent text-red-600 hover:text-red-800'
            }`}
          >
            <span>Quá hạn</span>
            <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs text-red-800 font-bold">
              {overdueCount}
            </span>
          </button>
        )}
        <button
          type="button"
          onClick={() => setActiveTab('RETURNED')}
          className={`px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors ${
            activeTab === 'RETURNED'
              ? 'border-blue-900 text-blue-900'
              : 'border-transparent text-slate-600 hover:text-slate-900'
          }`}
        >
          Đã trả
        </button>
      </div>

      {/* Content */}
      {error ? (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-6 text-center">
          <p className="text-sm text-rose-700">{error}</p>
          <button
            type="button"
            onClick={() => setCurrentPage((p) => p)}
            className="mt-3 rounded-lg bg-rose-700 px-4 py-2 text-xs font-semibold text-white hover:bg-rose-800"
          >
            Thử lại
          </button>
        </div>
      ) : isLoading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-28 rounded-xl border border-slate-200 bg-white p-5 animate-pulse" />
          ))}
        </div>
      ) : filteredBorrows.length === 0 ? (
        <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-xl text-slate-400">
            📖
          </div>
          <h3 className="mt-3 text-base font-semibold text-slate-900">
            {activeTab === 'OVERDUE'
              ? 'Tuyệt vời! Bạn không có cuốn sách nào bị quá hạn.'
              : activeTab === 'ACTIVE'
              ? 'Bạn hiện không mượn cuốn sách nào.'
              : 'Chưa có dữ liệu mượn sách.'}
          </h3>
          <p className="mt-1 text-xs text-slate-500">
            Khám phá hàng ngàn đầu sách tại thư viện và đăng ký mượn ngay hôm nay.
          </p>
          <div className="mt-5">
            <Link
              to="/catalog"
              className="inline-flex rounded-lg bg-blue-900 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-800 transition-colors"
            >
              Tìm kiếm sách ngay →
            </Link>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredBorrows.map((item) => (
            <article
              key={item.id}
              className={`rounded-xl border p-5 bg-white shadow-xs transition-shadow ${
                item.overdue ? 'border-red-300 bg-red-50/30' : 'border-slate-200'
              }`}
            >
              <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2 flex-wrap">
                    <BorrowStatusBadge status={item.status} overdue={item.overdue} />
                    <span className="font-mono text-xs font-semibold text-slate-700 bg-slate-100 px-2 py-0.5 rounded-sm">
                      Mã bản sách: {item.barcode || '—'}
                    </span>
                    {item.libraryName && (
                      <span className="text-xs text-slate-500">📍 {item.libraryName}</span>
                    )}
                  </div>

                  <h3 className="mt-2 text-base font-bold text-slate-900">
                    {item.bookTitle || 'Nhan đề không khả dụng'}
                  </h3>
                </div>

                {/* Fine notification */}
                {item.fineAmount != null && Number(item.fineAmount) > 0 && (
                  <div className="rounded-lg bg-amber-50 border border-amber-200 p-2.5 text-right sm:min-w-44">
                    <span className="block text-[11px] font-medium text-amber-800">
                      Tiền phạt quá hạn
                    </span>
                    <span className="text-sm font-bold text-red-700">
                      {formatVnd(item.fineAmount)}
                    </span>
                    <span
                      className={`block text-[10px] font-semibold mt-0.5 ${
                        item.finePaidAt ? 'text-emerald-700' : 'text-amber-800'
                      }`}
                    >
                      {item.finePaidAt ? '✓ Đã thanh toán' : '⚠ Chưa thanh toán'}
                    </span>
                  </div>
                )}
              </div>

              {/* Loan Details Dates */}
              <div className="mt-4 pt-3 border-t border-slate-100 grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs text-slate-600">
                <div>
                  <span className="block font-medium text-slate-500">Ngày mượn</span>
                  <span className="font-semibold text-slate-800">{formatDateTime(item.borrowedAt)}</span>
                </div>
                <div>
                  <span className="block font-medium text-slate-500">Hạn trả sách</span>
                  <span
                    className={`font-semibold ${
                      item.overdue ? 'text-red-700 font-bold' : 'text-slate-800'
                    }`}
                  >
                    {formatDateTime(item.dueAt)}
                  </span>
                </div>
                <div>
                  <span className="block font-medium text-slate-500">Ngày thực tế trả</span>
                  <span className="font-semibold text-slate-800">
                    {item.returnedAt ? formatDateTime(item.returnedAt) : 'Chưa trả'}
                  </span>
                </div>
              </div>
            </article>
          ))}

          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={(page) => setCurrentPage(page)}
          />
        </div>
      )}
    </div>
  );
}
