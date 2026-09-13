export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  studentCode?: string | null;
  role: 'STUDENT' | 'LECTURER' | 'LIBRARIAN' | 'ADMIN';
  status: 'ACTIVE' | 'INACTIVE';
  departmentId?: number | null;
  departmentName?: string | null;
  createdAt: string;
}

export interface UpdateUserRequest {
  role?: 'STUDENT' | 'LECTURER' | 'LIBRARIAN' | 'ADMIN';
  status?: 'ACTIVE' | 'INACTIVE';
  departmentId?: number | null;
}

export interface RowError {
  rowNumber: number;
  email: string;
  message: string;
}

export interface UserImportResult {
  totalRows: number;
  importedCount: number;
  failedCount: number;
  errors: RowError[];
}

export interface AuditLogItem {
  id: number;
  userId?: number | null;
  userEmail?: string | null;
  userFullName?: string | null;
  action: string;
  resourceType?: string | null;
  resourceId?: string | null;
  requestId?: string | null;
  details?: string | null;
  createdAt: string;
}

export interface SystemSetting {
  id: number;
  key: string;
  value: string;
  description?: string | null;
  isSecret: boolean;
  updatedAt: string;
  updatedByEmail?: string | null;
}
