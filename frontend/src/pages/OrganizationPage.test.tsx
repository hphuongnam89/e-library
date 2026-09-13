import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OrganizationPage } from './OrganizationPage';
import * as useAuthModule from '../hooks/useAuth';
import * as orgApi from '../api/organization';

describe('OrganizationPage', () => {
  const studentUser: useAuthModule.UserDto = {
    id: 1,
    email: 'sinhvien@phuxuan.edu.vn',
    fullName: 'Sinh Viên A',
    studentCode: 'PXU1001',
    role: 'STUDENT',
    status: 'ACTIVE',
  };

  const adminUser: useAuthModule.UserDto = {
    id: 9,
    email: 'admin@phuxuan.edu.vn',
    fullName: 'Quản Trị Viên',
    studentCode: null,
    role: 'ADMIN',
    status: 'ACTIVE',
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('blocks non-admin/non-librarian with 403 Forbidden screen', () => {
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
        <OrganizationPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Truy cập bị từ chối')).toBeInTheDocument();
  });

  it('loads and displays organization hierarchy tabs for admin', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: adminUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(orgApi, 'fetchInstitutions').mockResolvedValue({
      content: [{ id: 1, name: 'Trường Đại học Phú Xuân', createdAt: '2026-09-01T00:00:00Z' }],
      totalElements: 1,
      totalPages: 1,
      size: 50,
      number: 0,
      first: true,
      last: true,
      empty: false,
    });

    vi.spyOn(orgApi, 'fetchCampuses').mockResolvedValue({
      content: [{ id: 1, institutionId: 1, institutionName: 'Trường Đại học Phú Xuân', name: 'Cơ sở 176 Trần Phú' }],
      totalElements: 1,
      totalPages: 1,
      size: 50,
      number: 0,
      first: true,
      last: true,
      empty: false,
    });

    vi.spyOn(orgApi, 'fetchLibraries').mockResolvedValue({
      content: [{ id: 1, campusId: 1, campusName: 'Cơ sở 176 Trần Phú', name: 'Thư viện Trung tâm', address: '176 Trần Phú' }],
      totalElements: 1,
      totalPages: 1,
      size: 50,
      number: 0,
      first: true,
      last: true,
      empty: false,
    });

    vi.spyOn(orgApi, 'fetchDepartments').mockResolvedValue({
      content: [{ id: 1, libraryId: 1, libraryName: 'Thư viện Trung tâm', name: 'Khoa Công nghệ Thông tin' }],
      totalElements: 1,
      totalPages: 1,
      size: 50,
      number: 0,
      first: true,
      last: true,
      empty: false,
    });

    render(
      <MemoryRouter>
        <OrganizationPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Cơ Cấu Tổ Chức & Phòng Ban')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Trường Đại học Phú Xuân')).toBeInTheDocument();
      expect(screen.getByText('1. Cơ quan / Trường (1)')).toBeInTheDocument();
    });

    // Switch tab to Campuses
    const campusTab = screen.getByRole('button', { name: /2. Cơ sở/ });
    await userEvent.click(campusTab);

    await waitFor(() => {
      expect(screen.getByText('Cơ sở 176 Trần Phú')).toBeInTheDocument();
    });
  });
});
