import { apiFetch } from './client';
import type {
  BatchCheckoutResponse,
  BorrowDto,
  BorrowsFilterParams,
  CheckoutRequest,
} from '../types/circulation';
import type { Page } from '../types/catalog';

export async function fetchMyBorrows(
  page = 0,
  size = 10,
  signal?: AbortSignal
): Promise<Page<BorrowDto>> {
  const searchParams = new URLSearchParams();
  searchParams.set('page', page.toString());
  searchParams.set('size', size.toString());

  return apiFetch<Page<BorrowDto>>(`/api/v1/me/borrows?${searchParams.toString()}`, {
    signal,
  });
}

export async function fetchBorrows(
  params: BorrowsFilterParams = {}
): Promise<Page<BorrowDto>> {
  const searchParams = new URLSearchParams();
  if (params.userId != null) searchParams.set('userId', params.userId.toString());
  if (params.status) searchParams.set('status', params.status);
  if (params.overdue != null) searchParams.set('overdue', params.overdue.toString());
  if (params.from) searchParams.set('from', params.from);
  if (params.to) searchParams.set('to', params.to);
  searchParams.set('page', (params.page ?? 0).toString());
  searchParams.set('size', (params.size ?? 10).toString());

  return apiFetch<Page<BorrowDto>>(`/api/v1/borrows?${searchParams.toString()}`, {
    signal: params.signal,
  });
}

export async function checkoutBooks(req: CheckoutRequest): Promise<BatchCheckoutResponse> {
  return apiFetch<BatchCheckoutResponse>('/api/v1/borrows', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(req),
  });
}

export async function returnBorrow(borrowId: number): Promise<BorrowDto> {
  return apiFetch<BorrowDto>(`/api/v1/borrows/${borrowId}/return`, {
    method: 'POST',
  });
}

export async function payFine(borrowId: number): Promise<BorrowDto> {
  return apiFetch<BorrowDto>(`/api/v1/borrows/${borrowId}/fine-payment`, {
    method: 'POST',
  });
}
