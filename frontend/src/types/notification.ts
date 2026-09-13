export type NotificationType = 'DUE_REMINDER' | 'OVERDUE' | 'SYSTEM';
export type NotificationChannel = 'EMAIL' | 'IN_APP';
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED';

export interface NotificationDto {
  id: number;
  borrowId?: number | null;
  notificationType: NotificationType;
  channel: NotificationChannel;
  title: string;
  message: string;
  deduplicationKey: string;
  status: NotificationStatus;
  sentAt?: string | null;
  readAt?: string | null;
  createdAt: string;
}

export interface UnreadCountDto {
  unreadCount: number;
}

export interface TriggerRemindersResponse {
  dueRemindersCreated: number;
  overdueCreated: number;
  dispatched: number;
}
