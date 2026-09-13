import { useState } from 'react';
import type { BorrowDto } from '../../types/circulation';
import { fetchBorrows, payFine, returnBorrow } from '../../api/circulation';
import { BorrowStatusBadge } from '../StatusBadge';

function formatVnd(amount: number | string | null): string {
  if (amount == null) return '0 đ';
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  if (isNaN(num)) return '0 đ';
  return num.toLocaleString('vi-VN') + ' đ';
}

export function ReturnPanel() {
  const [barcodeInput, setBarcodeInput] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [activeBorrow, setActiveBorrow] = useState<BorrowDto | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isReturning, setIsReturning] = useState(false);
  const [isPayingFine, setIsPayingFine] = useState(false);

  const handleSearchBorrow = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const clean = barcodeInput.trim();
    if (!clean) return;

    setIsSearching(true);
    setError(null);
    setSuccessMessage(null);
    setActiveBorrow(null);

    try {
      // Find active borrow for this barcode
      const res = await fetchBorrows({ status: 'BORROWED', size: 100 });
      const found = res.content.find((b) => b.barcode?.toUpperCase() === clean.toUpperCase());
      if (!found) {
        setError(`Không tìm thấy khoản mượn nào đang hoạt động cho mã barcode "${clean}".`);
      } else {
        setActiveBorrow(found);
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Lỗi khi tra cứu khoản mượn.');
    } finally {
      setIsSearching(false);
    }
  };

  const handleReturn = async () => {
    if (!activeBorrow) return;

    setIsReturning(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const updated = await returnBorrow(activeBorrow.id);
      setActiveBorrow(updated);
      setSuccessMessage('Trả sách thành công! Đã cập nhật trạng thái bản sao thành "Có sẵn".');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể thực hiện trả sách.');
    } finally {
      setIsReturning(false);
    }
  };

  const handlePayFine = async () => {
    if (!activeBorrow) return;

    setIsPayingFine(true);
    setError(null);

    try {
      const updated = await payFine(activeBorrow.id);
      setActiveBorrow(updated);
      setSuccessMessage('Đã ghi nhận thu tiền phạt thành công!');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể ghi nhận thu tiền phạt.');
    } finally {
      setIsPayingFine(false);
    }
  };

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs">
      <h2 className="text-lg font-bold text-slate-900">Quầy Tiếp Nhận Trả Sách & Thu Phạt</h2>
      <p className="mt-1 text-xs text-slate-500">
        Quét mã vạch trên cuốn sách để tra cứu khoản mượn và hoàn tất thủ tục trả sách.
      </p>

      {/* Barcode Search Form */}
      <form onSubmit={handleSearchBorrow} className="mt-6 flex max-w-xl gap-2">
        <input
          type="text"
          value={barcodeInput}
          onChange={(e) => setBarcodeInput(e.target.value)}
          placeholder="Quét hoặc nhập mã Barcode sách (PXU-BC-...)"
          className="flex-1 rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-mono text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900 shadow-xs"
        />
        <button
          type="submit"
          disabled={isSearching || !barcodeInput.trim()}
          className="rounded-xl bg-blue-900 px-6 py-3 text-sm font-semibold text-white shadow-xs hover:bg-blue-800 disabled:opacity-50 transition-colors"
        >
          {isSearching ? 'Đang tìm…' : 'Tra cứu'}
        </button>
      </form>

      {error && (
        <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-4 text-xs text-rose-700">
          {error}
        </div>
      )}

      {successMessage && (
        <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-xs font-semibold text-emerald-800">
          ✓ {successMessage}
        </div>
      )}

      {/* Active Borrow Card */}
      {activeBorrow && (
        <div className="mt-6 rounded-xl border border-slate-200 bg-slate-50/50 p-6">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-slate-200 pb-4">
            <div>
              <div className="flex items-center gap-2">
                <BorrowStatusBadge status={activeBorrow.status} overdue={activeBorrow.overdue} />
                <span className="font-mono text-xs font-bold text-slate-800">
                  {activeBorrow.barcode}
                </span>
              </div>
              <h3 className="mt-2 text-base font-bold text-slate-900">{activeBorrow.bookTitle}</h3>
              <p className="text-xs text-slate-500">{activeBorrow.libraryName}</p>
            </div>

            <div className="text-sm">
              <span className="block text-xs text-slate-500 font-medium">Người mượn:</span>
              <span className="font-semibold text-slate-800">
                {activeBorrow.userFullName} ({activeBorrow.studentCode})
              </span>
              <span className="block text-xs text-slate-500">{activeBorrow.userEmail}</span>
            </div>
          </div>

          <div className="mt-4 grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs text-slate-600">
            <div>
              <span className="block text-slate-500 font-medium">Ngày mượn</span>
              <span className="font-semibold text-slate-800">
                {new Date(activeBorrow.borrowedAt).toLocaleDateString('vi-VN')}
              </span>
            </div>
            <div>
              <span className="block text-slate-500 font-medium">Hạn trả</span>
              <span
                className={`font-semibold ${
                  activeBorrow.overdue ? 'text-red-700 font-bold' : 'text-slate-800'
                }`}
              >
                {new Date(activeBorrow.dueAt).toLocaleDateString('vi-VN')}
              </span>
            </div>
            <div>
              <span className="block text-slate-500 font-medium">Ngày trả thực tế</span>
              <span className="font-semibold text-slate-800">
                {activeBorrow.returnedAt
                  ? new Date(activeBorrow.returnedAt).toLocaleDateString('vi-VN')
                  : 'Chưa trả'}
              </span>
            </div>
          </div>

          {/* Fine amount status */}
          {activeBorrow.fineAmount != null && Number(activeBorrow.fineAmount) > 0 && (
            <div className="mt-4 rounded-lg border border-amber-300 bg-amber-50 p-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              <div>
                <span className="text-xs font-semibold text-amber-800 block">
                  Tiền phạt trễ hạn:
                </span>
                <span className="text-lg font-black text-red-700">
                  {formatVnd(activeBorrow.fineAmount)}
                </span>
                <span
                  className={`block text-xs mt-0.5 font-bold ${
                    activeBorrow.finePaidAt ? 'text-emerald-700' : 'text-amber-900'
                  }`}
                >
                  {activeBorrow.finePaidAt ? '✓ Đã thanh toán tiền phạt' : '⚠ Chưa thanh toán tiền phạt'}
                </span>
              </div>

              {!activeBorrow.finePaidAt && (
                <button
                  type="button"
                  onClick={handlePayFine}
                  disabled={isPayingFine}
                  className="rounded-xl bg-amber-700 px-4 py-2.5 text-xs font-bold text-white hover:bg-amber-800 disabled:opacity-50 transition-colors shadow-xs"
                >
                  {isPayingFine ? 'Đang xử lý…' : 'Xác nhận thu tiền phạt'}
                </button>
              )}
            </div>
          )}

          {/* Action: Return */}
          <div className="mt-6 flex justify-end gap-3 pt-4 border-t border-slate-200">
            {activeBorrow.status === 'BORROWED' && (
              <button
                type="button"
                onClick={handleReturn}
                disabled={isReturning}
                className="rounded-xl bg-blue-900 px-6 py-2.5 text-xs font-bold text-white hover:bg-blue-800 disabled:opacity-50 transition-colors shadow-xs"
              >
                {isReturning ? 'Đang cập nhật trả sách…' : 'Xác nhận trả sách ngay →'}
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
