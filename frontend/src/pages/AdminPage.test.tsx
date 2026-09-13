import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminPage } from './AdminPage';
import * as useAuthModule from '../hooks/useAuth';
import * as adminApi from '../api/admin';
import * as orgApi from '../api/organization';
import type { AdminUser, AuditLogItem, SystemSetting } from '../types/admin';

describe('AdminPage', () => {
  const studentUser: useAuthModule.UserDto = {
    id: 1,
    email: 'student@pxu.edu.vn',
    fullName: 'Nguyễn Văn Học',
    studentCode: 'SV001',
    role: 'STUDENT',
    status: 'ACTIVE',
  };

  const adminUser: useAuthModule.UserDto = {
    id: 99,
    email: 'admin@pxu.edu.vn',
    fullName: 'Admin Hệ Thống',
    studentCode: null,
    role: 'ADMIN',
    status: 'ACTIVE',
  };

  const mockUsers: AdminUser[] = [
    {
      id: 1,
      email: 'student1@pxu.edu.vn',
      fullName: 'Trần Văn Sinh',
      studentCode: 'SV001',
      role: 'STUDENT',
      status: 'ACTIVE',
      departmentId: 10,
      departmentName: 'Khoa CNTT',
      createdAt: '2026-09-01T08:00:00Z',
    },
    {
      id: 2,
      email: 'lecturer1@pxu.edu.vn',
      fullName: 'Lê Thị Giảng',
      studentCode: 'GV001',
      role: 'LECTURER',
      status: 'ACTIVE',
      departmentId: 10,
      departmentName: 'Khoa CNTT',
      createdAt: '2026-09-01T08:00:00Z',
    },
  ];

  const mockAuditLogs: AuditLogItem[] = [
    {
      id: 101,
      userId: 99,
      userEmail: 'admin@pxu.edu.vn',
      userFullName: 'Admin Hệ Thống',
      action: 'USER_UPDATE_ROLE',
      resourceType: 'USER',
      resourceId: '2',
      requestId: 'req-abc-123',
      details: 'Cập nhật vai trò thành LECTURER',
      createdAt: '2026-09-13T10:00:00Z',
    },
  ];

  const mockSettings: SystemSetting[] = [
    {
      id: 1,
      key: 'auth.allowed_domains',
      value: 'pxu.edu.vn',
      description: 'Domain email Google Workspace',
      isSecret: false,
      updatedAt: '2026-09-13T09:00:00Z',
      updatedByEmail: 'admin@pxu.edu.vn',
    },
    {
      id: 2,
      key: 'mail.password',
      value: '********',
      description: 'Mật khẩu SMTP',
      isSecret: true,
      updatedAt: '2026-09-13T09:00:00Z',
      updatedByEmail: 'admin@pxu.edu.vn',
    },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();

    vi.spyOn(orgApi, 'fetchDepartments').mockResolvedValue({
      content: [{ id: 10, name: 'Khoa CNTT', libraryId: 1, libraryName: 'Thư viện Trung tâm' }],
      totalElements: 1,
      totalPages: 1,
      size: 100,
      number: 0,
      first: true,
      last: true,
      empty: false,
    });

    vi.spyOn(adminApi, 'fetchAdminUsers').mockResolvedValue({
      content: mockUsers,
      totalElements: 2,
      totalPages: 1,
      size: 15,
      number: 0,
      first: true,
      last: true,
      empty: false,
    });

    vi.spyOn(adminApi, 'fetchAuditLogs').mockResolvedValue({
      content: mockAuditLogs,
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: false,
    });

    vi.spyOn(adminApi, 'fetchSystemSettings').mockResolvedValue(mockSettings);
  });

  it('từ chối truy cập nếu người dùng không phải Quản trị viên (ADMIN)', () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: studentUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    render(<AdminPage />);
    expect(screen.getByText('Truy Cập Bị Từ Chối')).toBeInTheDocument();
  });

  it('tải và hiển thị danh sách người dùng cho Quản trị viên', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: adminUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    render(<AdminPage />);

    expect(screen.getByText('Quản Trị Hệ Thống')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Trần Văn Sinh')).toBeInTheDocument();
      expect(screen.getByText('student1@pxu.edu.vn')).toBeInTheDocument();
      expect(screen.getByText('Lê Thị Giảng')).toBeInTheDocument();
      expect(screen.getAllByText('Khoa CNTT').length).toBeGreaterThan(0);
    });
  });

  it('mở modal chỉnh sửa người dùng và cập nhật thành công', async () => {
    const user = userEvent.setup();
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: adminUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    const updateSpy = vi.spyOn(adminApi, 'updateAdminUser').mockResolvedValue({
      ...mockUsers[0]!,
      role: 'LECTURER',
    });

    render(<AdminPage />);

    await waitFor(() => {
      expect(screen.getByText('Trần Văn Sinh')).toBeInTheDocument();
    });

    // Click Sửa on first row
    const editButtons = screen.getAllByText('Sửa');
    await user.click(editButtons[0]!);

    expect(screen.getByText('Chỉnh Sửa Tài Khoản')).toBeInTheDocument();

    // Select new role LECTURER
    const roleSelect = screen.getByLabelText('Vai trò (Role)');
    await user.selectOptions(roleSelect, 'LECTURER');

    // Click Lưu Thay Đổi
    await user.click(screen.getByText('Lưu Thay Đổi'));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith(1, {
        role: 'LECTURER',
        status: 'ACTIVE',
        departmentId: 10,
      });
    });
  });

  it('mở modal nhập Excel và hiển thị kết quả chi tiết', async () => {
    const user = userEvent.setup();
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: adminUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    const importSpy = vi.spyOn(adminApi, 'importUsersExcel').mockResolvedValue({
      totalRows: 3,
      importedCount: 2,
      failedCount: 1,
      errors: [{ rowNumber: 3, email: 'bad@gmail.com', message: 'Domain không hợp lệ' }],
    });

    render(<AdminPage />);

    // Click Nhập Excel
    await user.click(screen.getByText(/Nhập Excel hàng loạt/));
    expect(screen.getByText('Nhập Người Dùng Từ Excel')).toBeInTheDocument();

    // Upload file
    const file = new File(['dummy excel content'], 'users.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const fileInput = screen.getByLabelText(/Chọn tệp tin Excel/);
    await user.upload(fileInput, file);

    // Submit
    await user.click(screen.getByText('Tải Lên & Nhập Liệu'));

    await waitFor(() => {
      expect(importSpy).toHaveBeenCalled();
      expect(screen.getByText('Tổng số dòng')).toBeInTheDocument();
      expect(screen.getByText('Domain không hợp lệ')).toBeInTheDocument();
    });
  });

  it('chuyển sang tab Nhật ký kiểm toán và hiển thị dữ liệu cùng correlation requestId', async () => {
    const user = userEvent.setup();
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: adminUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    render(<AdminPage />);

    // Click Tab Nhật ký kiểm toán
    await user.click(screen.getByText(/Nhật Ký Kiểm Toán/));

    await waitFor(() => {
      expect(screen.getByText('USER_UPDATE_ROLE')).toBeInTheDocument();
      expect(screen.getByText('req-abc-123')).toBeInTheDocument();
      expect(screen.getByText('Cập nhật vai trò thành LECTURER')).toBeInTheDocument();
    });
  });

  it('chuyển sang tab Cấu hình hệ thống, che giấu mật mã và cho phép cập nhật', async () => {
    const user = userEvent.setup();
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      status: 'authenticated',
      user: adminUser,
      isLoading: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    const updateSettingSpy = vi.spyOn(adminApi, 'updateSystemSetting').mockResolvedValue({
      ...mockSettings[0]!,
      value: 'pxu.edu.vn,daihocphuxuan.edu.vn',
    });

    render(<AdminPage />);

    // Click Tab Cấu hình hệ thống
    await user.click(screen.getByText(/Cấu Hình Hệ Thống/));

    await waitFor(() => {
      expect(screen.getByText('auth.allowed_domains')).toBeInTheDocument();
      expect(screen.getByText('mail.password')).toBeInTheDocument();
      expect(screen.getByText('🔒 Bảo mật')).toBeInTheDocument();
      expect(screen.getByText('********')).toBeInTheDocument();
    });

    // Click Thay đổi for the first setting
    const changeButtons = screen.getAllByText('Thay đổi');
    await user.click(changeButtons[0]!);

    expect(screen.getByText('Thay Đổi Cấu Hình')).toBeInTheDocument();

    const input = screen.getByLabelText(/Giá trị mới/);
    await user.clear(input);
    await user.type(input, 'pxu.edu.vn,daihocphuxuan.edu.vn');

    await user.click(screen.getByText('Cập Nhật'));

    await waitFor(() => {
      expect(updateSettingSpy).toHaveBeenCalledWith(
        'auth.allowed_domains',
        'pxu.edu.vn,daihocphuxuan.edu.vn'
      );
    });
  });
});
