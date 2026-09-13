import { useEffect, useState } from 'react';
import type { CategoryDto } from '../../types/catalog';
import { createCategory, updateCategory } from '../../api/catalog';

interface CategoryModalProps {
  isOpen: boolean;
  categoryToEdit: CategoryDto | null;
  categories: CategoryDto[];
  onClose: () => void;
  onSuccess: () => void;
}

export function CategoryModal({
  isOpen,
  categoryToEdit,
  categories,
  onClose,
  onSuccess,
}: CategoryModalProps) {
  const [name, setName] = useState('');
  const [parentId, setParentId] = useState<number | ''>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (categoryToEdit) {
      setName(categoryToEdit.name);
      setParentId(categoryToEdit.parentId ?? '');
    } else {
      setName('');
      setParentId('');
    }
    setError(null);
  }, [categoryToEdit, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanName = name.trim();
    if (!cleanName) {
      setError('Vui lòng nhập tên danh mục.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      if (categoryToEdit) {
        await updateCategory(categoryToEdit.id, {
          name: cleanName,
          parentId: parentId !== '' ? Number(parentId) : null,
        });
      } else {
        await createCategory({
          name: cleanName,
          parentId: parentId !== '' ? Number(parentId) : null,
        });
      }
      onSuccess();
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể lưu danh mục.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="category-modal-heading"
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

        <h2 id="category-modal-heading" className="text-xl font-bold text-slate-900">
          {categoryToEdit ? 'Chỉnh sửa Danh mục' : 'Thêm Danh mục Mới'}
        </h2>
        <p className="mt-1 text-xs text-slate-500">
          Phân loại chuyên ngành và thể loại sách trong hệ thống thư viện.
        </p>

        {error && (
          <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          <div>
            <label htmlFor="category-name-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Tên danh mục *
            </label>
            <input
              id="category-name-input"
              type="text"
              required
              maxLength={255}
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ví dụ: Kỹ thuật phần mềm"
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
            />
          </div>

          <div>
            <label htmlFor="category-parent-select" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Danh mục cha (Tùy chọn)
            </label>
            <select
              id="category-parent-select"
              value={parentId}
              onChange={(e) => setParentId(e.target.value ? Number(e.target.value) : '')}
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
            >
              <option value="">-- Danh mục gốc (Cấp cao nhất) --</option>
              {categories
                .filter((c) => !categoryToEdit || c.id !== categoryToEdit.id)
                .map((c) => (
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
              {isSubmitting ? 'Đang lưu…' : categoryToEdit ? 'Lưu thay đổi' : 'Tạo danh mục'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
