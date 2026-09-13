import { useCallback, useEffect, useState } from 'react';
import type { BookCopyDto, BookTitleDto } from '../../types/catalog';
import { deleteBookCopy, fetchBookCopies } from '../../api/catalog';
import { BookCopyStatusBadge } from '../StatusBadge';

interface BookDetailModalProps {
  book: BookTitleDto | null;
  isLibrarianOrAdmin?: boolean;
  onClose: () => void;
  onAddCopy?: (book: BookTitleDto) => void;
  onEditCopy?: (copy: BookCopyDto, book: BookTitleDto) => void;
}

export function BookDetailModal({
  book,
  isLibrarianOrAdmin = false,
  onClose,
  onAddCopy,
  onEditCopy,
}: BookDetailModalProps) {
  const [copies, setCopies] = useState<BookCopyDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const loadCopies = useCallback((signal?: AbortSignal) => {
    if (!book) return;
    setIsLoading(true);
    setError(null);

    fetchBookCopies(book.id, signal)
      .then((page) => {
        setCopies(page.content ?? []);
      })
      .catch((err: unknown) => {
        if (err instanceof Error && err.name === 'AbortError') return;
        setError('Không thể tải danh sách bản sao của sách.');
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [book]);

  useEffect(() => {
    if (!book) {
      setCopies([]);
      setError(null);
      setActionError(null);
      return;
    }

    const controller = new AbortController();
    loadCopies(controller.signal);
    return () => controller.abort();
  }, [book, loadCopies]);

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  if (!book) return null;

  const availableCount = copies.filter((c) => c.status === 'AVAILABLE').length;

  const handleDeleteCopy = async (copy: BookCopyDto) => {
    if (!window.confirm(`Bạn có chắc chắn muốn xóa bản sao với mã barcode ${copy.barcode}?`)) {
      return;
    }

    setActionError(null);
    try {
      await deleteBookCopy(copy.id);
      loadCopies();
    } catch (err: unknown) {
      setActionError(err instanceof Error ? err.message : 'Không thể xóa bản sao này.');
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="book-detail-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 sm:p-6"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl bg-white p-6 sm:p-8 shadow-2xl">
        <button
          type="button"
          onClick={onClose}
          aria-label="Đóng chi tiết"
          className="absolute right-4 top-4 rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition-colors"
        >
          ✕
        </button>

        <div>
          {book.categoryName && (
            <span className="inline-block rounded-md bg-blue-100 px-2.5 py-0.5 text-xs font-semibold text-blue-800 mb-2">
              {book.categoryName}
            </span>
          )}
          <h2 id="book-detail-title" className="text-2xl font-bold text-slate-900">
            {book.title}
          </h2>
        </div>

        <div className="mt-4 grid grid-cols-2 sm:grid-cols-3 gap-3 rounded-xl bg-slate-50 p-4 text-xs">
          <div>
            <span className="block font-medium text-slate-500">Tác giả</span>
            <span className="font-semibold text-slate-800">{book.author || '—'}</span>
          </div>
          <div>
            <span className="block font-medium text-slate-500">Nhà xuất bản</span>
            <span className="font-semibold text-slate-800">{book.publisher || '—'}</span>
          </div>
          <div>
            <span className="block font-medium text-slate-500">Năm xuất bản</span>
            <span className="font-semibold text-slate-800">{book.publicationYear || '—'}</span>
          </div>
          <div>
            <span className="block font-medium text-slate-500">Mã ISBN</span>
            <span className="font-mono font-semibold text-slate-800">{book.isbn || '—'}</span>
          </div>
          <div className="col-span-2">
            <span className="block font-medium text-slate-500">Tình trạng lưu thông</span>
            <span className="font-semibold text-emerald-700">
              Có sẵn {availableCount} / {copies.length} cuốn
            </span>
          </div>
        </div>

        {actionError && (
          <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
            {actionError}
          </div>
        )}

        <div className="mt-6">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800">
              Các bản sách vật lý tại thư viện ({copies.length})
            </h3>
            {isLibrarianOrAdmin && onAddCopy && (
              <button
                type="button"
                onClick={() => onAddCopy(book)}
                className="rounded-lg bg-blue-900 px-3 py-1.5 text-xs font-semibold text-white hover:bg-blue-800 transition-colors shadow-xs"
              >
                + Thêm bản sao
              </button>
            )}
          </div>

          {isLoading ? (
            <div className="space-y-2 py-4">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-10 rounded-lg bg-slate-100 animate-pulse" />
              ))}
            </div>
          ) : error ? (
            <div className="rounded-lg bg-red-50 p-4 text-xs text-red-700">
              {error}
            </div>
          ) : copies.length === 0 ? (
            <div className="rounded-lg border border-dashed border-slate-200 p-6 text-center text-xs text-slate-500">
              Chưa có bản sao vật lý nào được đăng ký cho nhan đề này.
            </div>
          ) : (
            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold">
                  <tr>
                    <th scope="col" className="px-3.5 py-2.5">Mã Barcode</th>
                    <th scope="col" className="px-3.5 py-2.5">Thư viện</th>
                    <th scope="col" className="px-3.5 py-2.5">Vị trí kệ</th>
                    <th scope="col" className="px-3.5 py-2.5">Trạng thái</th>
                    {isLibrarianOrAdmin && (
                      <th scope="col" className="px-3.5 py-2.5 text-right">Thao tác</th>
                    )}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white text-slate-700">
                  {copies.map((copy) => (
                    <tr key={copy.id} className="hover:bg-slate-50">
                      <td className="px-3.5 py-2.5 font-mono font-semibold text-slate-900">
                        {copy.barcode}
                      </td>
                      <td className="px-3.5 py-2.5">{copy.libraryName}</td>
                      <td className="px-3.5 py-2.5 text-slate-500">{copy.location || 'Kệ chung'}</td>
                      <td className="px-3.5 py-2.5">
                        <BookCopyStatusBadge status={copy.status} />
                      </td>
                      {isLibrarianOrAdmin && (
                        <td className="px-3.5 py-2.5 text-right space-x-2">
                          {onEditCopy && (
                            <button
                              type="button"
                              onClick={() => onEditCopy(copy, book)}
                              className="text-xs font-semibold text-blue-700 hover:text-blue-900 hover:underline"
                            >
                              Sửa
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => handleDeleteCopy(copy)}
                            className="text-xs font-semibold text-rose-600 hover:text-rose-800 hover:underline"
                          >
                            Xóa
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="mt-6 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg bg-slate-100 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-200 transition-colors"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
}
