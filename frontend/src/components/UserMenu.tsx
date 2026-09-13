import { type UserDto, type UserRole } from '../hooks/useAuth';

const ROLE_LABELS: Record<UserRole, string> = {
  STUDENT: 'Sinh viên',
  LECTURER: 'Giảng viên',
  LIBRARIAN: 'Thủ thư',
  ADMIN: 'Quản trị viên',
};

interface UserMenuProps {
  user: UserDto;
  onLogout: () => void;
}

export function UserMenu({ user, onLogout }: UserMenuProps) {
  return (
    <div className="flex items-center gap-3">
      <div className="text-right">
        <p className="text-sm font-semibold text-slate-800 leading-none">{user.fullName}</p>
        <span className="inline-block mt-1 text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-800 font-medium">
          {ROLE_LABELS[user.role] ?? user.role}
        </span>
      </div>
      <button
        type="button"
        onClick={onLogout}
        className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition-colors"
      >
        Đăng xuất
      </button>
    </div>
  );
}
