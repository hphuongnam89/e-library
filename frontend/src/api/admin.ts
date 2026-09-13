import { apiFetch } from './client';
import type { Page } from '../types/catalog';
import type {
  AdminUser,
  AuditLogItem,
  SystemSetting,
  UpdateUserRequest,
  UserImportResult,
} from '../types/admin';

export interface AdminUserFilterParams {
  search?: string;
  role?: string;
  status?: string;
  departmentId?: number;
  page?: number;
  size?: number;
}

export interface AuditLogFilterParams {
  action?: string;
  resourceType?: string;
  userId?: number;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export async function fetchAdminUsers(
  params: AdminUserFilterParams = {},
  signal?: AbortSignal
): Promise<Page<AdminUser>> {
  const search = new URLSearchParams();
  if (params.search) search.set('search', params.search);
  if (params.role) search.set('role', params.role);
  if (params.status) search.set('status', params.status);
  if (params.departmentId !== undefined) search.set('departmentId', String(params.departmentId));
  search.set('page', String(params.page ?? 0));
  search.set('size', String(params.size ?? 20));

  return apiFetch<Page<AdminUser>>(`/api/v1/admin/users?${search.toString()}`, { signal });
}

export async function updateAdminUser(
  id: number,
  req: UpdateUserRequest
): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/v1/admin/users/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function importUsersExcel(file: File): Promise<UserImportResult> {
  const formData = new FormData();
  formData.append('file', file);
  return apiFetch<UserImportResult>('/api/v1/admin/users/import', {
    method: 'POST',
    body: formData,
  });
}

export async function fetchAuditLogs(
  params: AuditLogFilterParams = {},
  signal?: AbortSignal
): Promise<Page<AuditLogItem>> {
  const search = new URLSearchParams();
  if (params.action) search.set('action', params.action);
  if (params.resourceType) search.set('resourceType', params.resourceType);
  if (params.userId !== undefined) search.set('userId', String(params.userId));
  if (params.from) search.set('from', params.from);
  if (params.to) search.set('to', params.to);
  search.set('page', String(params.page ?? 0));
  search.set('size', String(params.size ?? 20));

  return apiFetch<Page<AuditLogItem>>(`/api/v1/admin/audit?${search.toString()}`, { signal });
}

export async function fetchSystemSettings(signal?: AbortSignal): Promise<SystemSetting[]> {
  return apiFetch<SystemSetting[]>('/api/v1/admin/settings', { signal });
}

export async function updateSystemSetting(key: string, value: string): Promise<SystemSetting> {
  return apiFetch<SystemSetting>(`/api/v1/admin/settings/${encodeURIComponent(key)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ value }),
  });
}
