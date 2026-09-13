import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CirculationDeskPage } from './CirculationDeskPage';
import * as useAuthModule from '../hooks/useAuth';
import * as circulationApi from '../api/circulation';
import * as catalogApi from '../api/catalog';

describe('CirculationDeskPage', () => {
  const studentUser: useAuthModule.UserDto = {
    id: 1,
    email: 'sinhvien@phuxuan.edu.vn',
    fullName: 'Sinh Viên A',
    studentCode: 'PXU1001',
    role: 'STUDENT',
    status: 'ACTIVE',
  };

  const librarianUser: useAuthModule.UserDto = {
    id: 2,
    email: 'thuthu@phuxuan.edu.vn',
    fullName: 'Thủ Thư B',
    studentCode: null,
    role: 'LIBRARIAN',
    status: 'ACTIVE',
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('blocks non-librarian/non-admin users with 403 Forbidden screen', () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: studentUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    render(
      <MemoryRouter>
        <CirculationDeskPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Truy cập bị từ chối')).toBeInTheDocument();
    expect(screen.getByText(/Quầy lưu thông thư viện yêu cầu quyền/)).toBeInTheDocument();
  });

  it('allows access for librarian and renders checkout desk by default', () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: librarianUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    render(
      <MemoryRouter>
        <CirculationDeskPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Quầy Lưu Thông Thư Viện')).toBeInTheDocument();
    expect(screen.getByText('Quầy Check-out Mượn Sách Theo Lô')).toBeInTheDocument();
    expect(screen.getByLabelText(/1. Mã số sinh viên/)).toBeInTheDocument();
  });

  it('performs barcode lookup and confirms batch checkout in CheckoutPanel', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: librarianUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(catalogApi, 'fetchBookCopyByBarcode').mockResolvedValue({
      id: 101,
      bookTitleId: 10,
      bookTitle: 'Trí Tuệ Nhân Tạo Cơ Bản',
      libraryId: 1,
      libraryName: 'Thư viện Trung tâm',
      barcode: 'PXU-BC-100001',
      location: 'Kệ A1',
      status: 'AVAILABLE',
      version: 0,
      createdAt: '2026-09-01T00:00:00Z',
      updatedAt: '2026-09-01T00:00:00Z',
    });

    const checkoutSpy = vi.spyOn(circulationApi, 'checkoutBooks').mockResolvedValue({
      items: [
        {
          id: 1,
          userId: 1,
          userEmail: 'sinhvien@phuxuan.edu.vn',
          studentCode: 'PXU1001',
          userFullName: 'Sinh Viên A',
          bookCopyId: 101,
          barcode: 'PXU-BC-100001',
          bookTitleId: 10,
          bookTitle: 'Trí Tuệ Nhân Tạo Cơ Bản',
          libraryId: 1,
          libraryName: 'Thư viện Trung tâm',
          borrowedAt: '2026-09-13T00:00:00Z',
          dueAt: '2026-09-27T00:00:00Z',
          returnedAt: null,
          dailyFine: 5000,
          fineAmount: 0,
          finePaidAt: null,
          status: 'BORROWED',
          overdue: false,
        },
      ],
    });

    render(
      <MemoryRouter>
        <CirculationDeskPage />
      </MemoryRouter>
    );

    const studentInput = screen.getByLabelText(/1. Mã số sinh viên/);
    const barcodeInput = screen.getByPlaceholderText(/Quét máy barcode/);
    const addBtn = screen.getByRole('button', { name: 'Thêm' });

    await userEvent.type(studentInput, 'PXU1001');
    await userEvent.type(barcodeInput, 'PXU-BC-100001');
    await userEvent.click(addBtn);

    await waitFor(() => {
      expect(screen.getByText('Trí Tuệ Nhân Tạo Cơ Bản')).toBeInTheDocument();
    });

    const confirmBtn = screen.getByRole('button', { name: /Xác nhận mượn sách theo lô/ });
    await userEvent.click(confirmBtn);

    await waitFor(() => {
      expect(checkoutSpy).toHaveBeenCalledWith({
        studentCode: 'PXU1001',
        barcodes: ['PXU-BC-100001'],
      });
      expect(screen.getByText('Mượn sách theo lô thành công!')).toBeInTheDocument();
    });
  });

  it('switches to return panel and displays return borrow details', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: librarianUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    render(
      <MemoryRouter>
        <CirculationDeskPage />
      </MemoryRouter>
    );

    const returnTabBtn = screen.getByRole('button', { name: /Tiếp nhận trả sách & Thu phạt/ });
    await userEvent.click(returnTabBtn);

    expect(screen.getByText('Quầy Tiếp Nhận Trả Sách & Thu Phạt')).toBeInTheDocument();
  });
});
