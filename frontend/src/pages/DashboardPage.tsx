import { useCallback, useEffect, useState } from 'react';
import {
  downloadReportExcel,
  fetchDashboardSummary,
  fetchReportData,
} from '../api/report';
import type {
  BookReportItemDto,
  BorrowReportItemDto,
  DashboardSummaryDto,
  ReadingReportItemDto,
  ReportType,
  UserReportItemDto,
} from '../types/report';
import { Pagination } from '../components/Pagination';
import { useAuth } from '../hooks/useAuth';
import { DashboardSummarySection } from './dashboard/DashboardSummarySection';
import { BookReportTable } from './dashboard/BookReportTable';
import { BorrowReportTable } from './dashboard/BorrowReportTable';
import { ReadingReportTable } from './dashboard/ReadingReportTable';
import { UserReportTable } from './dashboard/UserReportTable';

export function DashboardPage() {
  const { user, status: authStatus, login } = useAuth();
  const isLibrarianOrAdmin =
    authStatus === 'authenticated' && (user?.role === 'LIBRARIAN' || user?.role === 'ADMIN');

  // Main navigation tab
  const [mainTab, setMainTab] = useState<'DASHBOARD' | 'REPORTS'>('DASHBOARD');

  // Dashboard Summary State
  const [summary, setSummary] = useState<DashboardSummaryDto | null>(null);
  const [loadingSummary, setLoadingSummary] = useState(true);
  const [summaryError, setSummaryError] = useState<string | null>(null);

  // Reports Tab State
  const [reportType, setReportType] = useState<ReportType>('books');
  const [reportPage, setReportPage] = useState(0);
  const [reportTotalPages, setReportTotalPages] = useState(0);
  const [reportTotalElements, setReportTotalElements] = useState(0);
  const [loadingReport, setLoadingReport] = useState(false);
  const [reportError, setReportError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);

  // Filters State
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [filterRole, setFilterRole] = useState<string>('');
  const [filterOverdue, setFilterOverdue] = useState<boolean>(false);
  const [filterFrom, setFilterFrom] = useState<string>('');
  const [filterTo, setFilterTo] = useState<string>('');

  // Report Data
  const [booksData, setBooksData] = useState<BookReportItemDto[]>([]);
  const [borrowsData, setBorrowsData] = useState<BorrowReportItemDto[]>([]);
  const [readingData, setReadingData] = useState<ReadingReportItemDto[]>([]);
  const [usersData, setUsersData] = useState<UserReportItemDto[]>([]);

  // Load Dashboard Summary
  const loadSummary = useCallback(() => {
    if (!isLibrarianOrAdmin) return;
    setLoadingSummary(true);
    setSummaryError(null);
    fetchDashboardSummary()
      .then((data) => setSummary(data))
      .catch((err) => setSummaryError(err.message || 'Không thể tải số liệu tổng quan'))
      .finally(() => setLoadingSummary(false));
  }, [isLibrarianOrAdmin]);

  // Load Report Data
  const loadReport = useCallback(() => {
    if (!isLibrarianOrAdmin || mainTab !== 'REPORTS') return;
    setLoadingReport(true);
    setReportError(null);

    const params = {
      page: reportPage,
      size: 15,
      status: filterStatus || undefined,
      role: filterRole || undefined,
      overdue: reportType === 'borrows' && filterOverdue ? true : undefined,
      from: filterFrom ? new Date(filterFrom).toISOString() : undefined,
      to: filterTo ? new Date(filterTo + 'T23:59:59').toISOString() : undefined,
    };

    if (reportType === 'books') {
      fetchReportData<BookReportItemDto>('books', params)
        .then((data) => {
          setReportTotalPages(data.totalPages);
          setReportTotalElements(data.totalElements);
          setBooksData(data.content);
        })
        .catch((err) => setReportError(err.message || 'Không thể tải dữ liệu báo cáo'))
        .finally(() => setLoadingReport(false));
    } else if (reportType === 'borrows') {
      fetchReportData<BorrowReportItemDto>('borrows', params)
        .then((data) => {
          setReportTotalPages(data.totalPages);
          setReportTotalElements(data.totalElements);
          setBorrowsData(data.content);
        })
        .catch((err) => setReportError(err.message || 'Không thể tải dữ liệu báo cáo'))
        .finally(() => setLoadingReport(false));
    } else if (reportType === 'reading') {
      fetchReportData<ReadingReportItemDto>('reading', params)
        .then((data) => {
          setReportTotalPages(data.totalPages);
          setReportTotalElements(data.totalElements);
          setReadingData(data.content);
        })
        .catch((err) => setReportError(err.message || 'Không thể tải dữ liệu báo cáo'))
        .finally(() => setLoadingReport(false));
    } else if (reportType === 'users') {
      fetchReportData<UserReportItemDto>('users', params)
        .then((data) => {
          setReportTotalPages(data.totalPages);
          setReportTotalElements(data.totalElements);
          setUsersData(data.content);
        })
        .catch((err) => setReportError(err.message || 'Không thể tải dữ liệu báo cáo'))
        .finally(() => setLoadingReport(false));
    }
  }, [
    isLibrarianOrAdmin,
    mainTab,
    reportType,
    reportPage,
    filterStatus,
    filterRole,
    filterOverdue,
    filterFrom,
    filterTo,
  ]);

  useEffect(() => {
    if (mainTab === 'DASHBOARD') {
      loadSummary();
    } else {
      loadReport();
    }
  }, [mainTab, loadSummary, loadReport]);

  const handleExportExcel = async () => {
    try {
      setExporting(true);
      await downloadReportExcel(reportType, {
        status: filterStatus || undefined,
        role: filterRole || undefined,
        overdue: reportType === 'borrows' && filterOverdue ? true : undefined,
        from: filterFrom ? new Date(filterFrom).toISOString() : undefined,
        to: filterTo ? new Date(filterTo + 'T23:59:59').toISOString() : undefined,
      });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Lỗi khi xuất file Excel';
      alert(message);
    } finally {
      setExporting(false);
    }
  };

  const handleReportTypeChange = (type: ReportType) => {
    setReportType(type);
    setReportPage(0);
    setFilterStatus('');
    setFilterRole('');
    setFilterOverdue(false);
  };

  if (authStatus === 'loading') {
    return (
      <div className="py-20 text-center">
        <p className="text-sm text-slate-500">Đang kiểm tra quyền truy cập…</p>
      </div>
    );
  }

  if (!isLibrarianOrAdmin) {
    return (
      <div className="mx-auto max-w-xl py-16 text-center">
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-8">
          <div className="text-4xl mb-3">🔒</div>
          <h2 className="text-xl font-bold text-amber-900">Quyền truy cập bị hạn chế</h2>
          <p className="mt-2 text-sm text-amber-700">
            Chức năng <strong>Báo cáo & Thống kê</strong> chỉ dành cho Thủ thư và Quản trị viên hệ thống.
          </p>
          {authStatus !== 'authenticated' && (
            <button
              type="button"
              onClick={login}
              className="mt-6 rounded-xl bg-blue-900 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-800 transition"
            >
              Đăng nhập tài khoản trường
            </button>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      {/* Header */}
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Báo cáo & Thống kê</h1>
          <p className="mt-1 text-sm text-slate-600">
            Theo dõi tổng quan hoạt động thư viện, lưu thông sách, đọc tài liệu số và xuất báo cáo Excel.
          </p>
        </div>

        {/* Top Tab Toggle */}
        <div className="inline-flex rounded-xl bg-slate-100 p-1">
          <button
            type="button"
            onClick={() => setMainTab('DASHBOARD')}
            className={`rounded-lg px-4 py-2 text-xs font-semibold transition ${
              mainTab === 'DASHBOARD'
                ? 'bg-white text-blue-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            📊 Tổng quan thư viện
          </button>
          <button
            type="button"
            onClick={() => setMainTab('REPORTS')}
            className={`rounded-lg px-4 py-2 text-xs font-semibold transition ${
              mainTab === 'REPORTS'
                ? 'bg-white text-blue-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            📑 Báo cáo chi tiết & Xuất Excel
          </button>
        </div>
      </div>

      {/* TAB 1: DASHBOARD OVERVIEW */}
      {mainTab === 'DASHBOARD' && (
        <div className="space-y-6">
          <DashboardSummarySection
            summary={summary}
            loading={loadingSummary}
            error={summaryError}
          />
        </div>
      )}

      {/* TAB 2: DETAILED REPORTS & EXPORT */}
      {mainTab === 'REPORTS' && (
        <div className="space-y-6">
          {/* Sub-navigation for Report Type */}
          <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 pb-4">
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => handleReportTypeChange('books')}
                className={`rounded-xl px-4 py-2 text-xs font-semibold transition ${
                  reportType === 'books'
                    ? 'bg-blue-900 text-white shadow-xs'
                    : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-50'
                }`}
              >
                📚 Sách & Bản sao
              </button>
              <button
                type="button"
                onClick={() => handleReportTypeChange('borrows')}
                className={`rounded-xl px-4 py-2 text-xs font-semibold transition ${
                  reportType === 'borrows'
                    ? 'bg-blue-900 text-white shadow-xs'
                    : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-50'
                }`}
              >
                🔄 Mượn - Trả & Phạt
              </button>
              <button
                type="button"
                onClick={() => handleReportTypeChange('reading')}
                className={`rounded-xl px-4 py-2 text-xs font-semibold transition ${
                  reportType === 'reading'
                    ? 'bg-blue-900 text-white shadow-xs'
                    : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-50'
                }`}
              >
                💻 Đọc Tài liệu số
              </button>
              <button
                type="button"
                onClick={() => handleReportTypeChange('users')}
                className={`rounded-xl px-4 py-2 text-xs font-semibold transition ${
                  reportType === 'users'
                    ? 'bg-blue-900 text-white shadow-xs'
                    : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-50'
                }`}
              >
                👥 Độc giả
              </button>
            </div>

            {/* Export XLSX Button */}
            <button
              type="button"
              onClick={handleExportExcel}
              disabled={exporting}
              className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-semibold text-white hover:bg-emerald-500 transition shadow-xs disabled:opacity-50"
            >
              {exporting ? (
                <>
                  <span className="animate-spin">⏳</span>
                  <span>Đang xuất file...</span>
                </>
              ) : (
                <>
                  <span>📥</span>
                  <span>Xuất file Excel (.xlsx)</span>
                </>
              )}
            </button>
          </div>

          {/* Filter Bar */}
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-5 items-end">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Từ ngày</label>
                <input
                  type="date"
                  value={filterFrom}
                  onChange={(e) => {
                    setFilterFrom(e.target.value);
                    setReportPage(0);
                  }}
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs text-slate-700 focus:bg-white focus:outline-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Đến ngày</label>
                <input
                  type="date"
                  value={filterTo}
                  onChange={(e) => {
                    setFilterTo(e.target.value);
                    setReportPage(0);
                  }}
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs text-slate-700 focus:bg-white focus:outline-blue-500"
                />
              </div>

              {/* Status filter for books / borrows / users */}
              {reportType === 'books' && (
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Trạng thái bản sao</label>
                  <select
                    value={filterStatus}
                    onChange={(e) => {
                      setFilterStatus(e.target.value);
                      setReportPage(0);
                    }}
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs text-slate-700 focus:bg-white focus:outline-blue-500"
                  >
                    <option value="">Tất cả</option>
                    <option value="AVAILABLE">Sẵn sàng (AVAILABLE)</option>
                    <option value="BORROWED">Đang mượn (BORROWED)</option>
                    <option value="LOST">Mất (LOST)</option>
                    <option value="DAMAGED">Hỏng (DAMAGED)</option>
                    <option value="MAINTENANCE">Bảo trì (MAINTENANCE)</option>
                  </select>
                </div>
              )}

              {reportType === 'borrows' && (
                <>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Trạng thái mượn</label>
                    <select
                      value={filterStatus}
                      onChange={(e) => {
                        setFilterStatus(e.target.value);
                        setReportPage(0);
                      }}
                      className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs text-slate-700 focus:bg-white focus:outline-blue-500"
                    >
                      <option value="">Tất cả</option>
                      <option value="BORROWED">Đang mượn</option>
                      <option value="RETURNED">Đã trả</option>
                    </select>
                  </div>
                  <div className="flex items-center h-8 pb-1">
                    <label className="inline-flex items-center gap-2 text-xs font-medium text-slate-700 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={filterOverdue}
                        onChange={(e) => {
                          setFilterOverdue(e.target.checked);
                          setReportPage(0);
                        }}
                        className="rounded border-slate-300 text-blue-900 focus:ring-blue-500"
                      />
                      <span>Chỉ xem quá hạn</span>
                    </label>
                  </div>
                </>
              )}

              {reportType === 'users' && (
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Vai trò</label>
                  <select
                    value={filterRole}
                    onChange={(e) => {
                      setFilterRole(e.target.value);
                      setReportPage(0);
                    }}
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs text-slate-700 focus:bg-white focus:outline-blue-500"
                  >
                    <option value="">Tất cả vai trò</option>
                    <option value="STUDENT">Sinh viên (STUDENT)</option>
                    <option value="LECTURER">Giảng viên (LECTURER)</option>
                    <option value="LIBRARIAN">Thủ thư (LIBRARIAN)</option>
                    <option value="ADMIN">Quản trị viên (ADMIN)</option>
                  </select>
                </div>
              )}

              <div>
                <button
                  type="button"
                  onClick={() => {
                    setFilterFrom('');
                    setFilterTo('');
                    setFilterStatus('');
                    setFilterRole('');
                    setFilterOverdue(false);
                    setReportPage(0);
                  }}
                  className="w-full rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition"
                >
                  Xóa bộ lọc
                </button>
              </div>
            </div>
          </div>

          {/* Report Data Table */}
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-sm font-bold text-slate-900">
                Dữ liệu báo cáo{' '}
                {reportTotalElements > 0 && (
                  <span className="text-xs font-normal text-slate-500">
                    ({reportTotalElements} dòng)
                  </span>
                )}
              </h3>
            </div>

            {loadingReport ? (
              <div className="py-20 text-center text-sm text-slate-500">Đang tải dữ liệu báo cáo...</div>
            ) : reportError ? (
              <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
                {reportError}
              </div>
            ) : (
              <>
                {reportType === 'books' && (
                  <BookReportTable data={booksData} pageOffset={reportPage * 15} />
                )}
                {reportType === 'borrows' && (
                  <BorrowReportTable data={borrowsData} pageOffset={reportPage * 15} />
                )}
                {reportType === 'reading' && (
                  <ReadingReportTable data={readingData} pageOffset={reportPage * 15} />
                )}
                {reportType === 'users' && (
                  <UserReportTable data={usersData} pageOffset={reportPage * 15} />
                )}

                {/* Pagination */}
                <Pagination
                  currentPage={reportPage}
                  totalPages={reportTotalPages}
                  onPageChange={(page) => setReportPage(page)}
                  disabled={loadingReport}
                />
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
