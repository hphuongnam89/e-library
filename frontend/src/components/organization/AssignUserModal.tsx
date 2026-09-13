import { useState } from 'react';
import type { DepartmentDto } from '../../types/organization';
import { assignUserDepartment } from '../../api/organization';

interface AssignUserModalProps {
  isOpen: boolean;
  departments: DepartmentDto[];
  onClose: () => void;
  onSuccess: (message: string) => void;
}

export function AssignUserModal({
  isOpen,
  departments,
  onClose,
  onSuccess,
}: AssignUserModalProps) {
  const [userId, setUserId] = useState('');
  const [departmentId, setDepartmentId] = useState<number | ''>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const uid = Number(userId.trim());
    if (!uid || isNaN(uid)) {
      setError('Vui lòng nhập mã ID người dùng (số nguyên).');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const updatedUser = await assignUserDepartment(
        uid,
        departmentId !== '' ? Number(departmentId) : null
      );
      onSuccess(
        `Đã phân bổ người dùng "${updatedUser.fullName}" vào phòng ban thành công!`
      );
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể gán phòng ban cho người dùng.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="assign-user-modal-title"
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

        <h2 id="assign-user-modal-title" className="text-xl font-bold text-slate-900">
          Gán Người Dùng Vào Phòng Ban
        </h2>
        <p className="mt-1 text-xs text-slate-500">
          Chỉ định khoa hoặc phòng ban quản lý cho tài khoản người dùng theo quyết định D017.
        </p>

        {error && (
          <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          <div>
            <label htmlFor="user-id-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              ID Người dùng (User ID) *
            </label>
            <input
              id="user-id-input"
              type="number"
              required
              min={1}
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              placeholder="Ví dụ: 1"
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
            />
          </div>

          <div>
            <label htmlFor="department-select" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Chọn Khoa / Phòng ban
            </label>
            <select
              id="department-select"
              value={departmentId}
              onChange={(e) => setDepartmentId(e.target.value ? Number(e.target.value) : '')}
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
            >
              <option value="">-- Hủy gán (Không thuộc phòng ban) --</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name} ({d.libraryName})
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
              {isSubmitting ? 'Đang cập nhật…' : 'Xác nhận gán'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
