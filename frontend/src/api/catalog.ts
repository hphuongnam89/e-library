import { apiFetch } from './client';
import type {
  BookCopyDto,
  BookTitleDto,
  CategoryDto,
  CreateBookCopyRequest,
  CreateBookTitleRequest,
  CreateCategoryRequest,
  Page,
  UpdateBookCopyRequest,
  UpdateBookTitleRequest,
  UpdateCategoryRequest,
} from '../types/catalog';

export interface BookSearchParams {
  query?: string;
  categoryId?: number | null;
  page?: number;
  size?: number;
  signal?: AbortSignal;
}

export async function fetchBookTitles(params: BookSearchParams = {}): Promise<Page<BookTitleDto>> {
  const searchParams = new URLSearchParams();
  if (params.query && params.query.trim()) {
    searchParams.set('query', params.query.trim());
  }
  if (params.categoryId != null) {
    searchParams.set('categoryId', params.categoryId.toString());
  }
  searchParams.set('page', (params.page ?? 0).toString());
  searchParams.set('size', (params.size ?? 12).toString());

  return apiFetch<Page<BookTitleDto>>(`/api/v1/book-titles?${searchParams.toString()}`, {
    signal: params.signal,
  });
}

export async function fetchBookTitleById(id: number, signal?: AbortSignal): Promise<BookTitleDto> {
  return apiFetch<BookTitleDto>(`/api/v1/book-titles/${id}`, { signal });
}

export async function createBookTitle(req: CreateBookTitleRequest): Promise<BookTitleDto> {
  return apiFetch<BookTitleDto>('/api/v1/book-titles', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function updateBookTitle(id: number, req: UpdateBookTitleRequest): Promise<BookTitleDto> {
  return apiFetch<BookTitleDto>(`/api/v1/book-titles/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function deleteBookTitle(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/book-titles/${id}`, {
    method: 'DELETE',
  });
}

export async function fetchBookCopies(bookTitleId: number, signal?: AbortSignal): Promise<Page<BookCopyDto>> {
  return apiFetch<Page<BookCopyDto>>(`/api/v1/book-copies?bookTitleId=${bookTitleId}&size=100`, {
    signal,
  });
}

export async function fetchBookCopyByBarcode(barcode: string, signal?: AbortSignal): Promise<BookCopyDto> {
  return apiFetch<BookCopyDto>(`/api/v1/book-copies/barcode/${encodeURIComponent(barcode.trim())}`, {
    signal,
  });
}

export async function createBookCopy(req: CreateBookCopyRequest): Promise<BookCopyDto> {
  return apiFetch<BookCopyDto>('/api/v1/book-copies', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function updateBookCopy(id: number, req: UpdateBookCopyRequest): Promise<BookCopyDto> {
  return apiFetch<BookCopyDto>(`/api/v1/book-copies/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function deleteBookCopy(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/book-copies/${id}`, {
    method: 'DELETE',
  });
}

export async function fetchCategories(parentId?: number, page = 0, size = 100, signal?: AbortSignal): Promise<Page<CategoryDto>> {
  const query = parentId != null ? `&parentId=${parentId}` : '';
  return apiFetch<Page<CategoryDto>>(`/api/v1/categories?page=${page}&size=${size}${query}`, { signal });
}

export async function fetchRootCategories(signal?: AbortSignal): Promise<CategoryDto[]> {
  return apiFetch<CategoryDto[]>('/api/v1/categories/roots', { signal });
}

export async function fetchCategoryChildren(parentId: number, signal?: AbortSignal): Promise<CategoryDto[]> {
  return apiFetch<CategoryDto[]>(`/api/v1/categories/${parentId}/children`, { signal });
}

export async function createCategory(req: CreateCategoryRequest): Promise<CategoryDto> {
  return apiFetch<CategoryDto>('/api/v1/categories', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function updateCategory(id: number, req: UpdateCategoryRequest): Promise<CategoryDto> {
  return apiFetch<CategoryDto>(`/api/v1/categories/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function deleteCategory(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/categories/${id}`, {
    method: 'DELETE',
  });
}
