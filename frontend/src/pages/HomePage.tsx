import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function HomePage() {
  const { user, isAuthenticated, login } = useAuth();
  const isLibrarianOrAdmin =
    isAuthenticated && (user?.role === 'LIBRARIAN' || user?.role === 'ADMIN');
  const isAdmin = isAuthenticated && user?.role === 'ADMIN';

  return (
    <section className="py-12 sm:py-20">
      <p className="mb-4 text-sm font-semibold uppercase tracking-widest text-blue-700">
        E-LIB · Đại học Phú Xuân
      </p>
      <h1 className="max-w-3xl text-4xl font-semibold leading-tight tracking-tight text-slate-950 sm:text-6xl">
        Kết nối tri thức.<br />Mở rộng tương lai.
      </h1>
      <p className="mt-7 max-w-xl text-lg leading-8 text-slate-600">
        Hệ thống Thư viện thông minh hỗ trợ tra cứu trực tuyến mục lục sách (OPAC), quầy lưu thông mượn trả thời gian thực với máy quét mã vạch và quản lý cơ cấu tổ chức học thuật hiệu quả.
      </p>
      <div className="mt-8 flex flex-wrap items-center gap-4">
        <Link
          className="rounded-xl bg-blue-900 px-6 py-3 text-sm font-semibold text-white shadow-xs hover:bg-blue-800 transition-colors inline-flex items-center"
          to="/catalog"
        >
          Tra cứu mục lục sách (OPAC) <span aria-hidden="true" className="ml-2">→</span>
        </Link>
        <Link
          className="rounded-xl border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors inline-flex items-center"
          to="/digital-documents"
        >
          📖 Tài liệu số
        </Link>
        {isAuthenticated ? (
          <>
            <Link
              className="rounded-xl border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors inline-flex items-center"
              to="/my-borrows"
            >
              Sách của tôi
            </Link>
            {isLibrarianOrAdmin && (
              <Link
                className="rounded-xl bg-amber-600 px-5 py-3 text-sm font-semibold text-white hover:bg-amber-700 transition-colors inline-flex items-center shadow-xs"
                to="/circulation"
              >
                ⚡ Quầy Lưu Thông
              </Link>
            )}
            {isAdmin && (
              <Link
                className="rounded-xl border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors inline-flex items-center"
                to="/organization"
              >
                🏛 Cơ Cấu Tổ Chức
              </Link>
            )}
            {isAdmin && (
              <Link
                className="rounded-xl border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors inline-flex items-center"
                to="/admin"
              >
                ⚙️ Quản Trị Hệ Thống
              </Link>
            )}
          </>
        ) : (
          <button
            type="button"
            onClick={login}
            className="rounded-xl border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors"
          >
            Đăng nhập tài khoản trường
          </button>
        )}
      </div>
    </section>
  );
}
