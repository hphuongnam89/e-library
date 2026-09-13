import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CatalogPage } from './CatalogPage';
import * as useAuthModule from '../hooks/useAuth';
import * as catalogApi from '../api/catalog';

describe('CatalogPage', () => {
  const mockUser: useAuthModule.UserDto = {
    id: 1,
    email: 'sinhvien@phuxuan.edu.vn',
    fullName: 'Lê Văn B',
    studentCode: 'PXU2201',
    role: 'STUDENT',
    status: 'ACTIVE',
  };

  const mockCategories = [
    { id: 1, parentId: null, parentName: null, name: 'Công nghệ thông tin' },
    { id: 2, parentId: null, parentName: null, name: 'Kinh tế & Quản trị' },
  ];

  const mockBooksPage = {
    content: [
      {
        id: 1,
        title: 'Cấu Trúc Dữ Liệu & Giải Thuật',
        author: 'Trần Văn C',
        publisher: 'NXB Khoa Học',
        isbn: '978-0-1234-5678-9',
        publicationYear: 2023,
        categoryId: 1,
        categoryName: 'Công nghệ thông tin',
        createdAt: '2026-09-01T00:00:00Z',
        updatedAt: '2026-09-01T00:00:00Z',
      },
    ],
    totalElements: 1,
    totalPages: 1,
    size: 12,
    number: 0,
    first: true,
    last: true,
    empty: false,
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders login prompt when user is unauthenticated', () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'unauthenticated',
      user: null,
      isLoading: false,
      isAuthenticated: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    render(<CatalogPage />);

    expect(screen.getByText('Tra cứu danh mục sách E-LIB')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Đăng nhập Google để tiếp tục' })).toBeInTheDocument();
  });

  it('loads and displays categories and books when authenticated', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: mockUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(catalogApi, 'fetchRootCategories').mockResolvedValue(mockCategories);
    vi.spyOn(catalogApi, 'fetchBookTitles').mockResolvedValue(mockBooksPage);

    render(<CatalogPage />);

    expect(screen.getByText('Tra cứu & Quản lý Danh mục Sách')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Cấu Trúc Dữ Liệu & Giải Thuật')).toBeInTheDocument();
      expect(screen.getByText('Trần Văn C')).toBeInTheDocument();
      expect(screen.getByText('Công nghệ thông tin', { selector: 'button' })).toBeInTheDocument();
      expect(screen.getByText('Kinh tế & Quản trị', { selector: 'button' })).toBeInTheDocument();
    });
  });

  it('submits search query and triggers fetchBookTitles with query', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: mockUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(catalogApi, 'fetchRootCategories').mockResolvedValue(mockCategories);
    const fetchSpy = vi.spyOn(catalogApi, 'fetchBookTitles').mockResolvedValue(mockBooksPage);

    render(<CatalogPage />);

    const searchInput = screen.getByPlaceholderText('Nhập tên sách, tác giả, nhà xuất bản...');
    const searchButton = screen.getByRole('button', { name: 'Tìm kiếm' });

    await userEvent.type(searchInput, 'Python');
    await userEvent.click(searchButton);

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          query: 'Python',
        })
      );
    });
  });
});
