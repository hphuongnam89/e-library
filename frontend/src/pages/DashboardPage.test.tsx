import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DashboardPage } from './DashboardPage';
import * as useAuthModule from '../hooks/useAuth';
import * as reportApi from '../api/report';
import type { BookReportItemDto, DashboardSummaryDto } from '../types/report';
import type { Page } from '../types/catalog';

describe('DashboardPage', () => {
  const mockStudentUser: useAuthModule.UserDto = {
    id: 1,
    email: 'sinhvien@pxu.edu.vn',
    fullName: 'Nguyễn Văn A',
    studentCode: 'PXU001',
    role: 'STUDENT',
    status: 'ACTIVE',
  };

  const mockLibrarianUser: useAuthModule.UserDto = {
    id: 2,
    email: 'thuthu@pxu.edu.vn',
    fullName: 'Thủ Thư B',
    studentCode: null,
    role: 'LIBRARIAN',
    status: 'ACTIVE',
  };

  const mockSummary: DashboardSummaryDto = {
    books: {
      totalTitles: 120,
      totalCopies: 450,
      availableCopies: 320,
      borrowedCopies: 110,
      maintenanceOrDamagedCopies: 20,
    },
    digital: {
      totalDocuments: 65,
      totalReadingSessions: 380,
      totalReadingHours: 42.5,
    },
    circulation: {
      activeBorrows: 110,
      overdueBorrows: 14,
      returnedBorrows: 890,
      totalUnpaidFines: 70000,
    },
    users: {
      totalUsers: 1500,
      activeUsers: 1420,
      studentUsers: 1350,
      lecturerUsers: 70,
    },
    topBorrowedBooks: [
      {
        bookTitleId: 1,
        title: 'Lập trình Clean Code',
        author: 'Robert C. Martin',
        borrowCount: 38,
      },
    ],
    topReadDocuments: [
      {
        documentId: 10,
        title: 'Giáo trình Giải thuật',
        author: 'Phú Xuân',
        activeSeconds: 7200,
        sessionCount: 15,
      },
    ],
    recentBorrows: [
      {
        borrowId: 101,
        studentCode: 'PXU001',
        userName: 'Nguyễn Văn A',
        bookTitle: 'Clean Code',
        barcode: 'BC0001',
        borrowedAt: '2026-09-10T08:00:00Z',
        dueAt: '2026-09-24T08:00:00Z',
        status: 'BORROWED',
      },
    ],
  };

  const mockBooksPage: Page<BookReportItemDto> = {
    content: [
      {
        copyId: 1,
        titleId: 10,
        barcode: 'BC0001',
        title: 'Clean Code',
        author: 'Robert C. Martin',
        isbn: '9780132350884',
        categoryName: 'Công nghệ thông tin',
        status: 'AVAILABLE',
        libraryName: 'Thư viện Cơ sở 1',
        createdAt: '2026-09-01T00:00:00Z',
      },
    ],
    totalElements: 1,
    totalPages: 1,
    size: 15,
    number: 0,
    first: true,
    last: true,
    empty: false,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('từ chối truy cập khi người dùng không phải Thủ thư hoặc Admin', () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      user: mockStudentUser,
      status: 'authenticated',
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    render(<DashboardPage />);

    expect(screen.getByText(/Quyền truy cập bị hạn chế/i)).toBeInTheDocument();
    expect(screen.getByText(/chỉ dành cho Thủ thư và Quản trị viên/i)).toBeInTheDocument();
  });

  it('hiển thị đầy đủ thông số KPI tổng quan khi là Thủ thư', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      user: mockLibrarianUser,
      status: 'authenticated',
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(reportApi, 'fetchDashboardSummary').mockResolvedValue(mockSummary);

    render(<DashboardPage />);

    expect(screen.getByText('Báo cáo & Thống kê')).toBeInTheDocument();

    await waitFor(() => {
      // Books KPI
      expect(screen.getByText('450')).toBeInTheDocument();
      // Circulation KPI
      expect(screen.getAllByText('110').length).toBeGreaterThanOrEqual(1);
      // Digital reading hours KPI
      expect(screen.getByText('42.5')).toBeInTheDocument();
      // Active users KPI
      expect(screen.getByText('1420')).toBeInTheDocument();
      // Top borrowed book
      expect(screen.getByText('Lập trình Clean Code')).toBeInTheDocument();
      expect(screen.getByText(/38\s*lượt/i)).toBeInTheDocument();
      // Top read doc
      expect(screen.getByText('Giáo trình Giải thuật')).toBeInTheDocument();
      // Recent borrow
      expect(screen.getByText('BC0001')).toBeInTheDocument();
    });
  });

  it('chuyển đổi sang tab Báo cáo chi tiết và hiển thị bảng dữ liệu', async () => {
    const user = userEvent.setup();
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      user: mockLibrarianUser,
      status: 'authenticated',
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(reportApi, 'fetchDashboardSummary').mockResolvedValue(mockSummary);
    vi.spyOn(reportApi, 'fetchReportData').mockResolvedValue(mockBooksPage);

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText('450')).toBeInTheDocument();
    });

    // Click on Reports tab
    const reportsTabBtn = screen.getByRole('button', { name: /Báo cáo chi tiết & Xuất Excel/i });
    await user.click(reportsTabBtn);

    await waitFor(() => {
      expect(screen.getByText('Clean Code')).toBeInTheDocument();
      expect(screen.getByText('Robert C. Martin')).toBeInTheDocument();
      expect(screen.getByText('Công nghệ thông tin')).toBeInTheDocument();
    });
  });

  it('gọi hàm downloadReportExcel khi bấm nút Xuất file Excel', async () => {
    const user = userEvent.setup();
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      user: mockLibrarianUser,
      status: 'authenticated',
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(reportApi, 'fetchDashboardSummary').mockResolvedValue(mockSummary);
    vi.spyOn(reportApi, 'fetchReportData').mockResolvedValue(mockBooksPage);
    const exportSpy = vi.spyOn(reportApi, 'downloadReportExcel').mockResolvedValue();

    render(<DashboardPage />);

    // Switch to reports tab
    const reportsTabBtn = screen.getByRole('button', { name: /Báo cáo chi tiết & Xuất Excel/i });
    await user.click(reportsTabBtn);

    await waitFor(() => {
      expect(screen.getByText('Clean Code')).toBeInTheDocument();
    });

    const exportBtn = screen.getByRole('button', { name: /Xuất file Excel/i });
    await user.click(exportBtn);

    expect(exportSpy).toHaveBeenCalledWith('books', expect.any(Object));
  });
});
