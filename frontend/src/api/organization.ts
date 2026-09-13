import { apiFetch } from './client';
import type { Page } from '../types/catalog';
import type {
  CampusDto,
  CreateCampusRequest,
  CreateDepartmentRequest,
  CreateInstitutionRequest,
  CreateLibraryRequest,
  DepartmentDto,
  InstitutionDto,
  LibraryDto,
  UpdateCampusRequest,
  UpdateDepartmentRequest,
  UpdateInstitutionRequest,
  UpdateLibraryRequest,
} from '../types/organization';
import type { UserDto } from '../hooks/useAuth';

// Institutions
export async function fetchInstitutions(page = 0, size = 50, signal?: AbortSignal): Promise<Page<InstitutionDto>> {
  return apiFetch<Page<InstitutionDto>>(`/api/v1/institutions?page=${page}&size=${size}`, { signal });
}

export async function createInstitution(req: CreateInstitutionRequest): Promise<InstitutionDto> {
  return apiFetch<InstitutionDto>('/api/v1/institutions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function updateInstitution(id: number, req: UpdateInstitutionRequest): Promise<InstitutionDto> {
  return apiFetch<InstitutionDto>(`/api/v1/institutions/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function deleteInstitution(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/institutions/${id}`, {
    method: 'DELETE',
  });
}

// Campuses
export async function fetchCampuses(institutionId?: number, page = 0, size = 50, signal?: AbortSignal): Promise<Page<CampusDto>> {
  const query = institutionId != null ? `&institutionId=${institutionId}` : '';
  return apiFetch<Page<CampusDto>>(`/api/v1/campuses?page=${page}&size=${size}${query}`, { signal });
}

export async function createCampus(req: CreateCampusRequest): Promise<CampusDto> {
  return apiFetch<CampusDto>('/api/v1/campuses', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function updateCampus(id: number, req: UpdateCampusRequest): Promise<CampusDto> {
  return apiFetch<CampusDto>(`/api/v1/campuses/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function deleteCampus(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/campuses/${id}`, {
    method: 'DELETE',
  });
}

// Libraries
export async function fetchLibraries(campusId?: number, page = 0, size = 50, signal?: AbortSignal): Promise<Page<LibraryDto>> {
  const query = campusId != null ? `&campusId=${campusId}` : '';
  return apiFetch<Page<LibraryDto>>(`/api/v1/libraries?page=${page}&size=${size}${query}`, { signal });
}

export async function createLibrary(req: CreateLibraryRequest): Promise<LibraryDto> {
  return apiFetch<LibraryDto>('/api/v1/libraries', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function updateLibrary(id: number, req: UpdateLibraryRequest): Promise<LibraryDto> {
  return apiFetch<LibraryDto>(`/api/v1/libraries/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function deleteLibrary(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/libraries/${id}`, {
    method: 'DELETE',
  });
}

// Departments
export async function fetchDepartments(libraryId?: number, page = 0, size = 50, signal?: AbortSignal): Promise<Page<DepartmentDto>> {
  const query = libraryId != null ? `&libraryId=${libraryId}` : '';
  return apiFetch<Page<DepartmentDto>>(`/api/v1/departments?page=${page}&size=${size}${query}`, { signal });
}

export async function createDepartment(req: CreateDepartmentRequest): Promise<DepartmentDto> {
  return apiFetch<DepartmentDto>('/api/v1/departments', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function updateDepartment(id: number, req: UpdateDepartmentRequest): Promise<DepartmentDto> {
  return apiFetch<DepartmentDto>(`/api/v1/departments/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
}

export async function deleteDepartment(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/departments/${id}`, {
    method: 'DELETE',
  });
}

// Assign Department to User
export async function assignUserDepartment(userId: number, departmentId: number | null): Promise<UserDto> {
  return apiFetch<UserDto>(`/api/v1/admin/users/${userId}/department`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ departmentId }),
  });
}
