import { useState, useCallback, useEffect } from 'react';
import type { DepartmentDto } from '../../types/organization';
import type { AdminUser } from '../../types/admin';
import { fetchAdminUsers, updateAdminUser, type AdminUserFilterParams } from '../../api/admin';
import { UserEditModal } from '../../components/admin/UserEditModal';
import { UserImportModal } from '../../components/admin/UserImportModal';

interface AdminUsersTabProps {
  departments: DepartmentDto[];
  onSuccess: (message: string) => void;
  onError: (message: string) => void;
}

export function AdminUsersTab({ departments, onSuccess, onError }: AdminUsersTabProps) {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [usersTotalPages, setUsersTotalPages] = useState(1);
  const [usersTotalElements, setUsersTotalElements] = useState(0);
  const [isUsersLoading, setIsUsersLoading] = useState(false);
  const [userFilters, setUserFilters] = useState<AdminUserFilterParams>({
    search: '',
    role: '',
    status: '',
    departmentId: undefined,
    page: 0,
    size: 15,
  });

  const [editingUser, setEditingUser] = useState<AdminUser | null>(null);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);

  const loadUsers = useCallback((params: AdminUserFilterParams, signal?: AbortSignal) => {
    setIsUsersLoading(true);
    fetchAdminUsers(params, signal)
      .then((res) => {
        setUsers(res.content);
        setUsersTotalPages(res.totalPages);
        setUsersTotalElements(res.totalElements);
      })
      .catch((err) => {
        if (!signal?.aborted) onError(err.message || 'Không thể tải danh sách người dùng');
      })
      .finally(() => {
        if (!signal?.aborted) setIsUsersLoading(false);
      });
  }, [onError]);

  useEffect(() => {
    const controller = new AbortController();
    loadUsers(userFilters, controller.signal);
    return () => controller.abort();
  }, [userFilters, loadUsers]);

  const handleSaveUser = async (userId: number, data: { role: AdminUser['role']; status: AdminUser['status']; departmentId: number | undefined }) => {
    await updateAdminUser(userId, data);
    const userEmail = users.find((u) => u.id === userId)?.email || '';
    onSuccess(`Đã cập nhật thông tin tài khoản ${userEmail}`);
    loadUsers(userFilters);
  };

  return (
    <div>
      <div className="mb-4 flex justify-end">
        <button
          type="button"
          onClick={() => setIsImportModalOpen(true)}
          className="inline-flex items-center justify-center rounded-lg bg-emerald-700 px-4 py-2 text-sm font-semibold text-white shadow-xs hover:bg-emerald-800 transition-colors"
        >
          📥 Nhập Excel hàng loạt
        </button>
      </div>

      {/* User Filters */}
      <div className="mb-6 rounded-xl border border-slate-200 bg-white p-4 shadow-xs">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <label htmlFor="user-search-input" className="block text-xs font-semibold text-slate-600 mb-1">
              Tìm kiếm
            </label>
            <input
              id="user-search-input"
              type="text"
              placeholder="Email, họ tên, mã SV..."
              value={userFilters.search ?? ''}
              onChange={(e) =>
                setUserFilters((prev: AdminUserFilterParams) => ({ ...prev, search: e.target.value, page: 0 }))
              }
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-800 focus:outline-hidden"
            />
          </div>

          <div>
            <label htmlFor="user-role-select" className="block text-xs font-semibold text-slate-600 mb-1">
              Vai trò
            </label>
            <select
              id="user-role-select"
              value={userFilters.role ?? ''}
              onChange={(e) =>
                setUserFilters((prev: AdminUserFilterParams) => ({ ...prev, role: e.target.value, page: 0 }))
              }
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-800 focus:outline-hidden"
            >
              <option value="">Tất cả vai trò</option>
              <option value="STUDENT">Sinh viên (STUDENT)</option>
              <option value="LECTURER">Giảng viên (LECTURER)</option>
              <option value="LIBRARIAN">Thủ thư (LIBRARIAN)</option>
              <option value="ADMIN">Quản trị viên (ADMIN)</option>
            </select>
          </div>

          <div>
            <label htmlFor="user-status-select" className="block text-xs font-semibold text-slate-600 mb-1">
              Trạng thái
            </label>
            <select
              id="user-status-select"
              value={userFilters.status ?? ''}
              onChange={(e) =>
                setUserFilters((prev: AdminUserFilterParams) => ({ ...prev, status: e.target.value, page: 0 }))
              }
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-800 focus:outline-hidden"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Hoạt động (ACTIVE)</option>
              <option value="INACTIVE">Tạm khóa (INACTIVE)</option>
            </select>
          </div>

          <div>
            <label htmlFor="user-dept-select" className="block text-xs font-semibold text-slate-600 mb-1">
              Khoa / Phòng ban
            </label>
            <select
              id="user-dept-select"
              value={userFilters.departmentId ?? ''}
              onChange={(e) =>
                setUserFilters((prev: AdminUserFilterParams) => ({
                  ...prev,
                  departmentId: e.target.value ? Number(e.target.value) : undefined,
                  page: 0,
                }))
              }
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-800 focus:outline-hidden"
            >
              <option value="">Tất cả đơn vị</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Users Table */}
      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Người dùng</th>
                <th className="px-4 py-3">Mã SV / GV</th>
                <th className="px-4 py-3">Vai trò</th>
                <th className="px-4 py-3">Trạng thái</th>
                <th className="px-4 py-3">Khoa / Đơn vị</th>
                <th className="px-4 py-3">Ngày tạo</th>
                <th className="px-4 py-3 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isUsersLoading ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-slate-400">
                    Đang tải danh sách người dùng...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-slate-400">
                    Không tìm thấy người dùng phù hợp.
                  </td>
                </tr>
              ) : (
                users.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50/50">
                    <td className="px-4 py-3">
                      <div className="font-semibold text-slate-900">{u.fullName}</div>
                      <div className="text-xs text-slate-400">{u.email}</div>
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-slate-700">
                      {u.studentCode || '—'}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                          u.role === 'ADMIN'
                            ? 'bg-purple-100 text-purple-800'
                            : u.role === 'LIBRARIAN'
                            ? 'bg-amber-100 text-amber-800'
                            : u.role === 'LECTURER'
                            ? 'bg-blue-100 text-blue-800'
                            : 'bg-emerald-100 text-emerald-800'
                        }`}
                      >
                        {u.role}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                          u.status === 'ACTIVE'
                            ? 'bg-green-100 text-green-800'
                            : 'bg-rose-100 text-rose-800'
                        }`}
                      >
                        {u.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm khóa'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-slate-700">
                      {u.departmentName || '—'}
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-400">
                      {new Date(u.createdAt).toLocaleDateString('vi-VN')}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        type="button"
                        onClick={() => setEditingUser(u)}
                        className="rounded-md border border-slate-200 px-2.5 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition-colors"
                      >
                        Sửa
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-xs text-slate-500">
          <span>
            Tổng cộng <strong>{usersTotalElements}</strong> người dùng
          </span>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={userFilters.page === 0}
              onClick={() =>
                setUserFilters((prev: AdminUserFilterParams) => ({ ...prev, page: Math.max(0, (prev.page ?? 0) - 1) }))
              }
              className="rounded-md border border-slate-200 px-3 py-1 disabled:opacity-40"
            >
              Trang trước
            </button>
            <span className="px-2 py-1">
              Trang {(userFilters.page ?? 0) + 1} / {Math.max(1, usersTotalPages)}
            </span>
            <button
              type="button"
              disabled={(userFilters.page ?? 0) + 1 >= usersTotalPages}
              onClick={() =>
                setUserFilters((prev: AdminUserFilterParams) => ({ ...prev, page: (prev.page ?? 0) + 1 }))
              }
              className="rounded-md border border-slate-200 px-3 py-1 disabled:opacity-40"
            >
              Trang sau
            </button>
          </div>
        </div>
      </div>

      {editingUser && (
        <UserEditModal
          user={editingUser}
          departments={departments}
          onSave={handleSaveUser}
          onClose={() => setEditingUser(null)}
        />
      )}

      {isImportModalOpen && (
        <UserImportModal
          onImportComplete={(res) => {
            onSuccess(`Nhập Excel hoàn tất: ${res.importedCount} thành công, ${res.failedCount} lỗi.`);
            loadUsers(userFilters);
          }}
          onClose={() => setIsImportModalOpen(false)}
        />
      )}
    </div>
  );
}
