export type DigitalDocumentStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'PUBLISHED';
export type DigitalDocumentPermission = 'AUTHENTICATED' | 'RESTRICTED';

export interface DigitalDocumentDto {
  id: number;
  libraryId: number;
  libraryName: string;
  title: string;
  description?: string | null;
  publisher?: string | null;
  categoryId?: number | null;
  categoryName?: string | null;
  contentType: string;
  sizeBytes: number;
  status: DigitalDocumentStatus;
  permission: DigitalDocumentPermission;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentGrantDto {
  id: number;
  documentId: number;
  institutionId?: number | null;
  institutionName?: string | null;
  campusId?: number | null;
  campusName?: string | null;
  departmentId?: number | null;
  departmentName?: string | null;
  userId?: number | null;
  userFullName?: string | null;
  userEmail?: string | null;
}

export interface GrantTargetRequest {
  institutionId?: number | null;
  campusId?: number | null;
  departmentId?: number | null;
  userId?: number | null;
}

export interface UpdateDocumentGrantsRequest {
  grants: GrantTargetRequest[];
}

export interface UpdateDigitalDocumentRequest {
  title?: string;
  description?: string;
  publisher?: string;
  categoryId?: number | null;
  permission?: DigitalDocumentPermission;
  isActive?: boolean;
}

export interface ApproveDocumentRequest {
  approved: boolean;
  note?: string;
}
