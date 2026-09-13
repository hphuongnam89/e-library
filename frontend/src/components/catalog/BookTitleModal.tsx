import { useEffect, useState } from 'react';
import type { BookTitleDto, CategoryDto } from '../../types/catalog';
import { createBookTitle, updateBookTitle } from '../../api/catalog';

interface BookTitleModalProps {
  isOpen: boolean;
  bookToEdit: BookTitleDto | null;
  categories: CategoryDto[];
  onClose: () => void;
  onSuccess: (book: BookTitleDto) => void;
}

export function BookTitleModal({
  isOpen,
  bookToEdit,
  categories,
  onClose,
  onSuccess,
}: BookTitleModalProps) {
  const [title, setTitle] = useState('');
  const [author, setAuthor] = useState('');
  const [publisher, setPublisher] = useState('');
  const [isbn, setIsbn] = useState('');
  const [publicationYear, setPublicationYear] = useState<number | ''>('');
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (bookToEdit) {
      setTitle(bookToEdit.title);
      setAuthor(bookToEdit.author || '');
      setPublisher(bookToEdit.publisher || '');
      setIsbn(bookToEdit.isbn || '');
      setPublicationYear(bookToEdit.publicationYear || '');
      setCategoryId(bookToEdit.categoryId ?? '');
    } else {
      setTitle('');
      setAuthor('');
      setPublisher('');
      setIsbn('');
      setPublicationYear(new Date().getFullYear());
      setCategoryId('');
    }
    setError(null);
  }, [bookToEdit, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanTitle = title.trim();
    if (!cleanTitle) {
      setError('Vui lòng nhập nhan đề sách.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    const payload = {
      title: cleanTitle,
      author: author.trim() || undefined,
      publisher: publisher.trim() || undefined,
      isbn: isbn.trim() || undefined,
      publicationYear: publicationYear ? Number(publicationYear) : undefined,
      categoryId: categoryId !== '' ? Number(categoryId) : null,
    };

    try {
      if (bookToEdit) {
        const updated = await updateBookTitle(bookToEdit.id, payload);
        onSuccess(updated);
      } else {
        const created = await createBookTitle(payload);
        onSuccess(created);
      }
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể lưu thông tin nhan đề sách.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="book-title-modal-heading"
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 sm:p-6"
    >
      <div className="relative w-full max-w-lg rounded-2xl bg-white p-6 sm:p-8 shadow-2xl">
        <button
          type="button"
          onClick={onClose}
          aria-label="Đóng"
          className="absolute right-4 top-4 rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition-colors"
        >
          ✕
        </button>

        <h2 id="book-title-modal-heading" className="text-xl font-bold text-slate-900">
          {bookToEdit ? 'Chỉnh sửa Nhan đề Sách' : 'Thêm Nhan đề Sách Mới'}
        </h2>
        <p className="mt-1 text-xs text-slate-500">
          Nhập các thông tin thư mục của sách để quản lý trong mục lục OPAC.
        </p>

        {error && (
          <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          <div>
            <label htmlFor="title-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Nhan đề sách *
            </label>
            <input
              id="title-input"
              type="text"
              required
              maxLength={500}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Nhập tên sách đầy đủ"
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label htmlFor="author-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                Tác giả
              </label>
              <input
                id="author-input"
                type="text"
                maxLength={255}
                value={author}
                onChange={(e) => setAuthor(e.target.value)}
                placeholder="Tên tác giả"
                className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
              />
            </div>
            <div>
              <label htmlFor="publisher-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                Nhà xuất bản
              </label>
              <input
                id="publisher-input"
                type="text"
                maxLength={255}
                value={publisher}
                onChange={(e) => setPublisher(e.target.value)}
                placeholder="Tên NXB"
                className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label htmlFor="isbn-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                Mã ISBN
              </label>
              <input
                id="isbn-input"
                type="text"
                maxLength={20}
                value={isbn}
                onChange={(e) => setIsbn(e.target.value)}
                placeholder="Ví dụ: 978-604-0-12345-6"
                className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm font-mono text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
              />
            </div>
            <div>
              <label htmlFor="pub-year-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                Năm xuất bản
              </label>
              <input
                id="pub-year-input"
                type="number"
                min={1900}
                max={2100}
                value={publicationYear}
                onChange={(e) => setPublicationYear(e.target.value ? Number(e.target.value) : '')}
                placeholder="2024"
                className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
              />
            </div>
          </div>

          <div>
            <label htmlFor="category-select" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Danh mục chuyên ngành
            </label>
            <select
              id="category-select"
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : '')}
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
            >
              <option value="">-- Chưa gán danh mục --</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
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
              {isSubmitting ? 'Đang lưu…' : bookToEdit ? 'Lưu thay đổi' : 'Tạo nhan đề mới'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
