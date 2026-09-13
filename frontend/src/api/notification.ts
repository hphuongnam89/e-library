import { apiFetch } from './client';
import type { Page } from '../types/catalog';
import type {
  NotificationChannel,
  NotificationDto,
  NotificationType,
  TriggerRemindersResponse,
  UnreadCountDto,
} from '../types/notification';

export interface FetchNotificationsParams {
  type?: NotificationType;
  channel?: NotificationChannel;
  page?: number;
  size?: number;
}

export async function fetchMyNotifications(
  params: FetchNotificationsParams = {},
  signal?: AbortSignal
): Promise<Page<NotificationDto>> {
  const search = new URLSearchParams();
  if (params.type) search.set('type', params.type);
  if (params.channel) search.set('channel', params.channel);
  search.set('page', String(params.page ?? 0));
  search.set('size', String(params.size ?? 20));

  return apiFetch<Page<NotificationDto>>(`/api/v1/me/notifications?${search.toString()}`, { signal });
}

export async function fetchUnreadCount(signal?: AbortSignal): Promise<number> {
  const data = await apiFetch<UnreadCountDto>('/api/v1/me/notifications/unread-count', { signal });
  return data.unreadCount;
}

export async function markNotificationRead(id: number): Promise<NotificationDto> {
  return apiFetch<NotificationDto>(`/api/v1/me/notifications/${id}/read`, {
    method: 'PATCH',
  });
}

export async function markAllNotificationsRead(): Promise<number> {
  return apiFetch<number>('/api/v1/me/notifications/read-all', {
    method: 'POST',
  });
}

export async function triggerReminders(): Promise<TriggerRemindersResponse> {
  return apiFetch<TriggerRemindersResponse>('/api/v1/librarian/notifications/trigger-reminders', {
    method: 'POST',
  });
}
