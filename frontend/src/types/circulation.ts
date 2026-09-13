export type BorrowStatus = 'BORROWED' | 'RETURNED';

export interface BorrowDto {
  id: number;
  userId: number | null;
  userEmail: string | null;
  studentCode: string | null;
  userFullName: string | null;
  bookCopyId: number | null;
  barcode: string | null;
  bookTitleId: number | null;
  bookTitle: string | null;
  libraryId: number | null;
  libraryName: string | null;
  borrowedAt: string;
  dueAt: string;
  returnedAt: string | null;
  dailyFine: number | string | null;
  fineAmount: number | string | null;
  finePaidAt: string | null;
  status: BorrowStatus;
  overdue: boolean;
}

export interface CheckoutRequest {
  studentCode: string;
  barcodes: string[];
}

export interface BatchCheckoutResponse {
  items: BorrowDto[];
}

export interface BorrowsFilterParams {
  userId?: number;
  status?: BorrowStatus;
  overdue?: boolean;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  signal?: AbortSignal;
}
