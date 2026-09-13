import { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { fetchDepartments } from '../api/organization';
import type { DepartmentDto } from '../types/organization';
import { AdminUsersTab } from './admin/AdminUsersTab';
import { AdminAuditTab } from './admin/AdminAuditTab';
import { AdminSettingsTab } from './admin/AdminSettingsTab';

type AdminTab = 'users' | 'audit' | 'settings';

export function AdminPage() {
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const isAdmin = isAuthenticated && user?.role === 'ADMIN';

  const [activeTab, setActiveTab] = useState<AdminTab>('users');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [departments, setDepartments] = useState<DepartmentDto[]>([]);

  useEffect(() => {
    if (isAdmin) {
      fetchDepartments(undefined, 0, 100)
        .then((res) => setDepartments(res.content))
        .catch(() => {});
    }
  }, [isAdmin]);

  if (isAuthLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <span className="text-slate-500">Đang tải quyền truy cập...</span>
      </div>
    );
  }

  if (!isAdmin) {
    return (
      <div className="mx-auto my-12 max-w-lg rounded-xl border border-red-200 bg-red-50 p-6 text-center text-red-700">
        <h2 className="text-lg font-bold">Truy Cập Bị Từ Chối</h2>
        <p className="mt-2 text-sm">Trang này dành riêng cho Quản trị viên hệ thống (Role: ADMIN).</p>
      </div>
    );
  }

  const handleSuccess = (message: string) => {
    setSuccess(message);
    setError(null);
  };

  const handleError = (message: string) => {
    setError(message);
    setSuccess(null);
  };

  return (
    <div className="py-8">
      {/* Header */}
      <div className="mb-6 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Quản Trị Hệ Thống</h1>
          <p className="text-sm text-slate-500">
            Quản lý người dùng, phân quyền, nhập liệu hàng loạt, nhật ký kiểm toán và cấu hình toàn cục.
          </p>
        </div>
      </div>

      {/* Global Alerts */}
      {error && (
        <div className="mb-4 rounded-lg bg-red-50 p-4 text-sm text-red-700 border border-red-200">
          {error}
        </div>
      )}
      {success && (
        <div className="mb-4 flex items-center justify-between rounded-lg bg-emerald-50 p-4 text-sm text-emerald-700 border border-emerald-200">
          <span>{success}</span>
          <button
            type="button"
            onClick={() => setSuccess(null)}
            className="text-emerald-900 font-bold hover:underline"
          >
            ✕
          </button>
        </div>
      )}

      {/* Tab Navigation */}
      <div className="mb-6 flex border-b border-slate-200">
        <button
          type="button"
          onClick={() => {
            setActiveTab('users');
            setError(null);
            setSuccess(null);
          }}
          className={`px-6 py-3 text-sm font-medium border-b-2 transition-colors ${
            activeTab === 'users'
              ? 'border-blue-900 text-blue-900 font-bold'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          👤 Người Dùng & Phân Quyền
        </button>
        <button
          type="button"
          onClick={() => {
            setActiveTab('audit');
            setError(null);
            setSuccess(null);
          }}
          className={`px-6 py-3 text-sm font-medium border-b-2 transition-colors ${
            activeTab === 'audit'
              ? 'border-blue-900 text-blue-900 font-bold'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          📜 Nhật Ký Kiểm Toán (Audit Log)
        </button>
        <button
          type="button"
          onClick={() => {
            setActiveTab('settings');
            setError(null);
            setSuccess(null);
          }}
          className={`px-6 py-3 text-sm font-medium border-b-2 transition-colors ${
            activeTab === 'settings'
              ? 'border-blue-900 text-blue-900 font-bold'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          ⚙️ Cấu Hình Hệ Thống
        </button>
      </div>

      {activeTab === 'users' && (
        <AdminUsersTab
          departments={departments}
          onSuccess={handleSuccess}
          onError={handleError}
        />
      )}
      {activeTab === 'audit' && (
        <AdminAuditTab onError={handleError} />
      )}
      {activeTab === 'settings' && (
        <AdminSettingsTab
          onSuccess={handleSuccess}
          onError={handleError}
        />
      )}
    </div>
  );
}
