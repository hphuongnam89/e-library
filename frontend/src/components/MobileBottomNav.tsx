import { NavLink } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function MobileBottomNav() {
  const { isAuthenticated, login } = useAuth();

  return (
    <nav className="sm:hidden fixed bottom-0 left-0 right-0 z-50 flex items-center justify-around border-t border-slate-200 bg-white px-2 py-2 pb-safe shadow-lg">
      <NavLink
        to="/"
        className={({ isActive }) =>
          `flex flex-col items-center gap-1 p-2 text-[10px] font-medium transition-colors ${
            isActive ? 'text-blue-700' : 'text-slate-500 hover:text-slate-900'
          }`
        }
      >
        <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>
        <span>Trang chủ</span>
      </NavLink>

      <NavLink
        to="/catalog"
        className={({ isActive }) =>
          `flex flex-col items-center gap-1 p-2 text-[10px] font-medium transition-colors ${
            isActive ? 'text-blue-700' : 'text-slate-500 hover:text-slate-900'
          }`
        }
      >
        <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <span>Tra cứu</span>
      </NavLink>

      <NavLink
        to="/digital-documents"
        className={({ isActive }) =>
          `flex flex-col items-center gap-1 p-2 text-[10px] font-medium transition-colors ${
            isActive ? 'text-blue-700' : 'text-slate-500 hover:text-slate-900'
          }`
        }
      >
        <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
        </svg>
        <span>Tài liệu số</span>
      </NavLink>

      {isAuthenticated ? (
        <>
          <NavLink
            to="/my-borrows"
            className={({ isActive }) =>
              `flex flex-col items-center gap-1 p-2 text-[10px] font-medium transition-colors ${
                isActive ? 'text-blue-700' : 'text-slate-500 hover:text-slate-900'
              }`
            }
          >
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>Đã mượn</span>
          </NavLink>
          
          <NavLink
            to="/profile"
            className={({ isActive }) =>
              `flex flex-col items-center gap-1 p-2 text-[10px] font-medium transition-colors ${
                isActive ? 'text-blue-700' : 'text-slate-500 hover:text-slate-900'
              }`
            }
          >
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            <span>Tài khoản</span>
          </NavLink>
        </>
      ) : (
        <button
          onClick={login}
          className="flex flex-col items-center gap-1 p-2 text-[10px] font-medium text-slate-500 hover:text-slate-900 transition-colors"
        >
          <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
          </svg>
          <span>Đăng nhập</span>
        </button>
      )}
    </nav>
  );
}
