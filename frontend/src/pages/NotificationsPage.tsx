import { useCallback, useEffect, useState } from 'react';
import {
  fetchMyNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  triggerReminders,
} from '../api/notification';
import type { NotificationDto, NotificationType } from '../types/notification';
import { Pagination } from '../components/Pagination';
import { useAuth } from '../hooks/useAuth';

export function NotificationsPage() {
  const { user, status: authStatus, login } = useAuth();
  const isLibrarianOrAdmin =
    authStatus === 'authenticated' && (user?.role === 'LIBRARIAN' || user?.role === 'ADMIN');

  const [notifications, setNotifications] = useState<NotificationDto[]>([]);
  const [activeTab, setActiveTab] = useState<NotificationType | 'ALL'>('ALL');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Trigger reminders feedback state
  const [triggering, setTriggering] = useState(false);
  const [triggerMsg, setTriggerMsg] = useState<string | null>(null);

  const loadNotifications = useCallback(
    (page: number, type: NotificationType | 'ALL') => {
      if (authStatus !== 'authenticated') {
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);
      fetchMyNotifications({
        type: type === 'ALL' ? undefined : type,
        page,
        size: 10,
      })
        .then((data) => {
          setNotifications(data.content);
          setTotalPages(data.totalPages);
          setTotalElements(data.totalElements);
        })
        .catch((err) => {
          setError(err.message || 'Không thể tải danh sách thông báo');
        })
        .finally(() => {
          setLoading(false);
        });
    },
    [authStatus]
  );

  useEffect(() => {
    loadNotifications(currentPage, activeTab);
  }, [currentPage, activeTab, loadNotifications]);

  const handleTabChange = (tab: NotificationType | 'ALL') => {
    setActiveTab(tab);
    setCurrentPage(0);
  };

  const handleMarkAsRead = async (id: number) => {
    try {
      await markNotificationRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, readAt: new Date().toISOString() } : n))
      );
    } catch {
      // ignore
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await markAllNotificationsRead();
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, readAt: new Date().toISOString() }))
      );
    } catch {
      // ignore
    }
  };

  const handleTriggerScan = async () => {
    setTriggering(true);
    setTriggerMsg(null);
    try {
      const res = await triggerReminders();
      setTriggerMsg(
        `Quét thành công: Tạo ${res.dueRemindersCreated} nhắc hẹn, ${res.overdueCreated} cảnh báo quá hạn, đã gửi ${res.dispatched} thông báo.`
      );
      loadNotifications(0, activeTab);
    } catch (err: unknown) {
      setTriggerMsg('Không thể kích hoạt quét nhắc hẹn: ' + ((err as Error).message || ''));
    } finally {
      setTriggering(false);
    }
  };

  if (authStatus === 'unauthenticated') {
    return (
      <div className="mx-auto max-w-4xl px-4 py-16 text-center">
        <div className="rounded-2xl border border-slate-200 bg-white p-10 shadow-xs">
          <span className="inline-block text-4xl mb-4">🔔</span>
          <h2 className="text-xl font-bold text-slate-800">Yêu cầu đăng nhập</h2>
          <p className="mt-2 text-sm text-slate-600">
            Vui lòng đăng nhập để xem thông báo mượn trả và nhắc hẹn từ Thư viện.
          </p>
          <button
            type="button"
            onClick={login}
            className="mt-6 rounded-xl bg-blue-600 px-6 py-2.5 text-sm font-semibold text-white hover:bg-blue-500 shadow-xs transition"
          >
            Đăng nhập Google
          </button>
        </div>
      </div>
    );
  }

  const getTypeBadge = (type: NotificationType) => {
    switch (type) {
      case 'DUE_REMINDER':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-semibold text-blue-700">
            ⏰ Nhắc hẹn trả
          </span>
        );
      case 'OVERDUE':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-red-50 px-2.5 py-0.5 text-xs font-semibold text-red-700">
            🚨 Quá hạn mượn
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-700">
            📢 Hệ thống
          </span>
        );
    }
  };

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      {/* Page Header */}
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            Hộp thư Thông báo{' '}
            {totalElements > 0 && (
              <span className="text-base font-normal text-slate-500">({totalElements})</span>
            )}
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Nhận thông báo nhắc hạn trả sách, cảnh báo quá hạn và tin tức quan trọng từ thư viện.
          </p>
        </div>

        <div className="flex items-center gap-3">
          {isLibrarianOrAdmin && (
            <button
              type="button"
              onClick={handleTriggerScan}
              disabled={triggering}
              className="rounded-xl bg-amber-600 px-4 py-2 text-xs font-semibold text-white hover:bg-amber-500 transition shadow-xs disabled:opacity-50"
            >
              {triggering ? 'Đang quét...' : '⚡ Quét nhắc nhở ngay'}
            </button>
          )}
          <button
            type="button"
            onClick={handleMarkAllAsRead}
            className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-xs"
          >
            Đánh dấu tất cả đã đọc
          </button>
        </div>
      </div>

      {triggerMsg && (
        <div className="mb-6 rounded-xl border border-blue-200 bg-blue-50 p-4 text-xs font-medium text-blue-800">
          {triggerMsg}
        </div>
      )}

      {/* Filter Tabs */}
      <div className="mb-6 flex items-center gap-2 border-b border-slate-200 pb-3">
        {(
          [
            { id: 'ALL', label: 'Tất cả' },
            { id: 'DUE_REMINDER', label: 'Nhắc hẹn trả' },
            { id: 'OVERDUE', label: 'Quá hạn' },
            { id: 'SYSTEM', label: 'Hệ thống' },
          ] as const
        ).map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => handleTabChange(tab.id)}
            className={`rounded-lg px-3.5 py-1.5 text-xs font-semibold transition ${
              activeTab === tab.id
                ? 'bg-blue-600 text-white shadow-xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200 hover:text-slate-900'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Notifications Content */}
      {loading ? (
        <div className="flex h-64 items-center justify-center rounded-xl border border-slate-200 bg-white p-8">
          <div className="flex flex-col items-center space-y-3">
            <div className="h-8 w-8 animate-spin rounded-full border-3 border-blue-600 border-t-transparent" />
            <p className="text-sm text-slate-500">Đang tải thông báo...</p>
          </div>
        </div>
      ) : error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 p-6 text-center text-red-700">
          <p className="font-semibold">{error}</p>
        </div>
      ) : notifications.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-300 bg-white p-12 text-center">
          <span className="inline-block text-4xl mb-3">📭</span>
          <h3 className="text-base font-semibold text-slate-800">Không có thông báo</h3>
          <p className="mt-1 text-sm text-slate-500">
            Hiện tại bạn chưa có thông báo nào trong mục này.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {notifications.map((n) => (
            <div
              key={n.id}
              onClick={() => !n.readAt && handleMarkAsRead(n.id)}
              className={`rounded-2xl border transition p-5 shadow-xs cursor-pointer ${
                !n.readAt
                  ? 'border-blue-200 bg-blue-50/30 hover:border-blue-300'
                  : 'border-slate-200 bg-white hover:border-slate-300'
              }`}
            >
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                <div className="flex items-center gap-2.5">
                  {!n.readAt && (
                    <span className="h-2 w-2 shrink-0 rounded-full bg-blue-600" title="Chưa đọc" />
                  )}
                  {getTypeBadge(n.notificationType)}
                  <h3 className="text-sm font-bold text-slate-900">{n.title}</h3>
                </div>
                <div className="flex items-center gap-2 text-xs text-slate-400">
                  <span className="rounded-md bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-600">
                    {n.channel}
                  </span>
                  <span>{new Date(n.createdAt).toLocaleString('vi-VN')}</span>
                </div>
              </div>

              <div
                className="mt-3 text-xs leading-relaxed text-slate-700 border-t border-slate-100 pt-3"
                dangerouslySetInnerHTML={{ __html: n.message }}
              />
            </div>
          ))}

          <div className="pt-4">
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
            />
          </div>
        </div>
      )}
    </div>
  );
}
