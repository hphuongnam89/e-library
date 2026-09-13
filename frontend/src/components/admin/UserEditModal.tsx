import { useState } from 'react';
import type { AdminUser } from '../../types/admin';
import type { DepartmentDto } from '../../types/organization';

interface UserEditModalProps {
  user: AdminUser;
  departments: DepartmentDto[];
  onSave: (userId: number, data: { role: AdminUser['role']; status: AdminUser['status']; departmentId: number | undefined }) => Promise<void>;
  onClose: () => void;
}

export function UserEditModal({ user, departments, onSave, onClose }: UserEditModalProps) {
  const [editRole, setEditRole] = useState<AdminUser['role']>(user.role);
  const [editStatus, setEditStatus] = useState<AdminUser['status']>(user.status);
  const [editDepartmentId, setEditDepartmentId] = useState<number | undefined>(user.departmentId ?? undefined);
  const [isUpdatingUser, setIsUpdatingUser] = useState(false);
  const [userModalError, setUserModalError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsUpdatingUser(true);
    setUserModalError(null);

    try {
      await onSave(user.id, {
        role: editRole,
        status: editStatus,
        departmentId: editDepartmentId,
      });
      onClose();
    } catch (err: unknown) {
      setUserModalError((err as Error).message || 'Cập nhật thất bại');
    } finally {
      setIsUpdatingUser(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
      <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
        <h3 className="text-lg font-bold text-slate-900 mb-2">Chỉnh Sửa Tài Khoản</h3>
        <p className="text-xs text-slate-500 mb-4">
          Người dùng: <strong>{user.fullName}</strong> ({user.email})
        </p>

        {userModalError && (
          <div className="mb-4 rounded-lg bg-red-50 p-3 text-xs text-red-700 border border-red-200">
            {userModalError}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="modal-edit-role" className="block text-xs font-semibold text-slate-700 mb-1">
              Vai trò (Role)
            </label>
            <select
              id="modal-edit-role"
              value={editRole}
              onChange={(e) => setEditRole(e.target.value as AdminUser['role'])}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-800 focus:outline-hidden"
            >
              <option value="STUDENT">Sinh viên (STUDENT)</option>
              <option value="LECTURER">Giảng viên (LECTURER)</option>
              <option value="LIBRARIAN">Thủ thư (LIBRARIAN)</option>
              <option value="ADMIN">Quản trị viên (ADMIN)</option>
            </select>
          </div>

          <div>
            <label htmlFor="modal-edit-status" className="block text-xs font-semibold text-slate-700 mb-1">
              Trạng thái (Status)
            </label>
            <select
              id="modal-edit-status"
              value={editStatus}
              onChange={(e) => setEditStatus(e.target.value as AdminUser['status'])}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-800 focus:outline-hidden"
            >
              <option value="ACTIVE">Hoạt động (ACTIVE)</option>
              <option value="INACTIVE">Tạm khóa (INACTIVE)</option>
            </select>
          </div>

          <div>
            <label htmlFor="modal-edit-dept" className="block text-xs font-semibold text-slate-700 mb-1">
              Khoa / Phòng ban
            </label>
            <select
              id="modal-edit-dept"
              value={editDepartmentId ?? ''}
              onChange={(e) =>
                setEditDepartmentId(e.target.value ? Number(e.target.value) : undefined)
              }
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-800 focus:outline-hidden"
            >
              <option value="">Chưa gán đơn vị</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>

          <div className="mt-6 flex justify-end gap-3">
            <button
              type="button"
              disabled={isUpdatingUser}
              onClick={onClose}
              className="rounded-lg border border-slate-300 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isUpdatingUser}
              className="rounded-lg bg-blue-900 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-800 disabled:opacity-50"
            >
              {isUpdatingUser ? 'Đang lưu...' : 'Lưu Thay Đổi'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
