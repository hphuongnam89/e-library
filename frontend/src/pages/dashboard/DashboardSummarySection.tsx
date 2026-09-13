import type { DashboardSummaryDto } from '../../types/report';
import { formatDate } from '../../lib/formatters';

interface DashboardSummarySectionProps {
  summary: DashboardSummaryDto | null;
  loading: boolean;
  error: string | null;
}

export function DashboardSummarySection({
  summary,
  loading,
  error,
}: DashboardSummarySectionProps) {
  if (loading) {
    return (
      <div className="py-20 text-center text-sm text-slate-500">
        Đang tổng hợp dữ liệu...
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
        {error}
      </div>
    );
  }

  if (!summary) {
    return null;
  }

  return (
    <>
      {/* KPI Metric Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Books Card */}
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Sách & Bản sao
            </span>
            <span className="text-xl">📚</span>
          </div>
          <div className="mt-3">
            <span className="text-3xl font-bold text-slate-900">
              {summary.books.totalCopies}
            </span>
            <span className="ml-2 text-xs text-slate-500">
              bản ({summary.books.totalTitles} đầu sách)
            </span>
          </div>
          <div className="mt-3 flex items-center justify-between text-xs text-slate-600 border-t border-slate-100 pt-2.5">
            <span>
              🟢 Sẵn sàng: <strong>{summary.books.availableCopies}</strong>
            </span>
            <span>
              🔄 Đang mượn: <strong>{summary.books.borrowedCopies}</strong>
            </span>
          </div>
        </div>

        {/* Circulation Card */}
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Lưu thông mượn trả
            </span>
            <span className="text-xl">🔄</span>
          </div>
          <div className="mt-3">
            <span className="text-3xl font-bold text-blue-900">
              {summary.circulation.activeBorrows}
            </span>
            <span className="ml-2 text-xs text-slate-500">lượt đang mượn</span>
          </div>
          <div className="mt-3 flex items-center justify-between text-xs border-t border-slate-100 pt-2.5">
            <span className="text-red-600 font-semibold">
              🚨 Quá hạn: {summary.circulation.overdueBorrows}
            </span>
            <span className="text-emerald-700">
              Đã trả: {summary.circulation.returnedBorrows}
            </span>
          </div>
        </div>

        {/* Digital Card */}
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Tài liệu số & Đọc
            </span>
            <span className="text-xl">💻</span>
          </div>
          <div className="mt-3">
            <span className="text-3xl font-bold text-indigo-900">
              {summary.digital.totalReadingHours}
            </span>
            <span className="ml-2 text-xs text-slate-500">giờ đọc tích lũy</span>
          </div>
          <div className="mt-3 flex items-center justify-between text-xs text-slate-600 border-t border-slate-100 pt-2.5">
            <span>📄 {summary.digital.totalDocuments} tài liệu</span>
            <span>⏱️ {summary.digital.totalReadingSessions} phiên đọc</span>
          </div>
        </div>

        {/* Users Card */}
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Độc giả hoạt động
            </span>
            <span className="text-xl">👥</span>
          </div>
          <div className="mt-3">
            <span className="text-3xl font-bold text-emerald-900">
              {summary.users.activeUsers}
            </span>
            <span className="ml-2 text-xs text-slate-500">
              / {summary.users.totalUsers} tài khoản
            </span>
          </div>
          <div className="mt-3 flex items-center justify-between text-xs text-slate-600 border-t border-slate-100 pt-2.5">
            <span>
              🎓 Sinh viên: <strong>{summary.users.studentUsers}</strong>
            </span>
            <span>
              👨‍🏫 Giảng viên: <strong>{summary.users.lecturerUsers}</strong>
            </span>
          </div>
        </div>
      </div>

      {/* Two Column Layout: Top Books & Top Docs */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Top Borrowed Books */}
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
          <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider mb-4 flex items-center gap-2">
            <span>🏆 Top 5 Sách được mượn nhiều nhất</span>
          </h3>
          {summary.topBorrowedBooks.length === 0 ? (
            <p className="text-xs text-slate-400 py-6 text-center">
              Chưa có dữ liệu mượn sách
            </p>
          ) : (
            <div className="space-y-3">
              {summary.topBorrowedBooks.map((book, idx) => (
                <div
                  key={book.bookTitleId}
                  className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100"
                >
                  <div className="flex items-center gap-3">
                    <span className="w-6 text-center font-bold text-slate-400 text-sm">
                      #{idx + 1}
                    </span>
                    <div>
                      <p className="text-sm font-semibold text-slate-900 line-clamp-1">
                        {book.title}
                      </p>
                      <p className="text-xs text-slate-500">{book.author}</p>
                    </div>
                  </div>
                  <span className="rounded-full bg-blue-100 px-2.5 py-1 text-xs font-bold text-blue-800 whitespace-nowrap">
                    {book.borrowCount} lượt
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Top Read Documents */}
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
          <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider mb-4 flex items-center gap-2">
            <span>⚡ Top 5 Tài liệu số được đọc nhiều nhất</span>
          </h3>
          {summary.topReadDocuments.length === 0 ? (
            <p className="text-xs text-slate-400 py-6 text-center">
              Chưa có dữ liệu đọc tài liệu
            </p>
          ) : (
            <div className="space-y-3">
              {summary.topReadDocuments.map((doc, idx) => (
                <div
                  key={doc.documentId}
                  className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100"
                >
                  <div className="flex items-center gap-3">
                    <span className="w-6 text-center font-bold text-indigo-400 text-sm">
                      #{idx + 1}
                    </span>
                    <div>
                      <p className="text-sm font-semibold text-slate-900 line-clamp-1">
                        {doc.title}
                      </p>
                      <p className="text-xs text-slate-500">
                        {doc.author || 'Tài liệu trường'} · {doc.sessionCount} phiên
                        đọc
                      </p>
                    </div>
                  </div>
                  <span className="rounded-full bg-indigo-100 px-2.5 py-1 text-xs font-bold text-indigo-800 whitespace-nowrap">
                    {Math.max(1, Math.round(doc.activeSeconds / 60))} phút
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Recent Borrows Table */}
      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
        <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider mb-4">
          Hoạt động lưu thông gần đây
        </h3>
        {summary.recentBorrows.length === 0 ? (
          <p className="text-xs text-slate-400 py-6 text-center">
            Chưa có lượt mượn nào
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-600">
              <thead className="bg-slate-50 text-slate-700 font-semibold border-b border-slate-200">
                <tr>
                  <th className="py-2.5 px-3">Mã SV/CB</th>
                  <th className="py-2.5 px-3">Họ tên</th>
                  <th className="py-2.5 px-3">Tên sách</th>
                  <th className="py-2.5 px-3">Mã vạch</th>
                  <th className="py-2.5 px-3">Ngày mượn</th>
                  <th className="py-2.5 px-3">Hạn trả</th>
                  <th className="py-2.5 px-3">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {summary.recentBorrows.map((b) => (
                  <tr key={b.borrowId} className="hover:bg-slate-50/50">
                    <td className="py-2.5 px-3 font-semibold text-slate-800">
                      {b.studentCode || '—'}
                    </td>
                    <td className="py-2.5 px-3">{b.userName || '—'}</td>
                    <td className="py-2.5 px-3 font-medium text-slate-900 max-w-xs truncate">
                      {b.bookTitle || '—'}
                    </td>
                    <td className="py-2.5 px-3 font-mono text-slate-500">
                      {b.barcode || '—'}
                    </td>
                    <td className="py-2.5 px-3">
                      {formatDate(b.borrowedAt)}
                    </td>
                    <td className="py-2.5 px-3">
                      {formatDate(b.dueAt)}
                    </td>
                    <td className="py-2.5 px-3">
                      {b.status === 'BORROWED' ? (
                        <span className="rounded-full bg-blue-50 px-2 py-0.5 font-semibold text-blue-700">
                          Đang mượn
                        </span>
                      ) : (
                        <span className="rounded-full bg-emerald-50 px-2 py-0.5 font-semibold text-emerald-700">
                          Đã trả
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}
