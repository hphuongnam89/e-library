import { useEffect, useState } from 'react';
import type { BorrowDto, BorrowStatus } from '../../types/circulation';
import { fetchBorrows } from '../../api/circulation';
import { BorrowStatusBadge } from '../StatusBadge';
import { Pagination } from '../Pagination';

export function BorrowTable() {
  const [borrows, setBorrows] = useState<BorrowDto[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState<BorrowStatus | 'ALL'>('ALL');
  const [overdueOnly, setOverdueOnly] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setIsLoading(true);
    setError(null);

    fetchBorrows({
      status: statusFilter === 'ALL' ? undefined : statusFilter,
      overdue: overdueOnly ? true : undefined,
      page: currentPage,
      size: 10,
      signal: controller.signal,
    })
      .then((page) => {
        setBorrows(page.content ?? []);
        setTotalPages(page.totalPages ?? 0);
        setTotalElements(page.totalElements ?? 0);
      })
      .catch((err: unknown) => {
        if (err instanceof Error && err.name === 'AbortError') return;
        setError(err instanceof Error ? err.message : 'Không thể tải danh sách mượn trả.');
      })
      .finally(() => setIsLoading(false));

    return () => controller.abort();
  }, [statusFilter, overdueOnly, currentPage]);

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h2 className="text-lg font-bold text-slate-900">Quản Lý Danh Sách Lưu Thông Toàn Hệ Thống</h2>
          <p className="mt-1 text-xs text-slate-500">
            Tổng số bản ghi: <strong>{totalElements}</strong> lượt mượn
          </p>
        </div>

        {/* Filters */}
        <div className="flex flex-wrap items-center gap-3 text-xs">
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value as BorrowStatus | 'ALL');
              setCurrentPage(0);
            }}
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-semibold text-slate-700"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="BORROWED">Đang mượn</option>
            <option value="RETURNED">Đã trả</option>
          </select>

          <label className="flex items-center gap-2 cursor-pointer font-medium text-slate-700">
            <input
              type="checkbox"
              checked={overdueOnly}
              onChange={(e) => {
                setOverdueOnly(e.target.checked);
                setCurrentPage(0);
              }}
              className="rounded-sm border-slate-300 text-blue-900 focus:ring-blue-900"
            />
            Chỉ xem quá hạn
          </label>
        </div>
      </div>

      {error ? (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-6 text-center text-xs text-rose-700">
          {error}
        </div>
      ) : isLoading ? (
        <div className="space-y-2 py-4">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-10 rounded-lg bg-slate-100 animate-pulse" />
          ))}
        </div>
      ) : borrows.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-200 p-8 text-center text-xs text-slate-500">
          Không có dữ liệu mượn trả nào phù hợp với bộ lọc hiện tại.
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-slate-200">
          <table className="min-w-full divide-y divide-slate-200 text-xs text-left">
            <thead className="bg-slate-50 text-slate-600 font-semibold">
              <tr>
                <th scope="col" className="px-3.5 py-2.5">Mã Barcode</th>
                <th scope="col" className="px-3.5 py-2.5">Nhan đề sách</th>
                <th scope="col" className="px-3.5 py-2.5">Người mượn</th>
                <th scope="col" className="px-3.5 py-2.5">Ngày mượn</th>
                <th scope="col" className="px-3.5 py-2.5">Hạn trả</th>
                <th scope="col" className="px-3.5 py-2.5">Trạng thái</th>
                <th scope="col" className="px-3.5 py-2.5 text-right">Phạt</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white text-slate-700">
              {borrows.map((b) => (
                <tr key={b.id} className="hover:bg-slate-50">
                  <td className="px-3.5 py-2.5 font-mono font-bold text-slate-900">{b.barcode}</td>
                  <td className="px-3.5 py-2.5 font-medium text-slate-900">{b.bookTitle}</td>
                  <td className="px-3.5 py-2.5">
                    <span className="font-semibold text-slate-800">{b.userFullName}</span>
                    <span className="block text-[11px] text-slate-400 font-mono">({b.studentCode})</span>
                  </td>
                  <td className="px-3.5 py-2.5">{new Date(b.borrowedAt).toLocaleDateString('vi-VN')}</td>
                  <td className="px-3.5 py-2.5">
                    <span className={b.overdue ? 'text-red-700 font-bold' : ''}>
                      {new Date(b.dueAt).toLocaleDateString('vi-VN')}
                    </span>
                  </td>
                  <td className="px-3.5 py-2.5">
                    <BorrowStatusBadge status={b.status} overdue={b.overdue} />
                  </td>
                  <td className="px-3.5 py-2.5 text-right">
                    {b.fineAmount != null && Number(b.fineAmount) > 0 ? (
                      <span className="font-bold text-red-700">
                        {Number(b.fineAmount).toLocaleString('vi-VN')} đ
                      </span>
                    ) : (
                      '—'
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Pagination
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={(page) => setCurrentPage(page)}
      />
    </div>
  );
}
