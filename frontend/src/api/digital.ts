import { apiFetch } from './client';
import type { Page } from '../types/catalog';
import type {
  ApproveDocumentRequest,
  DigitalDocumentDto,
  DigitalDocumentStatus,
  DocumentGrantDto,
  UpdateDigitalDocumentRequest,
  UpdateDocumentGrantsRequest,
} from '../types/digital';

export interface FetchDigitalDocumentsParams {
  libraryId?: number;
  categoryId?: number;
  status?: DigitalDocumentStatus;
  query?: string;
  page?: number;
  size?: number;
}

export async function fetchDigitalDocuments(
  params: FetchDigitalDocumentsParams = {},
  signal?: AbortSignal
): Promise<Page<DigitalDocumentDto>> {
  const search = new URLSearchParams();
  if (params.libraryId != null) search.set('libraryId', String(params.libraryId));
  if (params.categoryId != null) search.set('categoryId', String(params.categoryId));
  if (params.status) search.set('status', params.status);
  if (params.query?.trim()) search.set('query', params.query.trim());
  search.set('page', String(params.page ?? 0));
  search.set('size', String(params.size ?? 20));

  return apiFetch<Page<DigitalDocumentDto>>(`/api/v1/digital-documents?${search.toString()}`, { signal });
}

export async function fetchDigitalDocumentById(id: number, signal?: AbortSignal): Promise<DigitalDocumentDto> {
  return apiFetch<DigitalDocumentDto>(`/api/v1/digital-documents/${id}`, { signal });
}

export async function uploadDigitalDocument(formData: FormData): Promise<DigitalDocumentDto> {
  return apiFetch<DigitalDocumentDto>('/api/v1/digital-documents', {
    method: 'POST',
    body: formData,
  });
}

export async function updateDigitalDocument(
  id: number,
  req: UpdateDigitalDocumentRequest
): Promise<DigitalDocumentDto> {
  return apiFetch<DigitalDocumentDto>(`/api/v1/digital-documents/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function submitDocumentForApproval(id: number): Promise<DigitalDocumentDto> {
  return apiFetch<DigitalDocumentDto>(`/api/v1/digital-documents/${id}/submit`, {
    method: 'POST',
  });
}

export async function approveOrRejectDocument(
  id: number,
  req: ApproveDocumentRequest
): Promise<DigitalDocumentDto> {
  return apiFetch<DigitalDocumentDto>(`/api/v1/digital-documents/${id}/approve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function publishDocument(id: number): Promise<DigitalDocumentDto> {
  return apiFetch<DigitalDocumentDto>(`/api/v1/digital-documents/${id}/publish`, {
    method: 'POST',
  });
}

export async function fetchDocumentGrants(id: number, signal?: AbortSignal): Promise<DocumentGrantDto[]> {
  return apiFetch<DocumentGrantDto[]>(`/api/v1/digital-documents/${id}/grants`, { signal });
}

export async function updateDocumentGrants(
  id: number,
  req: UpdateDocumentGrantsRequest
): Promise<DocumentGrantDto[]> {
  return apiFetch<DocumentGrantDto[]>(`/api/v1/digital-documents/${id}/grants`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export function getDocumentStreamUrl(id: number): string {
  return `/api/v1/digital-documents/${id}/stream`;
}
