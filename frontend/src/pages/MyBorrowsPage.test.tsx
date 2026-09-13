import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MyBorrowsPage } from './MyBorrowsPage';
import * as useAuthModule from '../hooks/useAuth';
import * as circulationApi from '../api/circulation';

describe('MyBorrowsPage', () => {
  const mockUser: useAuthModule.UserDto = {
    id: 1,
    email: 'sinhvien@phuxuan.edu.vn',
    fullName: 'Lê Văn B',
    studentCode: 'PXU2201',
    role: 'STUDENT',
    status: 'ACTIVE',
  };

  const mockBorrowsPage = {
    content: [
      {
        id: 1,
        userId: 1,
        userEmail: 'sinhvien@phuxuan.edu.vn',
        studentCode: 'PXU2201',
        userFullName: 'Lê Văn B',
        bookCopyId: 101,
        barcode: 'PXU-BC-100001',
        bookTitleId: 10,
        bookTitle: 'Nhập môn Trí tuệ Nhân tạo',
        libraryId: 1,
        libraryName: 'Thư viện Cơ sở 1',
        borrowedAt: '2026-09-01T10:00:00Z',
        dueAt: '2026-09-15T10:00:00Z',
        returnedAt: null,
        dailyFine: 5000,
        fineAmount: 0,
        finePaidAt: null,
        status: 'BORROWED' as const,
        overdue: false,
      },
      {
        id: 2,
        userId: 1,
        userEmail: 'sinhvien@phuxuan.edu.vn',
        studentCode: 'PXU2201',
        userFullName: 'Lê Văn B',
        bookCopyId: 102,
        barcode: 'PXU-BC-100002',
        bookTitleId: 11,
        bookTitle: 'Kỹ thuật Lập trình Web',
        libraryId: 1,
        libraryName: 'Thư viện Cơ sở 1',
        borrowedAt: '2026-08-01T10:00:00Z',
        dueAt: '2026-08-15T10:00:00Z',
        returnedAt: null,
        dailyFine: 5000,
        fineAmount: 25000,
        finePaidAt: null,
        status: 'BORROWED' as const,
        overdue: true,
      },
    ],
    totalElements: 2,
    totalPages: 1,
    size: 10,
    number: 0,
    first: true,
    last: true,
    empty: false,
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders login prompt when unauthenticated', () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'unauthenticated',
      user: null,
      isLoading: false,
      isAuthenticated: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    render(
      <MemoryRouter>
        <MyBorrowsPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Lịch sử mượn sách cá nhân')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Đăng nhập Google' })).toBeInTheDocument();
  });

  it('loads and displays active borrows and overdue fine warnings when authenticated', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: mockUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(circulationApi, 'fetchMyBorrows').mockResolvedValue(mockBorrowsPage);

    render(
      <MemoryRouter>
        <MyBorrowsPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Sách & Khoản Mượn Của Tôi')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Nhập môn Trí tuệ Nhân tạo')).toBeInTheDocument();
      expect(screen.getByText('Kỹ thuật Lập trình Web')).toBeInTheDocument();
      expect(screen.getByText('Mã bản sách: PXU-BC-100001')).toBeInTheDocument();
      expect(screen.getByText('25.000 đ')).toBeInTheDocument();
      expect(screen.getByText('Quá hạn', { selector: 'button *' })).toBeInTheDocument();
    });
  });

  it('switches tabs to filter borrows', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: mockUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(circulationApi, 'fetchMyBorrows').mockResolvedValue(mockBorrowsPage);

    render(
      <MemoryRouter>
        <MyBorrowsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Nhập môn Trí tuệ Nhân tạo')).toBeInTheDocument();
    });

    const returnedTab = screen.getByRole('button', { name: 'Đã trả' });
    await userEvent.click(returnedTab);

    expect(screen.getByText('Chưa có dữ liệu mượn sách.')).toBeInTheDocument();
  });
});
