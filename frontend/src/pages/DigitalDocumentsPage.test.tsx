import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DigitalDocumentsPage } from './DigitalDocumentsPage';
import * as useAuthModule from '../hooks/useAuth';
import * as digitalApi from '../api/digital';
import * as catalogApi from '../api/catalog';
import type { DigitalDocumentDto } from '../types/digital';

describe('DigitalDocumentsPage', () => {
  const mockStudentUser: useAuthModule.UserDto = {
    id: 1,
    email: 'sinhvien@pxu.edu.vn',
    fullName: 'Sinh Viên A',
    studentCode: 'PXU1001',
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

  const mockDocuments: DigitalDocumentDto[] = [
    {
      id: 1,
      libraryId: 1,
      libraryName: 'Thư viện Trung tâm',
      title: 'Giáo trình Cơ sở dữ liệu nâng cao',
      description: 'Chuyên đề cơ sở dữ liệu quan hệ và NoSQL',
      publisher: 'NXB Khoa Học',
      categoryId: 1,
      categoryName: 'Công nghệ thông tin',
      contentType: 'application/pdf',
      sizeBytes: 1572864, // 1.5MB
      status: 'PUBLISHED',
      permission: 'AUTHENTICATED',
      isActive: true,
      createdAt: '2026-09-01T00:00:00Z',
      updatedAt: '2026-09-01T00:00:00Z',
    },
    {
      id: 2,
      libraryId: 1,
      libraryName: 'Thư viện Trung tâm',
      title: 'Kinh tế vi mô & Vĩ mô',
      description: 'Lý thuyết kinh tế đại cương',
      publisher: 'NXB Kinh Tế',
      categoryId: 2,
      categoryName: 'Kinh tế & Quản trị',
      contentType: 'application/pdf',
      sizeBytes: 3145728, // 3MB
      status: 'DRAFT',
      permission: 'RESTRICTED',
      isActive: true,
      createdAt: '2026-09-02T00:00:00Z',
      updatedAt: '2026-09-02T00:00:00Z',
    },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(catalogApi, 'fetchCategories').mockResolvedValue({
      content: [
        { id: 1, name: 'Công nghệ thông tin', parentId: null, parentName: null },
        { id: 2, name: 'Kinh tế & Quản trị', parentId: null, parentName: null },
      ],
      number: 0,
      size: 100,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
      empty: false,
    });
  });

  it('renders page header and published documents for student', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: mockStudentUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(digitalApi, 'fetchDigitalDocuments').mockResolvedValue({
      content: [mockDocuments[0]!],
      number: 0,
      size: 12,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      empty: false,
    });

    render(
      <MemoryRouter>
        <DigitalDocumentsPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Kho Tài Liệu Số (Digital Library)')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Giáo trình Cơ sở dữ liệu nâng cao')).toBeInTheDocument();
      expect(screen.getByText('NXB Khoa Học • Công nghệ thông tin')).toBeInTheDocument();
      expect(screen.getByText('Toàn trường')).toBeInTheDocument();
    });

    // Student should not see the upload button
    expect(screen.queryByRole('button', { name: /Tải lên tài liệu PDF/i })).not.toBeInTheDocument();
  });

  it('displays staff actions and controls for librarian', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: mockLibrarianUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(digitalApi, 'fetchDigitalDocuments').mockResolvedValue({
      content: mockDocuments,
      number: 0,
      size: 12,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
      empty: false,
    });

    render(
      <MemoryRouter>
        <DigitalDocumentsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Tải lên tài liệu PDF/i })).toBeInTheDocument();
      expect(screen.getByText('DRAFT')).toBeInTheDocument();
      expect(screen.getByText('Gửi duyệt')).toBeInTheDocument();
    });
  });

  it('opens reader modal when clicking Read Document', async () => {
    const user = userEvent.setup();

    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: mockStudentUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(digitalApi, 'fetchDigitalDocuments').mockResolvedValue({
      content: [mockDocuments[0]!],
      number: 0,
      size: 12,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      empty: false,
    });

    render(
      <MemoryRouter>
        <DigitalDocumentsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Giáo trình Cơ sở dữ liệu nâng cao')).toBeInTheDocument();
    });

    const readBtn = screen.getByRole('button', { name: /Đọc tài liệu/i });
    await user.click(readBtn);

    expect(screen.getByRole('dialog', { name: /Giáo trình Cơ sở dữ liệu nâng cao/i })).toBeInTheDocument();
    expect(screen.getByTestId('reader-watermark')).toBeInTheDocument();
  });
});
