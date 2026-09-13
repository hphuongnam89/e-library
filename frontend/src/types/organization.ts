export interface InstitutionDto {
  id: number;
  name: string;
  createdAt: string;
}

export interface CreateInstitutionRequest {
  name: string;
}

export interface UpdateInstitutionRequest {
  name: string;
}

export interface CampusDto {
  id: number;
  institutionId: number;
  institutionName: string;
  name: string;
}

export interface CreateCampusRequest {
  institutionId: number;
  name: string;
}

export interface UpdateCampusRequest {
  name: string;
}

export interface LibraryDto {
  id: number;
  campusId: number;
  campusName: string;
  name: string;
  address: string;
}

export interface CreateLibraryRequest {
  campusId: number;
  name: string;
  address: string;
}

export interface UpdateLibraryRequest {
  name: string;
  address: string;
}

export interface DepartmentDto {
  id: number;
  libraryId: number;
  libraryName: string;
  name: string;
}

export interface CreateDepartmentRequest {
  libraryId: number;
  name: string;
}

export interface UpdateDepartmentRequest {
  name: string;
}

export interface AssignDepartmentRequest {
  departmentId: number | null;
}
