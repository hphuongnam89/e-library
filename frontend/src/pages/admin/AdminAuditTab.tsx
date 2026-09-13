import { useState, useCallback, useEffect } from 'react';
import type { AuditLogItem } from '../../types/admin';
import { fetchAuditLogs, type AuditLogFilterParams } from '../../api/admin';

interface AdminAuditTabProps {
  onError: (message: string) => void;
}

export function AdminAuditTab({ onError }: AdminAuditTabProps) {
  const [auditLogs, setAuditLogs] = useState<AuditLogItem[]>([]);
  const [auditTotalPages, setAuditTotalPages] = useState(1);
  const [auditTotalElements, setAuditTotalElements] = useState(0);
  const [isAuditLoading, setIsAuditLoading] = useState(false);
  const [auditFilters, setAuditFilters] = useState<AuditLogFilterParams>({
    action: '',
    resourceType: '',
    from: '',
    to: '',
    page: 0,
    size: 20,
  });

  const loadAuditLogs = useCallback((params: AuditLogFilterParams, signal?: AbortSignal) => {
    setIsAuditLoading(true);
    fetchAuditLogs(params, signal)
      .then((res) => {
        setAuditLogs(res.content);
        setAuditTotalPages(res.totalPages);
        setAuditTotalElements(res.totalElements);
      })
      .catch((err) => {
        if (!signal?.aborted) onError(err.message || 'Không thể tải nhật ký kiểm toán');
      })
      .finally(() => {
        if (!signal?.aborted) setIsAuditLoading(false);
      });
  }, [onError]);

  useEffect(() => {
    const controller = new AbortController();
    loadAuditLogs(auditFilters, controller.signal);
    return () => controller.abort();
  }, [auditFilters, loadAuditLogs]);

  return (
    <div>
      {/* Audit Filters */}
      <div className="mb-6 rounded-xl border border-slate-200 bg-white p-4 shadow-xs">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <label htmlFor="audit-action-select" className="block text-xs font-semibold text-slate-600 mb-1">
              Thao tác
            </label>
            <select
              id="audit-action-select"
              value={auditFilters.action ?? ''}
              onChange={(e) =>
                setAuditFilters((prev: AuditLogFilterParams) => ({ ...prev, action: e.target.value, page: 0 }))
              }
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-800 focus:outline-hidden"
            >
              <option value="">Tất cả thao tác</option>
              <option value="USER_UPDATE_ROLE">Thay đổi vai trò (USER_UPDATE_ROLE)</option>
              <option value="USER_UPDATE_STATUS">Thay đổi trạng thái (USER_UPDATE_STATUS)</option>
              <option value="USER_BULK_IMPORT">Nhập Excel hàng loạt (USER_BULK_IMPORT)</option>
              <option value="SETTING_UPDATE">Cập nhật cấu hình (SETTING_UPDATE)</option>
            </select>
          </div>

          <div>
            <label htmlFor="audit-resource-select" className="block text-xs font-semibold text-slate-600 mb-1">
              Tài nguyên
            </label>
            <select
              id="audit-resource-select"
              value={auditFilters.resourceType ?? ''}
              onChange={(e) =>
                setAuditFilters((prev: AuditLogFilterParams) => ({ ...prev, resourceType: e.target.value, page: 0 }))
              }
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-800 focus:outline-hidden"
            >
              <option value="">Tất cả tài nguyên</option>
              <option value="USER">USER</option>
              <option value="SYSTEM_SETTING">SYSTEM_SETTING</option>
            </select>
          </div>

          <div>
            <label htmlFor="audit-from-input" className="block text-xs font-semibold text-slate-600 mb-1">
              Từ ngày
            </label>
            <input
              id="audit-from-input"
              type="date"
              value={auditFilters.from ? auditFilters.from.slice(0, 10) : ''}
              onChange={(e) =>
                setAuditFilters((prev: AuditLogFilterParams) => ({
                  ...prev,
                  from: e.target.value ? `${e.target.value}T00:00:00Z` : '',
                  page: 0,
                }))
              }
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-800 focus:outline-hidden"
            />
          </div>

          <div>
            <label htmlFor="audit-to-input" className="block text-xs font-semibold text-slate-600 mb-1">
              Đến ngày
            </label>
            <input
              id="audit-to-input"
              type="date"
              value={auditFilters.to ? auditFilters.to.slice(0, 10) : ''}
              onChange={(e) =>
                setAuditFilters((prev: AuditLogFilterParams) => ({
                  ...prev,
                  to: e.target.value ? `${e.target.value}T23:59:59Z` : '',
                  page: 0,
                }))
              }
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-800 focus:outline-hidden"
            />
          </div>
        </div>
      </div>

      {/* Audit Logs Table */}
      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Thời gian</th>
                <th className="px-4 py-3">Người thực hiện</th>
                <th className="px-4 py-3">Thao tác</th>
                <th className="px-4 py-3">Tài nguyên</th>
                <th className="px-4 py-3">Request ID</th>
                <th className="px-4 py-3">Chi tiết</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isAuditLoading ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-slate-400">
                    Đang tải nhật ký kiểm toán...
                  </td>
                </tr>
              ) : auditLogs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-slate-400">
                    Chưa có dữ liệu nhật ký kiểm toán.
                  </td>
                </tr>
              ) : (
                auditLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50/50">
                    <td className="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">
                      {new Date(log.createdAt).toLocaleString('vi-VN')}
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-semibold text-slate-800">
                        {log.userFullName || 'Hệ thống'}
                      </div>
                      {log.userEmail && (
                        <div className="text-xs text-slate-400">{log.userEmail}</div>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                          log.action.includes('ROLE')
                            ? 'bg-purple-100 text-purple-800'
                            : log.action.includes('STATUS')
                            ? 'bg-amber-100 text-amber-800'
                            : log.action.includes('IMPORT')
                            ? 'bg-blue-100 text-blue-800'
                            : 'bg-emerald-100 text-emerald-800'
                        }`}
                      >
                        {log.action}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs font-medium text-slate-700">
                      {log.resourceType || '—'} {log.resourceId ? `(#${log.resourceId})` : ''}
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-slate-500">
                      {log.requestId || '—'}
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-700 max-w-xs truncate" title={log.details ?? ''}>
                      {log.details || '—'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-xs text-slate-500">
          <span>
            Tổng cộng <strong>{auditTotalElements}</strong> bản ghi
          </span>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={auditFilters.page === 0}
              onClick={() =>
                setAuditFilters((prev: AuditLogFilterParams) => ({ ...prev, page: Math.max(0, (prev.page ?? 0) - 1) }))
              }
              className="rounded-md border border-slate-200 px-3 py-1 disabled:opacity-40"
            >
              Trang trước
            </button>
            <span className="px-2 py-1">
              Trang {(auditFilters.page ?? 0) + 1} / {Math.max(1, auditTotalPages)}
            </span>
            <button
              type="button"
              disabled={(auditFilters.page ?? 0) + 1 >= auditTotalPages}
              onClick={() =>
                setAuditFilters((prev: AuditLogFilterParams) => ({ ...prev, page: (prev.page ?? 0) + 1 }))
              }
              className="rounded-md border border-slate-200 px-3 py-1 disabled:opacity-40"
            >
              Trang sau
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
