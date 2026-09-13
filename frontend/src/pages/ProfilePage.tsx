import { useAuth } from '../hooks/useAuth';
import { Navigate, Link } from 'react-router-dom';

export function ProfilePage() {
  const { user, isAuthenticated, isLoading, logout } = useAuth();

  if (isLoading) {
    return (
      <div className="py-12 flex justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 border-t-transparent"></div>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/" replace />;
  }

  const roleText = {
    STUDENT: 'Sinh viên',
    LECTURER: 'Giảng viên',
    LIBRARIAN: 'Thủ thư',
    ADMIN: 'Quản trị viên',
  }[user.role];

  return (
    <div className="mx-auto max-w-3xl py-8 sm:py-12">
      <div className="mb-8 px-4 sm:px-0">
        <h1 className="text-2xl font-bold text-slate-900">Hồ sơ cá nhân</h1>
        <p className="text-slate-500">Quản lý thông tin và hoạt động của bạn tại thư viện</p>
      </div>

      <div className="flex flex-col gap-6">
        {/* Thông tin cá nhân */}
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs">
          <div className="px-6 py-8 sm:p-10">
            <div className="flex flex-col sm:flex-row items-center sm:items-start gap-6 text-center sm:text-left">
              <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-full bg-blue-100 text-4xl font-bold text-blue-700">
                {user.fullName.charAt(0).toUpperCase()}
              </div>
              <div className="flex-1">
                <h2 className="text-2xl font-bold text-slate-900">{user.fullName}</h2>
                <p className="text-slate-500 mt-1">{user.email}</p>
                <div className="mt-3 inline-flex items-center rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
                  {roleText}
                </div>
              </div>
            </div>

            <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="rounded-lg border border-slate-100 bg-slate-50 p-4">
                <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Mã số (MSSV/MGV)</p>
                <p className="mt-1.5 font-semibold text-slate-900 text-lg">{user.studentCode || 'Chưa cập nhật'}</p>
              </div>
              <div className="rounded-lg border border-slate-100 bg-slate-50 p-4">
                <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Trạng thái tài khoản</p>
                <div className="mt-1.5 flex items-center gap-2">
                  <span className="flex h-2.5 w-2.5 rounded-full bg-emerald-500"></span>
                  <p className="font-semibold text-slate-900 text-lg">Đang hoạt động</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Hoạt động của tôi */}
        <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs">
          <div className="border-b border-slate-100 px-6 py-4">
            <h3 className="text-lg font-bold text-slate-900">Hoạt động của tôi</h3>
          </div>
          <div className="grid grid-cols-1 divide-y divide-slate-100 sm:grid-cols-2 sm:divide-x sm:divide-y-0">
            <Link
              to="/my-borrows"
              className="flex items-start gap-4 p-6 hover:bg-slate-50 transition-colors"
            >
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-amber-100 text-amber-600">
                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div>
                <h4 className="font-semibold text-slate-900">Sách đang mượn & Lịch sử</h4>
                <p className="mt-1 text-sm text-slate-500">Xem danh sách sách vật lý đang mượn, ngày hạn trả và lịch sử mượn trước đây.</p>
              </div>
            </Link>

            <Link
              to="/reading-history"
              className="flex items-start gap-4 p-6 hover:bg-slate-50 transition-colors"
            >
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-indigo-100 text-indigo-600">
                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <div>
                <h4 className="font-semibold text-slate-900">Tài liệu số đã đọc</h4>
                <p className="mt-1 text-sm text-slate-500">Xem lại các tài liệu điện tử bạn đã mở và tổng thời gian đọc trên hệ thống.</p>
              </div>
            </Link>
          </div>
        </section>

        {/* Hành động */}
        <section className="mt-4 px-4 sm:px-0">
          <button
            onClick={logout}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-red-50 px-4 py-4 text-sm font-semibold text-red-600 hover:bg-red-100 transition-colors"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            Đăng xuất khỏi hệ thống
          </button>
        </section>
      </div>
    </div>
  );
}
