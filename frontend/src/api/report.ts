import { apiFetch } from './client';
import type { Page } from '../types/catalog';
import type {
  DashboardSummaryDto,
  ReportFilterParams,
  ReportType,
} from '../types/report';

function buildSearchParams(params: ReportFilterParams): URLSearchParams {
  const search = new URLSearchParams();
  if (params.status) search.set('status', params.status);
  if (params.role) search.set('role', params.role);
  if (params.categoryId !== undefined) search.set('categoryId', String(params.categoryId));
  if (params.departmentId !== undefined) search.set('departmentId', String(params.departmentId));
  if (params.documentId !== undefined) search.set('documentId', String(params.documentId));
  if (params.overdue !== undefined) search.set('overdue', String(params.overdue));
  if (params.from) search.set('from', params.from);
  if (params.to) search.set('to', params.to);
  search.set('page', String(params.page ?? 0));
  search.set('size', String(params.size ?? 20));
  return search;
}

export async function fetchDashboardSummary(signal?: AbortSignal): Promise<DashboardSummaryDto> {
  return apiFetch<DashboardSummaryDto>('/api/v1/dashboard/summary', { signal });
}

export async function fetchReportData<T>(
  report: ReportType,
  params: ReportFilterParams = {},
  signal?: AbortSignal
): Promise<Page<T>> {
  const search = buildSearchParams(params);
  return apiFetch<Page<T>>(`/api/v1/reports/${report}?${search.toString()}`, { signal });
}

export async function downloadReportExcel(
  report: ReportType,
  params: ReportFilterParams = {}
): Promise<void> {
  const search = buildSearchParams(params);
  const response = await fetch(`/api/v1/reports/${report}/export?${search.toString()}`, {
    credentials: 'include',
  });

  if (!response.ok) {
    throw new Error('Không thể xuất báo cáo Excel (Mã lỗi: ' + response.status + ')');
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `report-${report}-${new Date().toISOString().slice(0, 10)}.xlsx`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}
