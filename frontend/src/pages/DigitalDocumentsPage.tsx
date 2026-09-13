import { useState, useEffect, useCallback } from 'react';
import {
  fetchDigitalDocuments,
  submitDocumentForApproval,
  approveOrRejectDocument,
  publishDocument,
} from '../api/digital';
import { fetchCategories } from '../api/catalog';
import { useAuth } from '../hooks/useAuth';
import type { DigitalDocumentDto, DigitalDocumentStatus } from '../types/digital';
import type { CategoryDto, Page } from '../types/catalog';
import { DigitalDocViewerModal } from '../components/digital/DigitalDocViewerModal';
import { DigitalDocUploadModal } from '../components/digital/DigitalDocUploadModal';
import { DigitalDocGrantModal } from '../components/digital/DigitalDocGrantModal';
import { Pagination } from '../components/Pagination';

export function DigitalDocumentsPage() {
  const { user } = useAuth();
  const isStaff = user?.role === 'ADMIN' || user?.role === 'LIBRARIAN';

  const [documentsPage, setDocumentsPage] = useState<Page<DigitalDocumentDto>>({
    content: [],
    number: 0,
    size: 12,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
    empty: true,
  });

  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<number | undefined>(undefined);
  const [selectedStatus, setSelectedStatus] = useState<DigitalDocumentStatus | ''>('');
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Modals
  const [viewingDoc, setViewingDoc] = useState<DigitalDocumentDto | null>(null);
  const [grantingDoc, setGrantingDoc] = useState<DigitalDocumentDto | null>(null);
  const [isUploadOpen, setIsUploadOpen] = useState(false);

  const loadDocuments = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await fetchDigitalDocuments({
        categoryId: selectedCategory,
        status: selectedStatus ? selectedStatus : undefined,
        query: searchQuery,
        page,
        size: 12,
      });
      setDocumentsPage(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể tải danh sách tài liệu số');
    } finally {
      setIsLoading(false);
    }
  }, [selectedCategory, selectedStatus, searchQuery, page]);

  useEffect(() => {
    fetchCategories()
      .then((page) => setCategories(page.content))
      .catch(() => {});
  }, []);

  useEffect(() => {
    loadDocuments();
  }, [loadDocuments]);

  const handleSubmitApproval = async (doc: DigitalDocumentDto) => {
    if (!confirm(`Gửi tài liệu "${doc.title}" để phê duyệt?`)) return;
    try {
      await submitDocumentForApproval(doc.id);
      loadDocuments();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Lỗi gửi phê duyệt');
    }
  };

  const handleApprove = async (doc: DigitalDocumentDto, approved: boolean) => {
    const actionText = approved ? 'phê duyệt' : 'từ chối';
    const note = prompt(`Nhập ghi chú ${actionText}:`, approved ? 'Đạt chuẩn' : 'Cần chỉnh sửa');
    if (note === null) return;

    try {
      await approveOrRejectDocument(doc.id, { approved, note });
      loadDocuments();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : `Lỗi ${actionText}`);
    }
  };

  const handlePublish = async (doc: DigitalDocumentDto) => {
    if (!confirm(`Xuất bản tài liệu "${doc.title}" cho bạn đọc?`)) return;
    try {
      await publishDocument(doc.id);
      loadDocuments();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Lỗi xuất bản');
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Kho Tài Liệu Số (Digital Library)
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Tra cứu, đọc giáo trình điện tử và tài liệu số có bản quyền của Đại học Phú Xuân
          </p>
        </div>

        {isStaff && (
          <button
            type="button"
            onClick={() => setIsUploadOpen(true)}
            className="inline-flex items-center justify-center rounded-xl bg-blue-900 px-4 py-2.5 text-sm font-semibold text-white shadow-xs hover:bg-blue-800 transition"
          >
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Tải lên tài liệu PDF
          </button>
        )}
      </div>

      {/* Filter and Search Toolbar */}
      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <div>
            <label htmlFor="search-input" className="sr-only">Tìm kiếm tài liệu</label>
            <input
              id="search-input"
              type="text"
              placeholder="Tìm kiếm theo tiêu đề, tác giả, nhà xuất bản..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:ring-1 focus:ring-blue-900"
            />
          </div>

          <div>
            <label htmlFor="category-select" className="sr-only">Lọc theo chuyên ngành</label>
            <select
              id="category-select"
              value={selectedCategory ?? ''}
              onChange={(e) => {
                setSelectedCategory(e.target.value ? Number(e.target.value) : undefined);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 focus:border-blue-900 focus:ring-1 focus:ring-blue-900"
            >
              <option value="">-- Tất cả chuyên ngành --</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          {isStaff && (
            <div>
              <label htmlFor="status-select" className="sr-only">Trạng thái phê duyệt</label>
              <select
                id="status-select"
                value={selectedStatus}
                onChange={(e) => {
                  setSelectedStatus(e.target.value as DigitalDocumentStatus | '');
                  setPage(0);
                }}
                className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 focus:border-blue-900 focus:ring-1 focus:ring-blue-900"
              >
                <option value="">-- Tất cả trạng thái --</option>
                <option value="DRAFT">DRAFT (Bản nháp)</option>
                <option value="PENDING">PENDING (Chờ duyệt)</option>
                <option value="APPROVED">APPROVED (Đã duyệt)</option>
                <option value="PUBLISHED">PUBLISHED (Đã xuất bản)</option>
              </select>
            </div>
          )}
        </div>
      </div>

      {error && (
        <div role="alert" className="rounded-xl bg-rose-50 p-4 text-sm text-rose-700">
          {error}
        </div>
      )}

      {/* Documents Grid */}
      {isLoading ? (
        <div className="py-12 text-center text-sm text-slate-500">
          <span className="inline-block h-6 w-6 animate-spin rounded-full border-2 border-blue-900 border-t-transparent mb-2" />
          <p>Đang tải tài liệu số...</p>
        </div>
      ) : documentsPage.content.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-200 p-12 text-center">
          <p className="text-base font-semibold text-slate-700">Không tìm thấy tài liệu số phù hợp</p>
          <p className="text-sm text-slate-400 mt-1">
            Vui lòng thử tìm kiếm với từ khóa khác hoặc điều chỉnh bộ lọc chuyên ngành.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {documentsPage.content.map((doc) => {
            const sizeMb = (doc.sizeBytes / (1024 * 1024)).toFixed(2);
            return (
              <div
                key={doc.id}
                className="flex flex-col justify-between rounded-2xl border border-slate-200 bg-white p-5 shadow-xs hover:border-blue-200 hover:shadow-md transition-all"
              >
                <div>
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <span className="inline-flex items-center rounded-md bg-blue-50 px-2 py-0.5 text-xs font-semibold text-blue-700">
                      PDF • {sizeMb} MB
                    </span>

                    <div className="flex items-center space-x-1.5">
                      {isStaff && (
                        <span
                          className={`inline-flex items-center rounded-md px-2 py-0.5 text-[11px] font-bold ${
                            doc.status === 'PUBLISHED'
                              ? 'bg-emerald-100 text-emerald-800'
                              : doc.status === 'APPROVED'
                              ? 'bg-blue-100 text-blue-800'
                              : doc.status === 'PENDING'
                              ? 'bg-amber-100 text-amber-800'
                              : 'bg-slate-100 text-slate-700'
                          }`}
                        >
                          {doc.status}
                        </span>
                      )}

                      <span
                        className={`inline-flex items-center rounded-md px-2 py-0.5 text-[11px] font-semibold ${
                          doc.permission === 'AUTHENTICATED'
                            ? 'bg-purple-100 text-purple-800'
                            : 'bg-orange-100 text-orange-800'
                        }`}
                      >
                        {doc.permission === 'AUTHENTICATED' ? 'Toàn trường' : 'Giới hạn'}
                      </span>
                    </div>
                  </div>

                  <h3 className="text-base font-bold text-slate-900 line-clamp-2 hover:text-blue-900 transition">
                    {doc.title}
                  </h3>

                  <p className="text-xs text-slate-500 mt-1">
                    {doc.publisher || 'NXB Đại học'} • {doc.categoryName || 'Chung'}
                  </p>

                  {doc.description && (
                    <p className="text-xs text-slate-600 mt-2.5 line-clamp-2 leading-relaxed">
                      {doc.description}
                    </p>
                  )}
                </div>

                {/* Card Action Footer */}
                <div className="mt-5 border-t border-slate-100 pt-3 flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <button
                      type="button"
                      onClick={() => setViewingDoc(doc)}
                      className="inline-flex items-center rounded-lg bg-blue-900 px-3.5 py-1.5 text-xs font-bold text-white hover:bg-blue-800 transition"
                    >
                      <svg className="w-3.5 h-3.5 mr-1.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                      Đọc tài liệu
                    </button>

                    {isStaff && doc.permission === 'RESTRICTED' && (
                      <button
                        type="button"
                        onClick={() => setGrantingDoc(doc)}
                        className="text-xs font-semibold text-slate-600 hover:text-blue-900"
                      >
                        Phân quyền
                      </button>
                    )}
                  </div>

                  {/* Staff State Machine Buttons */}
                  {isStaff && (
                    <div className="flex flex-wrap gap-1.5 pt-1 border-t border-slate-50 text-[11px]">
                      {doc.status === 'DRAFT' && (
                        <button
                          type="button"
                          onClick={() => handleSubmitApproval(doc)}
                          className="rounded bg-slate-100 px-2 py-0.5 font-medium text-slate-700 hover:bg-slate-200"
                        >
                          Gửi duyệt
                        </button>
                      )}

                      {doc.status === 'PENDING' && (
                        <>
                          <button
                            type="button"
                            onClick={() => handleApprove(doc, true)}
                            className="rounded bg-emerald-50 px-2 py-0.5 font-medium text-emerald-700 hover:bg-emerald-100"
                          >
                            Duyệt
                          </button>
                          <button
                            type="button"
                            onClick={() => handleApprove(doc, false)}
                            className="rounded bg-rose-50 px-2 py-0.5 font-medium text-rose-700 hover:bg-rose-100"
                          >
                            Từ chối
                          </button>
                        </>
                      )}

                      {doc.status === 'APPROVED' && (
                        <>
                          <button
                            type="button"
                            onClick={() => handlePublish(doc)}
                            className="rounded bg-blue-50 px-2 py-0.5 font-medium text-blue-700 hover:bg-blue-100"
                          >
                            Xuất bản
                          </button>
                          <button
                            type="button"
                            onClick={() => handleApprove(doc, false)}
                            className="rounded bg-amber-50 px-2 py-0.5 font-medium text-amber-700 hover:bg-amber-100"
                          >
                            Làm lại
                          </button>
                        </>
                      )}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {documentsPage.totalPages > 1 && (
        <Pagination
          currentPage={page}
          totalPages={documentsPage.totalPages}
          onPageChange={setPage}
        />
      )}

      {/* Modals */}
      <DigitalDocViewerModal
        isOpen={!!viewingDoc}
        document={viewingDoc}
        currentUser={user}
        onClose={() => setViewingDoc(null)}
      />

      <DigitalDocUploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        onSuccess={loadDocuments}
      />

      <DigitalDocGrantModal
        isOpen={!!grantingDoc}
        document={grantingDoc}
        onClose={() => setGrantingDoc(null)}
        onSuccess={loadDocuments}
      />
    </div>
  );
}
