import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  fetchMyNotifications,
  fetchUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '../api/notification';
import type { NotificationDto } from '../types/notification';

export function NotificationBell() {
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [recentNotifications, setRecentNotifications] = useState<NotificationDto[]>([]);
  const [isOpen, setIsOpen] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const loadUnread = () => {
    fetchUnreadCount()
      .then(setUnreadCount)
      .catch(() => {});
  };

  useEffect(() => {
    loadUnread();
    // Poll unread count every 60s
    const timer = setInterval(loadUnread, 60000);
    return () => clearInterval(timer);
  }, []);

  // Fetch recent notifications when dropdown opens
  useEffect(() => {
    if (!isOpen) return;

    setLoading(true);
    fetchMyNotifications({ page: 0, size: 5 })
      .then((data) => setRecentNotifications(data.content))
      .catch(() => {})
      .finally(() => setLoading(false));

    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  const handleMarkAsRead = async (id: number) => {
    try {
      await markNotificationRead(id);
      setRecentNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, readAt: new Date().toISOString() } : n))
      );
      setUnreadCount((c) => Math.max(0, c - 1));
    } catch {
      // ignore
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await markAllNotificationsRead();
      setRecentNotifications((prev) =>
        prev.map((n) => ({ ...n, readAt: new Date().toISOString() }))
      );
      setUnreadCount(0);
    } catch {
      // ignore
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'DUE_REMINDER':
        return '⏰';
      case 'OVERDUE':
        return '🚨';
      default:
        return '📢';
    }
  };

  return (
    <div className="relative inline-block" ref={dropdownRef}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="relative flex h-9 w-9 items-center justify-center rounded-lg text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition"
        aria-label="Thông báo"
        aria-expanded={isOpen}
      >
        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.75}
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
          />
        </svg>
        {unreadCount > 0 && (
          <span
            data-testid="unread-badge"
            className="absolute -top-1 -right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-bold text-white shadow-xs"
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div
          role="region"
          aria-label="Hộp thông báo"
          className="absolute right-0 mt-2 w-80 sm:w-96 rounded-2xl border border-slate-200 bg-white shadow-xl z-50 overflow-hidden"
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3 bg-slate-50/50">
            <div className="flex items-center gap-2">
              <span className="text-sm font-bold text-slate-900">Thông báo</span>
              {unreadCount > 0 && (
                <span className="rounded-full bg-red-100 px-2 py-0.5 text-[10px] font-semibold text-red-700">
                  {unreadCount} mới
                </span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={handleMarkAllAsRead}
                className="text-xs text-blue-600 hover:text-blue-800 font-medium"
              >
                Đã đọc tất cả
              </button>
            )}
          </div>

          {/* List */}
          <div className="max-h-80 overflow-y-auto divide-y divide-slate-100">
            {loading ? (
              <div className="p-6 text-center text-xs text-slate-400">Đang tải thông báo...</div>
            ) : recentNotifications.length === 0 ? (
              <div className="p-6 text-center text-xs text-slate-400">
                <span className="block text-2xl mb-1">🔔</span>
                Không có thông báo nào
              </div>
            ) : (
              recentNotifications.map((n) => (
                <div
                  key={n.id}
                  onClick={() => !n.readAt && handleMarkAsRead(n.id)}
                  className={`flex items-start gap-3 p-3.5 transition cursor-pointer hover:bg-slate-50 ${
                    !n.readAt ? 'bg-blue-50/40' : ''
                  }`}
                >
                  <span className="text-lg shrink-0 mt-0.5">{getTypeIcon(n.notificationType)}</span>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-1">
                      <p className="text-xs font-semibold text-slate-900 truncate">{n.title}</p>
                      {!n.readAt && (
                        <span className="h-2 w-2 shrink-0 rounded-full bg-blue-600" />
                      )}
                    </div>
                    <div
                      className="mt-0.5 text-xs text-slate-600 line-clamp-2"
                      dangerouslySetInnerHTML={{ __html: n.message.replace(/<[^>]*>/g, ' ') }}
                    />
                    <span className="mt-1 block text-[10px] text-slate-400">
                      {new Date(n.createdAt).toLocaleDateString('vi-VN')}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Footer */}
          <div className="border-t border-slate-100 bg-slate-50 p-2.5 text-center">
            <Link
              to="/notifications"
              onClick={() => setIsOpen(false)}
              className="text-xs font-semibold text-blue-700 hover:text-blue-900"
            >
              Xem tất cả thông báo →
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
