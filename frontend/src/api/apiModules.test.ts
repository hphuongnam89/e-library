import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import * as client from './client';
import * as catalog from './catalog';
import * as circulation from './circulation';
import * as admin from './admin';
import * as report from './report';
import * as digital from './digital';
import * as org from './organization';

describe('API Modules Unit Tests', () => {
  let apiFetchSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    vi.restoreAllMocks();
    apiFetchSpy = vi.spyOn(client, 'apiFetch');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('catalog API', () => {
    it('fetchBookTitles calls correct URL with query and pagination', async () => {
      apiFetchSpy.mockResolvedValueOnce({ content: [], totalPages: 1 });
      await catalog.fetchBookTitles({ query: 'Spring Boot', categoryId: 2, page: 1, size: 20 });
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/book-titles?query=Spring+Boot&categoryId=2&page=1&size=20',
        expect.anything()
      );
    });

    it('CRUD on book titles', async () => {
      apiFetchSpy.mockResolvedValue({ id: 10 });
      await catalog.fetchBookTitleById(10);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/book-titles/10', expect.anything());

      await catalog.createBookTitle({ isbn: '123', title: 'Test', author: 'Author' });
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/book-titles',
        expect.objectContaining({ method: 'POST' })
      );

      await catalog.updateBookTitle(10, { title: 'Updated' });
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/book-titles/10',
        expect.objectContaining({ method: 'PATCH' })
      );

      await catalog.deleteBookTitle(10);
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/book-titles/10',
        expect.objectContaining({ method: 'DELETE' })
      );
    });

    it('Copies and categories operations', async () => {
      apiFetchSpy.mockResolvedValue({});
      await catalog.fetchBookCopies(5);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/book-copies?bookTitleId=5&size=100', expect.anything());

      await catalog.fetchBookCopyByBarcode('BC-001');
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/book-copies/barcode/BC-001', expect.anything());

      await catalog.createBookCopy({ bookTitleId: 5, libraryId: 1, barcode: 'BC-002', location: 'Kệ A1' });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/book-copies', expect.objectContaining({ method: 'POST' }));

      await catalog.updateBookCopy(2, { status: 'MAINTENANCE' });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/book-copies/2', expect.objectContaining({ method: 'PATCH' }));

      await catalog.deleteBookCopy(2);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/book-copies/2', expect.objectContaining({ method: 'DELETE' }));

      await catalog.fetchCategories(1);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/categories?page=0&size=100&parentId=1', expect.anything());

      await catalog.fetchRootCategories();
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/categories/roots', expect.anything());

      await catalog.fetchCategoryChildren(3);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/categories/3/children', expect.anything());

      await catalog.createCategory({ name: 'IT' });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/categories', expect.objectContaining({ method: 'POST' }));

      await catalog.updateCategory(3, { name: 'Tech' });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/categories/3', expect.objectContaining({ method: 'PATCH' }));

      await catalog.deleteCategory(3);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/categories/3', expect.objectContaining({ method: 'DELETE' }));
    });
  });

  describe('circulation API', () => {
    it('handles my borrows and system borrows query', async () => {
      apiFetchSpy.mockResolvedValue({});
      await circulation.fetchMyBorrows(1, 15);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/me/borrows?page=1&size=15', expect.anything());

      await circulation.fetchBorrows({ userId: 10, status: 'BORROWED', overdue: true, page: 0, size: 10 });
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/borrows?userId=10&status=BORROWED&overdue=true&page=0&size=10',
        expect.anything()
      );
    });

    it('checkout, return and pay fine', async () => {
      apiFetchSpy.mockResolvedValue({});
      await circulation.checkoutBooks({ studentCode: 'PXU001', barcodes: ['BC-001'] });
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/borrows',
        expect.objectContaining({ method: 'POST' })
      );

      await circulation.returnBorrow(99);
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/borrows/99/return',
        expect.objectContaining({ method: 'POST' })
      );

      await circulation.payFine(99);
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/borrows/99/fine-payment',
        expect.objectContaining({ method: 'POST' })
      );
    });
  });

  describe('admin API', () => {
    it('fetchAdminUsers, updateAdminUser, importUsersExcel', async () => {
      apiFetchSpy.mockResolvedValue({});
      await admin.fetchAdminUsers({ search: 'Nam', role: 'STUDENT', status: 'ACTIVE', departmentId: 3 });
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/admin/users?search=Nam&role=STUDENT&status=ACTIVE&departmentId=3&page=0&size=20',
        expect.anything()
      );

      await admin.updateAdminUser(1, { status: 'ACTIVE' });
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/admin/users/1',
        expect.objectContaining({ method: 'PATCH' })
      );

      const fakeFile = new File(['csv content'], 'users.xlsx');
      await admin.importUsersExcel(fakeFile);
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/admin/users/import',
        expect.objectContaining({ method: 'POST' })
      );
    });

    it('audit logs and system settings', async () => {
      apiFetchSpy.mockResolvedValue({});
      await admin.fetchAuditLogs({ action: 'CREATE', resourceType: 'BOOK', userId: 1, from: '2026-01-01', to: '2026-01-31' });
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/admin/audit?action=CREATE&resourceType=BOOK&userId=1&from=2026-01-01&to=2026-01-31&page=0&size=20',
        expect.anything()
      );

      await admin.fetchSystemSettings();
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/admin/settings', expect.anything());

      await admin.updateSystemSetting('BORROW_LIMIT', '10');
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/admin/settings/BORROW_LIMIT',
        expect.objectContaining({ method: 'PATCH', body: JSON.stringify({ value: '10' }) })
      );
    });
  });

  describe('report API', () => {
    it('fetchDashboardSummary and fetchReportData', async () => {
      apiFetchSpy.mockResolvedValue({});
      await report.fetchDashboardSummary();
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/dashboard/summary', expect.anything());

      await report.fetchReportData('books', { categoryId: 10, from: '2026-01-01' });
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/reports/books?categoryId=10&from=2026-01-01&page=0&size=20',
        expect.anything()
      );
    });

    it('downloadReportExcel handles export download', async () => {
      const mockBlob = new Blob(['mock excel'], { type: 'application/vnd.ms-excel' });
      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
        new Response(mockBlob, { status: 200 })
      );
      vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
      window.URL.createObjectURL = vi.fn().mockReturnValue('blob:http://localhost/test');
      window.URL.revokeObjectURL = vi.fn();

      await report.downloadReportExcel('borrows', { status: 'OVERDUE' });
      expect(window.URL.createObjectURL).toHaveBeenCalled();
      expect(window.URL.revokeObjectURL).toHaveBeenCalled();
    });
  });

  describe('digital API', () => {
    it('fetchDigitalDocuments and stream url', async () => {
      apiFetchSpy.mockResolvedValue({});
      await digital.fetchDigitalDocuments({ query: 'Machine Learning', libraryId: 1 });
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/digital-documents?libraryId=1&query=Machine+Learning&page=0&size=20',
        expect.anything()
      );

      expect(digital.getDocumentStreamUrl(42)).toBe('/api/v1/digital-documents/42/stream');

      await digital.fetchDigitalDocumentById(42);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/digital-documents/42', expect.anything());

      const fd = new FormData();
      await digital.uploadDigitalDocument(fd);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/digital-documents', expect.objectContaining({ method: 'POST' }));

      await digital.updateDigitalDocument(42, { title: 'New Doc' });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/digital-documents/42', expect.objectContaining({ method: 'PATCH' }));

      await digital.submitDocumentForApproval(42);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/digital-documents/42/submit', expect.objectContaining({ method: 'POST' }));

      await digital.approveOrRejectDocument(42, { approved: true });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/digital-documents/42/approve', expect.objectContaining({ method: 'POST' }));

      await digital.publishDocument(42);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/digital-documents/42/publish', expect.objectContaining({ method: 'POST' }));

      await digital.fetchDocumentGrants(42);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/digital-documents/42/grants', expect.anything());

      await digital.updateDocumentGrants(42, { grants: [] });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/digital-documents/42/grants', expect.objectContaining({ method: 'PUT' }));
    });
  });

  describe('organization API', () => {
    it('institutions, campuses, libraries, departments CRUD and assign user', async () => {
      apiFetchSpy.mockResolvedValue({});

      await org.fetchInstitutions();
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/institutions?page=0&size=50', expect.anything());

      await org.createInstitution({ name: 'PXU' });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/institutions', expect.objectContaining({ method: 'POST' }));

      await org.updateInstitution(1, { name: 'PXU Univ' });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/institutions/1', expect.objectContaining({ method: 'PATCH' }));

      await org.deleteInstitution(1);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/institutions/1', expect.objectContaining({ method: 'DELETE' }));

      await org.fetchCampuses(1);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/campuses?page=0&size=50&institutionId=1', expect.anything());

      await org.createCampus({ institutionId: 1, name: 'Main' });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/campuses', expect.objectContaining({ method: 'POST' }));

      await org.updateCampus(2, { name: 'Branch' });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/campuses/2', expect.objectContaining({ method: 'PATCH' }));

      await org.deleteCampus(2);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/campuses/2', expect.objectContaining({ method: 'DELETE' }));

      await org.fetchLibraries(2);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/libraries?page=0&size=50&campusId=2', expect.anything());

      await org.createLibrary({ campusId: 2, name: 'Library A', address: 'HN' });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/libraries', expect.objectContaining({ method: 'POST' }));

      await org.updateLibrary(3, { name: 'Library B', address: 'HN' });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/libraries/3', expect.objectContaining({ method: 'PATCH' }));

      await org.deleteLibrary(3);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/libraries/3', expect.objectContaining({ method: 'DELETE' }));

      await org.fetchDepartments(3);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/departments?page=0&size=50&libraryId=3', expect.anything());

      await org.createDepartment({ libraryId: 3, name: 'IT' });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/departments', expect.objectContaining({ method: 'POST' }));

      await org.updateDepartment(4, { name: 'Computer Science' });
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/departments/4', expect.objectContaining({ method: 'PATCH' }));

      await org.deleteDepartment(4);
      expect(apiFetchSpy).toHaveBeenCalledWith('/api/v1/departments/4', expect.objectContaining({ method: 'DELETE' }));

      await org.assignUserDepartment(100, 4);
      expect(apiFetchSpy).toHaveBeenCalledWith(
        '/api/v1/admin/users/100/department',
        expect.objectContaining({ method: 'PATCH', body: JSON.stringify({ departmentId: 4 }) })
      );
    });
  });
});
