import { BrowserRouter, Link, NavLink, Route, Routes } from 'react-router-dom';
import { SystemStatus } from './pages/SystemStatus';
import { CatalogPage } from './pages/CatalogPage';
import { MyBorrowsPage } from './pages/MyBorrowsPage';
import { CirculationDeskPage } from './pages/CirculationDeskPage';
import { OrganizationPage } from './pages/OrganizationPage';
import { DigitalDocumentsPage } from './pages/DigitalDocumentsPage';
import { ReadingHistoryPage } from './pages/ReadingHistoryPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { DashboardPage } from './pages/DashboardPage';
import { AdminPage } from './pages/AdminPage';
import { HomePage } from './pages/HomePage';
import { NotFoundPage } from './pages/NotFoundPage';
import { ProfilePage } from './pages/ProfilePage';
import { NotificationBell } from './components/NotificationBell';
import { ErrorBoundary } from './components/ErrorBoundary';
import { useAuth } from './hooks/useAuth';
import { UserMenu } from './components/UserMenu';
import { MobileBottomNav } from './components/MobileBottomNav';

export default function App() {
  const { user, isAuthenticated, isLoading, login, logout } = useAuth();
  const isLibrarianOrAdmin =
    isAuthenticated && (user?.role === 'LIBRARIAN' || user?.role === 'ADMIN');
  const isAdmin = isAuthenticated && user?.role === 'ADMIN';

  return (
    <ErrorBoundary>
      <BrowserRouter>
        <a href="#main" className="skip-link">
          Bỏ qua điều hướng
        </a>
        <div className="flex min-h-screen flex-col bg-slate-50/50 pb-16 sm:pb-0">
          <header className="border-b border-slate-200 bg-white sticky top-0 z-40">
            <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-5 px-6 py-4">
              <div className="flex items-center gap-8">
                <Link
                  to="/"
                  aria-label="E-LIB — Trang chủ"
                  className="text-2xl font-black tracking-tight text-blue-900"
                >
                  E-LIB<span className="text-blue-500">.</span>
                </Link>
                <nav aria-label="Điều hướng chính" className="hidden sm:flex items-center gap-5 text-sm font-medium">
                  <NavLink
                    end
                    to="/"
                    className={({ isActive }) =>
                      isActive
                        ? 'text-blue-900 font-semibold underline underline-offset-8 decoration-2'
                        : 'text-slate-600 hover:text-slate-900'
                    }
                  >
                    Trang chủ
                  </NavLink>
                  <NavLink
                    to="/catalog"
                    className={({ isActive }) =>
                      isActive
                        ? 'text-blue-900 font-semibold underline underline-offset-8 decoration-2'
                        : 'text-slate-600 hover:text-slate-900'
                    }
                  >
                    Tra cứu sách
                  </NavLink>
                  <NavLink
                    to="/digital-documents"
                    className={({ isActive }) =>
                      isActive
                        ? 'text-blue-900 font-semibold underline underline-offset-8 decoration-2'
                        : 'text-slate-600 hover:text-slate-900'
                    }
                  >
                    Tài liệu số
                  </NavLink>
                  {isAuthenticated && (
                    <NavLink
                      to="/my-borrows"
                      className={({ isActive }) =>
                        isActive
                          ? 'text-blue-900 font-semibold underline underline-offset-8 decoration-2'
                          : 'text-slate-600 hover:text-slate-900'
                      }
                    >
                      Sách của tôi
                    </NavLink>
                  )}
                  {isAuthenticated && (
                    <NavLink
                      to="/reading-history"
                      className={({ isActive }) =>
                        isActive
                          ? 'text-blue-900 font-semibold underline underline-offset-8 decoration-2'
                          : 'text-slate-600 hover:text-slate-900'
                      }
                    >
                      Lịch sử đọc
                    </NavLink>
                  )}
                  {isLibrarianOrAdmin && (
                    <NavLink
                      to="/circulation"
                      className={({ isActive }) =>
                        isActive
                          ? 'text-blue-900 font-semibold underline underline-offset-8 decoration-2'
                          : 'text-slate-600 hover:text-slate-900 flex items-center gap-1.5'
                      }
                    >
                      <span>Quầy lưu thông</span>
                      <span className="rounded-full bg-amber-100 text-amber-800 text-[10px] font-bold px-1.5 py-0.2">
                        Thủ thư
                      </span>
                    </NavLink>
                  )}
                  {isLibrarianOrAdmin && (
                    <NavLink
                      to="/dashboard"
                      className={({ isActive }) =>
                        isActive
                          ? 'text-blue-900 font-semibold underline underline-offset-8 decoration-2'
                          : 'text-slate-600 hover:text-slate-900'
                      }
                    >
                      Báo cáo
                    </NavLink>
                  )}
                  {isLibrarianOrAdmin && (
                    <NavLink
                      to="/organization"
                      className={({ isActive }) =>
                        isActive
                          ? 'text-blue-900 font-semibold underline underline-offset-8 decoration-2'
                          : 'text-slate-600 hover:text-slate-900'
                      }
                    >
                      Tổ chức
                    </NavLink>
                  )}
                  {isAdmin && (
                    <NavLink
                      to="/admin"
                      className={({ isActive }) =>
                        isActive
                          ? 'text-blue-900 font-semibold underline underline-offset-8 decoration-2'
                          : 'text-slate-600 hover:text-slate-900'
                      }
                    >
                      Quản trị
                    </NavLink>
                  )}
                  <NavLink
                    to="/system"
                    className={({ isActive }) =>
                      isActive
                        ? 'text-blue-900 font-semibold underline underline-offset-8 decoration-2'
                        : 'text-slate-600 hover:text-slate-900'
                    }
                  >
                    Hệ thống
                  </NavLink>
                </nav>
              </div>
              <div className="flex items-center gap-3">
                {isLoading ? (
                  <span className="text-xs text-slate-400">Đang tải…</span>
                ) : isAuthenticated && user ? (
                  <>
                    <NotificationBell />
                    <UserMenu user={user} onLogout={logout} />
                  </>
                ) : (
                  <button
                    type="button"
                    onClick={login}
                    className="rounded-lg bg-blue-900 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-800 transition-colors shadow-xs"
                  >
                    Đăng nhập Google
                  </button>
                )}
              </div>
            </div>
          </header>

          <main id="main" tabIndex={-1} className="mx-auto w-full max-w-6xl flex-1 px-6">
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/catalog" element={<CatalogPage />} />
              <Route path="/digital-documents" element={<DigitalDocumentsPage />} />
              <Route path="/my-borrows" element={<MyBorrowsPage />} />
              <Route path="/reading-history" element={<ReadingHistoryPage />} />
              <Route path="/profile" element={<ProfilePage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/circulation" element={<CirculationDeskPage />} />
              <Route path="/organization" element={<OrganizationPage />} />
              <Route path="/admin" element={<AdminPage />} />
              <Route path="/system" element={<SystemStatus />} />
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </main>

          <footer className="border-t border-slate-200 bg-white px-6 py-6 text-xs text-slate-500">
            <div className="mx-auto flex max-w-6xl flex-col sm:flex-row items-center justify-between gap-4">
              <span>Thư viện Đại học Phú Xuân · E-LIB</span>
              <div className="flex gap-4">
                <Link to="/catalog" className="hover:text-blue-900">Tra cứu OPAC</Link>
                <Link to="/digital-documents" className="hover:text-blue-900">Tài liệu số</Link>
                {isAuthenticated && (
                  <Link to="/reading-history" className="hover:text-blue-900">Lịch sử đọc</Link>
                )}
                {isAuthenticated && (
                  <Link to="/notifications" className="hover:text-blue-900">Thông báo</Link>
                )}
                {isLibrarianOrAdmin && (
                  <Link to="/dashboard" className="hover:text-blue-900">Báo cáo</Link>
                )}
                {isLibrarianOrAdmin && (
                  <Link to="/circulation" className="hover:text-blue-900">Quầy thủ thư</Link>
                )}
                {isLibrarianOrAdmin && (
                  <Link to="/organization" className="hover:text-blue-900">Cơ cấu tổ chức</Link>
                )}
                {isAdmin && (
                  <Link to="/admin" className="hover:text-blue-900">Quản trị</Link>
                )}
                <Link to="/system" className="hover:text-blue-900">Trạng thái hệ thống</Link>
              </div>
            </div>
          </footer>
          <MobileBottomNav />
        </div>
      </BrowserRouter>
    </ErrorBoundary>
  );
}
