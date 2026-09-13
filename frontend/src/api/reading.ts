import { apiFetch } from './client';
import type { Page } from '../types/catalog';
import type {
  EndReadingSessionResponse,
  HeartbeatRequest,
  HeartbeatResponse,
  ReadingHistoryItem,
  StartReadingSessionResponse,
} from '../types/reading';

export async function startReadingSession(
  documentId: number,
  signal?: AbortSignal
): Promise<StartReadingSessionResponse> {
  return apiFetch<StartReadingSessionResponse>('/api/v1/reading/sessions', {
    method: 'POST',
    body: JSON.stringify({ documentId }),
    signal,
  });
}

export async function sendReadingHeartbeat(
  sessionId: string,
  data: HeartbeatRequest,
  signal?: AbortSignal
): Promise<HeartbeatResponse> {
  return apiFetch<HeartbeatResponse>(`/api/v1/reading/sessions/${sessionId}/heartbeat`, {
    method: 'POST',
    body: JSON.stringify(data),
    signal,
  });
}

export async function endReadingSession(
  sessionId: string,
  signal?: AbortSignal
): Promise<EndReadingSessionResponse> {
  return apiFetch<EndReadingSessionResponse>(`/api/v1/reading/sessions/${sessionId}/end`, {
    method: 'POST',
    signal,
  });
}

export async function fetchMyReadingHistory(
  page = 0,
  size = 20,
  signal?: AbortSignal
): Promise<Page<ReadingHistoryItem>> {
  const search = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  return apiFetch<Page<ReadingHistoryItem>>(`/api/v1/me/reading-history?${search.toString()}`, { signal });
}
