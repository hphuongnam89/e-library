import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationsPage } from './NotificationsPage';
import * as useAuthModule from '../hooks/useAuth';
import * as notificationApi from '../api/notification';
import type { NotificationDto } from '../types/notification';
import type { Page } from '../types/catalog';

describe('NotificationsPage', () => {
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

  const mockNotifications: NotificationDto[] = [
    {
      id: 1,
      borrowId: 10,
      notificationType: 'DUE_REMINDER',
      channel: 'EMAIL',
      title: 'Nhắc hẹn trả sách: Cấu trúc Dữ liệu',
      message: '<p>Sách sắp đến hạn trả vào ngày 15/09/2026.</p>',
      deduplicationKey: 'DUE_REMINDER:10:2026-09-15',
      status: 'SENT',
      sentAt: '2026-09-13T07:00:00Z',
      readAt: null,
      createdAt: '2026-09-13T07:00:00Z',
    },
    {
      id: 2,
      borrowId: 20,
      notificationType: 'OVERDUE',
      channel: 'EMAIL',
      title: 'Cảnh báo quá hạn: Lập trình Java',
      message: '<p>Sách đã quá hạn 3 ngày. Phí phạt: 15.000 VNĐ.</p>',
      deduplicationKey: 'OVERDUE:20:2026-09-13',
      status: 'SENT',
      sentAt: '2026-09-13T07:00:00Z',
      readAt: '2026-09-13T08:00:00Z',
      createdAt: '2026-09-13T07:00:00Z',
    },
  ];

  const mockPage: Page<NotificationDto> = {
    content: mockNotifications,
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
      user: null,
      status: 'unauthenticated',
      isAuthenticated: false,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    render(<NotificationsPage />);
    expect(screen.getByText('Yêu cầu đăng nhập')).toBeInTheDocument();
  });

  it('renders notification list with type badges and content', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      user: mockStudentUser,
      status: 'authenticated',
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(notificationApi, 'fetchMyNotifications').mockResolvedValueOnce(mockPage);

    render(<NotificationsPage />);

    await waitFor(() => {
      expect(screen.getByText('Hộp thư Thông báo')).toBeInTheDocument();
      expect(screen.getByText('Nhắc hẹn trả sách: Cấu trúc Dữ liệu')).toBeInTheDocument();
      expect(screen.getByText('Cảnh báo quá hạn: Lập trình Java')).toBeInTheDocument();
      expect(screen.getByText('⏰ Nhắc hẹn trả')).toBeInTheDocument();
      expect(screen.getByText('🚨 Quá hạn mượn')).toBeInTheDocument();
    });
  });

  it('renders empty state when no notifications', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      user: mockStudentUser,
      status: 'authenticated',
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(notificationApi, 'fetchMyNotifications').mockResolvedValueOnce({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 0,
      first: true,
      last: true,
      empty: true,
    });

    render(<NotificationsPage />);

    await waitFor(() => {
      expect(screen.getByText('Không có thông báo')).toBeInTheDocument();
    });
  });

  it('librarian sees trigger reminders button and can execute it', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      user: mockLibrarianUser,
      status: 'authenticated',
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(notificationApi, 'fetchMyNotifications').mockResolvedValueOnce(mockPage);
    const triggerSpy = vi.spyOn(notificationApi, 'triggerReminders').mockResolvedValueOnce({
      dueRemindersCreated: 2,
      overdueCreated: 1,
      dispatched: 3,
    });

    const user = userEvent.setup();
    render(<NotificationsPage />);

    const triggerBtn = await screen.findByRole('button', { name: /quét nhắc nhở ngay/i });
    expect(triggerBtn).toBeInTheDocument();

    await user.click(triggerBtn);

    await waitFor(() => {
      expect(triggerSpy).toHaveBeenCalled();
      expect(screen.getByText(/Quét thành công: Tạo 2 nhắc hẹn, 1 cảnh báo quá hạn/i)).toBeInTheDocument();
    });
  });
});
