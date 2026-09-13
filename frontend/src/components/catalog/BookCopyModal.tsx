import { useEffect, useState } from 'react';
import type { BookCopyDto, BookCopyStatus, BookTitleDto } from '../../types/catalog';
import type { LibraryDto } from '../../types/organization';
import { createBookCopy, updateBookCopy } from '../../api/catalog';
import { fetchLibraries } from '../../api/organization';

interface BookCopyModalProps {
  isOpen: boolean;
  book: BookTitleDto;
  copyToEdit: BookCopyDto | null;
  onClose: () => void;
  onSuccess: () => void;
}

export function BookCopyModal({
  isOpen,
  book,
  copyToEdit,
  onClose,
  onSuccess,
}: BookCopyModalProps) {
  const [libraries, setLibraries] = useState<LibraryDto[]>([]);
  const [libraryId, setLibraryId] = useState<number | ''>('');
  const [barcode, setBarcode] = useState('');
  const [location, setLocation] = useState('');
  const [status, setStatus] = useState<BookCopyStatus>('AVAILABLE');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) return;

    fetchLibraries(undefined, 0, 100)
      .then((page) => {
        setLibraries(page.content);
        if (page.content && page.content.length > 0 && !copyToEdit) {
          const first = page.content[0];
          if (first) setLibraryId(first.id);
        }
      })
      .catch(() => {
        // ignore or fallback
      });

    if (copyToEdit) {
      setLibraryId(copyToEdit.libraryId);
      setBarcode(copyToEdit.barcode);
      setLocation(copyToEdit.location || '');
      setStatus(copyToEdit.status);
    } else {
      setBarcode('');
      setLocation('');
      setStatus('AVAILABLE');
    }
    setError(null);
  }, [copyToEdit, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (libraryId === '') {
      setError('Vui lòng chọn thư viện lưu trữ bản sách.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      if (copyToEdit) {
        await updateBookCopy(copyToEdit.id, {
          libraryId: Number(libraryId),
          location: location.trim() || undefined,
          status,
        });
      } else {
        await createBookCopy({
          bookTitleId: book.id,
          libraryId: Number(libraryId),
          barcode: barcode.trim() || undefined,
          location: location.trim() || undefined,
          status,
        });
      }
      onSuccess();
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể lưu bản sao sách.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="book-copy-modal-heading"
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 sm:p-6"
    >
      <div className="relative w-full max-w-md rounded-2xl bg-white p-6 sm:p-8 shadow-2xl">
        <button
          type="button"
          onClick={onClose}
          aria-label="Đóng"
          className="absolute right-4 top-4 rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition-colors"
        >
          ✕
        </button>

        <h2 id="book-copy-modal-heading" className="text-xl font-bold text-slate-900">
          {copyToEdit ? 'Cập nhật Bản sao Sách' : 'Thêm Bản sao Vật lý Mới'}
        </h2>
        <p className="mt-1 text-xs text-slate-500 line-clamp-1 font-semibold">
          Sách: {book.title}
        </p>

        {error && (
          <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          <div>
            <label htmlFor="copy-library-select" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Thư viện lưu trữ *
            </label>
            <select
              id="copy-library-select"
              required
              value={libraryId}
              onChange={(e) => setLibraryId(e.target.value ? Number(e.target.value) : '')}
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
            >
              <option value="">-- Chọn thư viện --</option>
              {libraries.map((lib) => (
                <option key={lib.id} value={lib.id}>
                  {lib.name} ({lib.campusName})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="copy-barcode-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Mã vạch Barcode
            </label>
            <input
              id="copy-barcode-input"
              type="text"
              disabled={!!copyToEdit}
              value={barcode}
              onChange={(e) => setBarcode(e.target.value)}
              placeholder="Để trống hệ thống sẽ tự sinh (PXU-BC-...)"
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm font-mono text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900 disabled:bg-slate-100"
            />
            {copyToEdit && (
              <span className="text-[11px] text-slate-400 mt-1 block">Mã Barcode không thể sửa sau khi đã cấp.</span>
            )}
          </div>

          <div>
            <label htmlFor="copy-location-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Vị trí kệ sách
            </label>
            <input
              id="copy-location-input"
              type="text"
              maxLength={255}
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="Ví dụ: Kệ IT-01, Tầng 2"
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
            />
          </div>

          <div>
            <label htmlFor="copy-status-select" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Trạng thái bản sao
            </label>
            <select
              id="copy-status-select"
              value={status}
              onChange={(e) => setStatus(e.target.value as BookCopyStatus)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
            >
              <option value="AVAILABLE">Có sẵn (AVAILABLE)</option>
              <option value="BORROWED">Đang mượn (BORROWED)</option>
              <option value="MAINTENANCE">Bảo trì (MAINTENANCE)</option>
              <option value="DAMAGED">Hư hỏng (DAMAGED)</option>
              <option value="LOST">Thất lạc (LOST)</option>
            </select>
          </div>

          <div className="mt-6 flex justify-end gap-3 pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition-colors"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="rounded-lg bg-blue-900 px-5 py-2 text-xs font-semibold text-white hover:bg-blue-800 disabled:opacity-50 transition-colors shadow-xs"
            >
              {isSubmitting ? 'Đang lưu…' : copyToEdit ? 'Lưu thay đổi' : 'Thêm bản sao'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
