import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { UserMenu } from './UserMenu';
import { type UserDto } from '../hooks/useAuth';

describe('UserMenu', () => {
  const mockUser: UserDto = {
    id: 1,
    email: 'sinhvien@phuxuan.edu.vn',
    fullName: 'Nguyễn Văn A',
    studentCode: 'PXU12345',
    role: 'STUDENT',
    status: 'ACTIVE',
  };

  it('renders user full name and translated role badge', () => {
    render(<UserMenu user={mockUser} onLogout={vi.fn()} />);

    expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
    expect(screen.getByText('Sinh viên')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Đăng xuất' })).toBeInTheDocument();
  });

  it('calls onLogout when clicking logout button', async () => {
    const handleLogout = vi.fn();
    render(<UserMenu user={mockUser} onLogout={handleLogout} />);

    await userEvent.click(screen.getByRole('button', { name: 'Đăng xuất' }));
    expect(handleLogout).toHaveBeenCalledTimes(1);
  });

  it('correctly maps all roles to their Vietnamese labels', () => {
    const roles: Array<[UserDto['role'], string]> = [
      ['STUDENT', 'Sinh viên'],
      ['LECTURER', 'Giảng viên'],
      ['LIBRARIAN', 'Thủ thư'],
      ['ADMIN', 'Quản trị viên'],
    ];

    for (const [role, label] of roles) {
      const user: UserDto = { ...mockUser, role };
      const { unmount } = render(<UserMenu user={user} onLogout={vi.fn()} />);
      expect(screen.getByText(label)).toBeInTheDocument();
      unmount();
    }
  });
});
