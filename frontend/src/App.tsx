import { BrowserRouter, Link, NavLink, Route, Routes } from 'react-router-dom';
import { SystemStatus } from './pages/SystemStatus';
import { ErrorBoundary } from './components/ErrorBoundary';
import { useAuth } from './hooks/useAuth';
import { UserMenu } from './components/UserMenu';

function Home() {
  return <section className="py-12 sm:py-20">
    <p className="mb-4 text-sm font-semibold uppercase tracking-widest text-blue-700">E-LIB · Đại học Phú Xuân</p>
    <h1 className="max-w-3xl text-4xl font-semibold leading-tight tracking-tight text-slate-950 sm:text-6xl">Kết nối tri thức.<br />Mở rộng tương lai.</h1>
    <p className="mt-7 max-w-xl text-lg leading-8 text-slate-600">Thư viện đang được chuẩn bị để phục vụ bạn. Chức năng đăng nhập, tra cứu, mượn sách và đọc tài liệu sẽ được mở trong các giai đoạn tiếp theo.</p>
    <Link className="button mt-8 inline-flex" to="/system">Xem trạng thái hệ thống <span aria-hidden="true" className="ml-3">→</span></Link>
  </section>;
}
function NotFound() {
  return <section className="py-16"><p className="text-sm text-slate-500">404</p>
    <h1 className="mt-3 text-3xl font-semibold">Không tìm thấy trang</h1>
    <Link className="button mt-6 inline-flex" to="/">Về trang chủ</Link></section>;
}
export default function App() {
  const { user, isAuthenticated, isLoading, login, logout } = useAuth();

  return <ErrorBoundary><BrowserRouter>
    <a href="#main" className="skip-link">Bỏ qua điều hướng</a>
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-5 px-6 py-5">
          <div className="flex items-center gap-8">
            <Link to="/" aria-label="E-LIB — Trang chủ" className="text-2xl font-bold tracking-tight text-blue-900">E-LIB<span className="text-blue-500">.</span></Link>
            <nav aria-label="Điều hướng chính" className="flex gap-6 text-sm font-medium">
              <NavLink end to="/" className={({ isActive }) => isActive ? 'text-blue-800 underline underline-offset-8' : 'text-slate-600'}>Trang chủ</NavLink>
              <NavLink to="/system" className={({ isActive }) => isActive ? 'text-blue-800 underline underline-offset-8' : 'text-slate-600'}>Trạng thái hệ thống</NavLink>
            </nav>
          </div>
          <div>
            {isLoading ? (
              <span className="text-xs text-slate-400">Đang tải…</span>
            ) : isAuthenticated && user ? (
              <UserMenu user={user} onLogout={logout} />
            ) : (
              <button
                type="button"
                onClick={login}
                className="rounded-lg bg-blue-900 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-800 transition-colors"
              >
                Đăng nhập Google
              </button>
            )}
          </div>
        </div>
      </header>
      <main id="main" tabIndex={-1} className="mx-auto w-full max-w-6xl flex-1 px-6">
        <Routes><Route path="/" element={<Home />} /><Route path="/system" element={<SystemStatus />} /><Route path="*" element={<NotFound />} /></Routes>
      </main>
      <footer className="border-t border-slate-200 px-6 py-6 text-sm text-slate-500"><div className="mx-auto max-w-6xl">Thư viện Đại học Phú Xuân · E-LIB</div></footer>
    </div>
  </BrowserRouter></ErrorBoundary>;
}
