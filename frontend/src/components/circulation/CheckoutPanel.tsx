import { useState } from 'react';
import type { BookCopyDto } from '../../types/catalog';
import type { BorrowDto } from '../../types/circulation';
import { fetchBookCopyByBarcode } from '../../api/catalog';
import { checkoutBooks } from '../../api/circulation';
import { BookCopyStatusBadge } from '../StatusBadge';

interface StagedBook {
  barcode: string;
  copy?: BookCopyDto;
  error?: string;
}

export function CheckoutPanel() {
  const [studentCode, setStudentCode] = useState('');
  const [barcodeInput, setBarcodeInput] = useState('');
  const [stagedBooks, setStagedBooks] = useState<StagedBook[]>([]);
  const [isLookingUp, setIsLookingUp] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [checkoutError, setCheckoutError] = useState<string | null>(null);
  const [successBorrows, setSuccessBorrows] = useState<BorrowDto[] | null>(null);

  const handleAddBarcode = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const clean = barcodeInput.trim();
    if (!clean) return;

    if (stagedBooks.some((b) => b.barcode.toUpperCase() === clean.toUpperCase())) {
      setCheckoutError(`Mã barcode "${clean}" đã có trong danh sách.`);
      return;
    }

    setIsLookingUp(true);
    setCheckoutError(null);

    try {
      const copy = await fetchBookCopyByBarcode(clean);
      setStagedBooks((prev) => [
        ...prev,
        {
          barcode: copy.barcode,
          copy,
          error: copy.status !== 'AVAILABLE' ? `Sách đang ở trạng thái: ${copy.status}` : undefined,
        },
      ]);
      setBarcodeInput('');
    } catch {
      // If barcode not found in catalog, still allow adding or warn
      setStagedBooks((prev) => [
        ...prev,
        {
          barcode: clean,
          error: 'Không tìm thấy thông tin bản sao trong hệ thống.',
        },
      ]);
      setBarcodeInput('');
    } finally {
      setIsLookingUp(false);
    }
  };

  const handleRemoveBarcode = (index: number) => {
    setStagedBooks((prev) => prev.filter((_, i) => i !== index));
  };

  const handleConfirmCheckout = async () => {
    const cleanCode = studentCode.trim();
    if (!cleanCode) {
      setCheckoutError('Vui lòng nhập Mã số sinh viên (MSSV).');
      return;
    }

    if (stagedBooks.length === 0) {
      setCheckoutError('Vui lòng quét hoặc nhập ít nhất một mã vạch sách.');
      return;
    }

    const hasErrors = stagedBooks.some((b) => b.error);
    if (hasErrors) {
      setCheckoutError('Vui lòng xóa các bản sách không hợp lệ trước khi xác nhận mượn.');
      return;
    }

    setIsSubmitting(true);
    setCheckoutError(null);

    try {
      const res = await checkoutBooks({
        studentCode: cleanCode,
        barcodes: stagedBooks.map((b) => b.barcode),
      });
      setSuccessBorrows(res.items);
      setStagedBooks([]);
      setStudentCode('');
    } catch (err: unknown) {
      setCheckoutError(err instanceof Error ? err.message : 'Mượn sách không thành công.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleNewCheckout = () => {
    setSuccessBorrows(null);
    setStudentCode('');
    setBarcodeInput('');
    setStagedBooks([]);
    setCheckoutError(null);
  };

  if (successBorrows) {
    return (
      <div className="rounded-2xl border border-emerald-200 bg-white p-6 sm:p-8 shadow-xs">
        <div className="flex items-center gap-3 text-emerald-700">
          <span className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-100 text-xl font-bold">
            ✓
          </span>
          <div>
            <h2 className="text-xl font-bold text-slate-900">Mượn sách theo lô thành công!</h2>
            <p className="text-xs text-slate-600">
              Đã ghi nhận thành công {successBorrows.length} cuốn sách vào tài khoản người mượn.
            </p>
          </div>
        </div>

        <div className="mt-6 overflow-x-auto rounded-lg border border-slate-200">
          <table className="min-w-full divide-y divide-slate-200 text-xs text-left">
            <thead className="bg-slate-50 text-slate-600 font-semibold">
              <tr>
                <th scope="col" className="px-3.5 py-2.5">Mã Barcode</th>
                <th scope="col" className="px-3.5 py-2.5">Nhan đề sách</th>
                <th scope="col" className="px-3.5 py-2.5">Người mượn (MSSV)</th>
                <th scope="col" className="px-3.5 py-2.5">Hạn trả</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white text-slate-700">
              {successBorrows.map((b) => (
                <tr key={b.id}>
                  <td className="px-3.5 py-2.5 font-mono font-bold text-blue-900">{b.barcode}</td>
                  <td className="px-3.5 py-2.5 font-semibold text-slate-900">{b.bookTitle}</td>
                  <td className="px-3.5 py-2.5">{b.userFullName} ({b.studentCode})</td>
                  <td className="px-3.5 py-2.5 font-semibold text-emerald-700">
                    {new Date(b.dueAt).toLocaleDateString('vi-VN')}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="mt-6 flex justify-end">
          <button
            type="button"
            onClick={handleNewCheckout}
            className="rounded-xl bg-blue-900 px-5 py-2.5 text-xs font-semibold text-white hover:bg-blue-800 transition-colors shadow-xs"
          >
            + Tiếp tục mượn cho độc giả khác
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-xs">
      <h2 className="text-lg font-bold text-slate-900">Quầy Check-out Mượn Sách Theo Lô</h2>
      <p className="mt-1 text-xs text-slate-500">
        Hỗ trợ máy quét mã vạch USB HID (quét chuỗi kết thúc bằng Enter).
      </p>

      {checkoutError && (
        <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-4 text-xs text-rose-700">
          <strong>Lỗi mượn sách:</strong> {checkoutError}
        </div>
      )}

      <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Step 1: Student Code */}
        <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-5">
          <label htmlFor="student-code-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
            1. Mã số sinh viên / Độc giả (MSSV)
          </label>
          <input
            id="student-code-input"
            type="text"
            value={studentCode}
            onChange={(e) => setStudentCode(e.target.value)}
            placeholder="Ví dụ: PXU2201"
            className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm font-semibold text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
          />
        </div>

        {/* Step 2: Barcode Scanner Input */}
        <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-5">
          <label htmlFor="barcode-scanner-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
            2. Quét hoặc nhập mã vạch sách
          </label>
          <form onSubmit={handleAddBarcode} className="flex gap-2">
            <input
              id="barcode-scanner-input"
              type="text"
              value={barcodeInput}
              onChange={(e) => setBarcodeInput(e.target.value)}
              placeholder="Quét máy barcode hoặc nhập PXU-BC-..."
              className="flex-1 rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm font-mono text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
            />
            <button
              type="submit"
              disabled={isLookingUp || !barcodeInput.trim()}
              className="rounded-lg bg-blue-900 px-4 py-2.5 text-xs font-semibold text-white hover:bg-blue-800 disabled:opacity-50 transition-colors"
            >
              {isLookingUp ? 'Đang tra…' : 'Thêm'}
            </button>
          </form>
        </div>
      </div>

      {/* Staged Books Table */}
      <div className="mt-6">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700">
            Danh sách sách chờ mượn ({stagedBooks.length})
          </h3>
          {stagedBooks.length > 0 && (
            <button
              type="button"
              onClick={() => setStagedBooks([])}
              className="text-xs text-rose-700 hover:underline"
            >
              Xóa tất cả
            </button>
          )}
        </div>

        {stagedBooks.length === 0 ? (
          <div className="rounded-xl border border-dashed border-slate-200 p-8 text-center text-xs text-slate-500">
            Chưa có cuốn sách nào được quét vào danh sách. Hãy dùng máy quét hoặc gõ mã barcode ở trên.
          </div>
        ) : (
          <div className="overflow-x-auto rounded-xl border border-slate-200">
            <table className="min-w-full divide-y divide-slate-200 text-xs text-left">
              <thead className="bg-slate-50 text-slate-600 font-semibold">
                <tr>
                  <th scope="col" className="px-3.5 py-2.5">STT</th>
                  <th scope="col" className="px-3.5 py-2.5">Mã Barcode</th>
                  <th scope="col" className="px-3.5 py-2.5">Nhan đề sách</th>
                  <th scope="col" className="px-3.5 py-2.5">Vị trí</th>
                  <th scope="col" className="px-3.5 py-2.5">Trạng thái</th>
                  <th scope="col" className="px-3.5 py-2.5 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white text-slate-700">
                {stagedBooks.map((item, idx) => (
                  <tr key={item.barcode} className={item.error ? 'bg-rose-50/50' : 'hover:bg-slate-50'}>
                    <td className="px-3.5 py-2.5 text-slate-400">{idx + 1}</td>
                    <td className="px-3.5 py-2.5 font-mono font-semibold text-slate-900">
                      {item.barcode}
                    </td>
                    <td className="px-3.5 py-2.5 font-medium text-slate-900">
                      {item.copy?.bookTitle || <span className="text-slate-400 italic">Chưa xác định</span>}
                    </td>
                    <td className="px-3.5 py-2.5 text-slate-500">
                      {item.copy?.location || '—'}
                    </td>
                    <td className="px-3.5 py-2.5">
                      {item.error ? (
                        <span className="text-xs font-semibold text-rose-700">⚠ {item.error}</span>
                      ) : item.copy ? (
                        <BookCopyStatusBadge status={item.copy.status} />
                      ) : (
                        <span className="text-xs text-slate-400">Đã nhận mã</span>
                      )}
                    </td>
                    <td className="px-3.5 py-2.5 text-right">
                      <button
                        type="button"
                        onClick={() => handleRemoveBarcode(idx)}
                        className="rounded-md px-2 py-1 text-xs font-medium text-rose-700 hover:bg-rose-100 transition-colors"
                      >
                        Xóa
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Confirm Checkout Button */}
      <div className="mt-6 flex items-center justify-between pt-4 border-t border-slate-100">
        <span className="text-xs text-slate-500">
          Tổng số sách: <strong>{stagedBooks.length}</strong> cuốn
        </span>
        <button
          type="button"
          onClick={handleConfirmCheckout}
          disabled={isSubmitting || stagedBooks.length === 0 || !studentCode.trim()}
          className="rounded-xl bg-emerald-700 px-6 py-3 text-sm font-semibold text-white shadow-xs hover:bg-emerald-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {isSubmitting ? 'Đang xử lý check-out…' : 'Xác nhận mượn sách theo lô →'}
        </button>
      </div>
    </div>
  );
}
