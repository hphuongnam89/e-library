export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface BookTitleDto {
  id: number;
  title: string;
  author: string;
  publisher: string;
  isbn: string;
  publicationYear: number;
  categoryId: number | null;
  categoryName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBookTitleRequest {
  title: string;
  author?: string;
  publisher?: string;
  isbn?: string;
  publicationYear?: number;
  categoryId?: number | null;
}

export interface UpdateBookTitleRequest {
  title?: string;
  author?: string;
  publisher?: string;
  isbn?: string;
  publicationYear?: number;
  categoryId?: number | null;
}

export type BookCopyStatus = 'AVAILABLE' | 'BORROWED' | 'LOST' | 'DAMAGED' | 'MAINTENANCE';

export interface BookCopyDto {
  id: number;
  bookTitleId: number;
  bookTitle: string;
  libraryId: number;
  libraryName: string;
  barcode: string;
  location: string | null;
  status: BookCopyStatus;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBookCopyRequest {
  bookTitleId: number;
  libraryId: number;
  barcode?: string;
  location?: string;
  status?: BookCopyStatus;
}

export interface UpdateBookCopyRequest {
  libraryId?: number;
  location?: string;
  status?: BookCopyStatus;
}

export interface CategoryDto {
  id: number;
  parentId: number | null;
  parentName: string | null;
  name: string;
}

export interface CreateCategoryRequest {
  name: string;
  parentId?: number | null;
}

export interface UpdateCategoryRequest {
  name?: string;
  parentId?: number | null;
}
