import { useState, useEffect } from 'react';
import type { FormEvent } from 'react';
import { uploadDigitalDocument } from '../../api/digital';
import { fetchLibraries } from '../../api/organization';
import { fetchCategories } from '../../api/catalog';
import type { LibraryDto } from '../../types/organization';
import type { CategoryDto } from '../../types/catalog';
import type { DigitalDocumentPermission } from '../../types/digital';

interface DigitalDocUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function DigitalDocUploadModal({
  isOpen,
  onClose,
  onSuccess,
}: DigitalDocUploadModalProps) {
  const [file, setFile] = useState<File | null>(null);
  const [libraryId, setLibraryId] = useState<number | ''>('');
  const [title, setTitle] = useState('');
  const [publisher, setPublisher] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [permission, setPermission] = useState<DigitalDocumentPermission>('RESTRICTED');

  const [libraries, setLibraries] = useState<LibraryDto[]>([]);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) return;

    fetchLibraries(undefined, 0, 100)
      .then((page) => {
        setLibraries(page.content);
        if (page.content.length > 0) {
          const first = page.content[0];
          if (first) setLibraryId(first.id);
        }
      })
      .catch(() => {});

    fetchCategories()
      .then((page) => setCategories(page.content))
      .catch(() => {});

    setFile(null);
    setTitle('');
    setPublisher('');
    setDescription('');
    setCategoryId('');
    setPermission('RESTRICTED');
    setError(null);
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!file) {
      setError('Vui lòng chọn tệp PDF tải lên');
      return;
    }
    if (!libraryId) {
      setError('Vui lòng chọn thư viện lưu trữ');
      return;
    }
    if (!title.trim()) {
      setError('Tiêu đề tài liệu không được để trống');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('libraryId', String(libraryId));
      formData.append('title', title.trim());
      if (publisher.trim()) formData.append('publisher', publisher.trim());
      if (description.trim()) formData.append('description', description.trim());
      if (categoryId) formData.append('categoryId', String(categoryId));
      formData.append('permission', permission);

      await uploadDigitalDocument(formData);
      onSuccess();
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Lỗi khi tải lên tài liệu');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="upload-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-xs"
    >
      <div className="relative w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl transition-all">
        <div className="flex items-center justify-between border-b border-slate-100 pb-4">
          <h3 id="upload-modal-title" className="text-lg font-bold text-slate-900">
            Tải lên Tài liệu Số Mới (PDF)
          </h3>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
          >
            <span className="sr-only">Đóng</span>
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {error && (
          <div role="alert" className="mt-4 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          {/* File Picker */}
          <div>
            <label htmlFor="file-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Tệp PDF * (Tối đa 50MB)
            </label>
            <input
              id="file-input"
              type="file"
              accept="application/pdf,.pdf"
              required
              onChange={(e) => {
                const selected = e.target.files?.[0];
                if (selected) {
                  setFile(selected);
                  if (!title) {
                    const cleanName = selected.name.replace(/\.[^/.]+$/, '');
                    setTitle(cleanName);
                  }
                }
              }}
              className="w-full text-sm text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
            />
          </div>

          <div>
            <label htmlFor="upload-title" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Tiêu đề tài liệu *
            </label>
            <input
              id="upload-title"
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Nhập tên giáo trình, tài liệu số..."
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 focus:border-blue-900 focus:ring-1 focus:ring-blue-900"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="upload-library" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                Thư viện chủ quản *
              </label>
              <select
                id="upload-library"
                required
                value={libraryId}
                onChange={(e) => setLibraryId(e.target.value ? Number(e.target.value) : '')}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-blue-900 focus:ring-1 focus:ring-blue-900"
              >
                <option value="">-- Chọn thư viện --</option>
                {libraries.map((lib) => (
                  <option key={lib.id} value={lib.id}>
                    {lib.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label htmlFor="upload-category" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                Danh mục
              </label>
              <select
                id="upload-category"
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : '')}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-blue-900 focus:ring-1 focus:ring-blue-900"
              >
                <option value="">-- Không phân loại --</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="upload-publisher" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                Nhà xuất bản / Tác giả
              </label>
              <input
                id="upload-publisher"
                type="text"
                value={publisher}
                onChange={(e) => setPublisher(e.target.value)}
                placeholder="NXB hoặc tác giả..."
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-blue-900 focus:ring-1 focus:ring-blue-900"
              />
            </div>

            <div>
              <label htmlFor="upload-permission" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                Quyền truy cập
              </label>
              <select
                id="upload-permission"
                value={permission}
                onChange={(e) => setPermission(e.target.value as DigitalDocumentPermission)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-blue-900 focus:ring-1 focus:ring-blue-900"
              >
                <option value="RESTRICTED">Giới hạn (Theo Grant)</option>
                <option value="AUTHENTICATED">Công khai toàn trường</option>
              </select>
            </div>
          </div>

          <div>
            <label htmlFor="upload-desc" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Mô tả tóm tắt
            </label>
            <textarea
              id="upload-desc"
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Tóm tắt nội dung tài liệu..."
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-blue-900 focus:ring-1 focus:ring-blue-900"
            />
          </div>

          <div className="mt-6 flex justify-end space-x-3 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="rounded-lg bg-blue-900 px-5 py-2 text-sm font-semibold text-white shadow-xs hover:bg-blue-800 disabled:opacity-50"
            >
              {isSubmitting ? 'Đang tải lên...' : 'Tải lên tài liệu'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
