import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationBell } from './NotificationBell';
import * as notificationApi from '../api/notification';
import type { NotificationDto } from '../types/notification';
import type { Page } from '../types/catalog';

describe('NotificationBell', () => {
  const mockNotifications: NotificationDto[] = [
    {
      id: 1,
      borrowId: 10,
      notificationType: 'DUE_REMINDER',
      channel: 'EMAIL',
      title: 'Nhắc hẹn trả sách: Cấu trúc Dữ liệu',
      message: 'Sách sắp đến hạn trả vào ngày mai.',
      deduplicationKey: 'DUE_REMINDER:10:2026-09-14',
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
      message: 'Sách đã quá hạn 2 ngày.',
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
    size: 5,
    number: 0,
    first: true,
    last: true,
    empty: false,
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders unread badge when unreadCount > 0', async () => {
    vi.spyOn(notificationApi, 'fetchUnreadCount').mockResolvedValueOnce(3);

    render(
      <MemoryRouter>
        <NotificationBell />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('unread-badge')).toHaveTextContent('3');
    });
  });

  it('opens dropdown and displays recent notifications on click', async () => {
    vi.spyOn(notificationApi, 'fetchUnreadCount').mockResolvedValueOnce(1);
    vi.spyOn(notificationApi, 'fetchMyNotifications').mockResolvedValueOnce(mockPage);

    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <NotificationBell />
      </MemoryRouter>
    );

    const bellBtn = screen.getByRole('button', { name: /thông báo/i });
    await user.click(bellBtn);

    await waitFor(() => {
      expect(screen.getByText('Nhắc hẹn trả sách: Cấu trúc Dữ liệu')).toBeInTheDocument();
      expect(screen.getByText('Cảnh báo quá hạn: Lập trình Java')).toBeInTheDocument();
      expect(screen.getByText('Xem tất cả thông báo →')).toBeInTheDocument();
    });
  });

  it('calls markAllNotificationsRead when clicking Đã đọc tất cả', async () => {
    vi.spyOn(notificationApi, 'fetchUnreadCount').mockResolvedValueOnce(1);
    vi.spyOn(notificationApi, 'fetchMyNotifications').mockResolvedValueOnce(mockPage);
    const markAllSpy = vi.spyOn(notificationApi, 'markAllNotificationsRead').mockResolvedValueOnce(1);

    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <NotificationBell />
      </MemoryRouter>
    );

    const bellBtn = screen.getByRole('button', { name: /thông báo/i });
    await user.click(bellBtn);

    await waitFor(() => {
      expect(screen.getByText('Đã đọc tất cả')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Đã đọc tất cả'));
    expect(markAllSpy).toHaveBeenCalled();
  });
});
