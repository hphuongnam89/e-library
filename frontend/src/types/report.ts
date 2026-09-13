export type ReportType = 'books' | 'borrows' | 'reading' | 'users';

export interface DashboardSummaryDto {
  books: {
    totalTitles: number;
    totalCopies: number;
    availableCopies: number;
    borrowedCopies: number;
    maintenanceOrDamagedCopies: number;
  };
  digital: {
    totalDocuments: number;
    totalReadingSessions: number;
    totalReadingHours: number;
  };
  circulation: {
    activeBorrows: number;
    overdueBorrows: number;
    returnedBorrows: number;
    totalUnpaidFines: number;
  };
  users: {
    totalUsers: number;
    activeUsers: number;
    studentUsers: number;
    lecturerUsers: number;
  };
  topBorrowedBooks: Array<{
    bookTitleId: number;
    title: string;
    author: string;
    borrowCount: number;
  }>;
  topReadDocuments: Array<{
    documentId: number;
    title: string;
    author: string;
    activeSeconds: number;
    sessionCount: number;
  }>;
  recentBorrows: Array<{
    borrowId: number;
    studentCode: string | null;
    userName: string | null;
    bookTitle: string | null;
    barcode: string | null;
    borrowedAt: string;
    dueAt: string;
    status: string | null;
  }>;
}

export interface BookReportItemDto {
  copyId: number;
  titleId: number;
  barcode: string;
  title: string;
  author: string;
  isbn: string;
  categoryName: string | null;
  status: string;
  libraryName: string | null;
  createdAt: string;
}

export interface BorrowReportItemDto {
  borrowId: number;
  studentCode: string | null;
  userName: string | null;
  departmentName: string | null;
  bookTitle: string | null;
  barcode: string | null;
  borrowedAt: string;
  dueAt: string;
  returnedAt: string | null;
  status: string;
  fineAmount: number | null;
  finePaidAt: string | null;
}

export interface ReadingReportItemDto {
  documentId: number;
  documentTitle: string;
  userId: number;
  studentCode: string | null;
  userName: string | null;
  departmentName: string | null;
  totalActiveSeconds: number;
  sessionCount: number;
  lastActiveAt: string | null;
}

export interface UserReportItemDto {
  userId: number;
  email: string;
  fullName: string;
  studentCode: string | null;
  role: string;
  status: string;
  departmentName: string | null;
  activeBorrowsCount: number;
  totalReadingSeconds: number;
  createdAt: string;
}

export interface ReportFilterParams {
  status?: string;
  role?: string;
  categoryId?: number;
  departmentId?: number;
  documentId?: number;
  overdue?: boolean;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}
