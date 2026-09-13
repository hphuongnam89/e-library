import { useEffect, useState } from 'react';
import { fetchMyReadingHistory } from '../api/reading';
import type { ReadingHistoryItem } from '../types/reading';
import { formatReadingTime } from '../hooks/useReadingHeartbeat';
import { Pagination } from '../components/Pagination';
import { useAuth } from '../hooks/useAuth';
import { DigitalDocViewerModal } from '../components/digital/DigitalDocViewerModal';
import { fetchDigitalDocumentById } from '../api/digital';
import type { DigitalDocumentDto } from '../types/digital';

export function ReadingHistoryPage() {
  const { user, status: authStatus, login } = useAuth();
  const [historyItems, setHistoryItems] = useState<ReadingHistoryItem[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Selected document for re-reading
  const [selectedDoc, setSelectedDoc] = useState<DigitalDocumentDto | null>(null);
  const [isViewerOpen, setIsViewerOpen] = useState(false);

  useEffect(() => {
    if (authStatus !== 'authenticated') {
      setLoading(false);
      return;
    }

    const controller = new AbortController();
    setLoading(true);
    setError(null);

    fetchMyReadingHistory(currentPage, 10, controller.signal)
      .then((data) => {
        setHistoryItems(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err) => {
        if (!controller.signal.aborted) {
          setError(err.message || 'Không thể tải lịch sử đọc');
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [authStatus, currentPage]);

  const handleOpenDoc = async (docId: number) => {
    try {
      const doc = await fetchDigitalDocumentById(docId);
      setSelectedDoc(doc);
      setIsViewerOpen(true);
    } catch {
      setError('Không thể mở tài liệu đọc này');
    }
  };

  if (authStatus === 'unauthenticated') {
    return (
      <div className="mx-auto max-w-4xl px-4 py-16 text-center">
        <div className="rounded-2xl border border-slate-200 bg-white p-10 shadow-xs">
          <span className="inline-block text-4xl mb-4">📖</span>
          <h2 className="text-xl font-bold text-slate-800">Yêu cầu đăng nhập</h2>
          <p className="mt-2 text-sm text-slate-600">
            Vui lòng đăng nhập bằng tài khoản trường để theo dõi lịch sử đọc và tích lũy thời gian học tập.
          </p>
          <button
            type="button"
            onClick={login}
            className="mt-6 rounded-xl bg-blue-600 px-6 py-2.5 text-sm font-semibold text-white hover:bg-blue-500 shadow-xs transition"
          >
            Đăng nhập Google
          </button>
        </div>
      </div>
    );
  }

  // Calculate aggregates
  const totalActiveSeconds = historyItems.reduce((acc, item) => acc + item.activeSeconds, 0);
  const totalSessions = historyItems.reduce((acc, item) => acc + item.sessionCount, 0);

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      {/* Header Banner */}
      <div className="mb-8 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Lịch sử Đọc Tài liệu Số</h1>
          <p className="mt-1 text-sm text-slate-600">
            Theo dõi thời lượng đọc sách thực tế, số phiên và tiến trình học tập của bạn.
          </p>
        </div>
      </div>

      {/* Summary Stat Cards */}
      <div className="mb-8 grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Thời gian đọc tích lũy
          </div>
          <div className="mt-2 text-2xl font-extrabold text-blue-600 font-mono">
            {formatReadingTime(totalActiveSeconds)}
          </div>
          <div className="mt-1 text-xs text-slate-400">Chỉ tính thời gian hoạt động thực tế</div>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Số tài liệu đã xem
          </div>
          <div className="mt-2 text-2xl font-extrabold text-slate-800 font-mono">
            {totalElements}
          </div>
          <div className="mt-1 text-xs text-slate-400">Giáo trình & tài liệu số</div>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Tổng số phiên đọc
          </div>
          <div className="mt-2 text-2xl font-extrabold text-emerald-600 font-mono">
            {totalSessions}
          </div>
          <div className="mt-1 text-xs text-slate-400">Nhịp tim 15s xác thực server</div>
        </div>
      </div>

      {/* Content State */}
      {loading ? (
        <div className="flex h-64 items-center justify-center rounded-xl border border-slate-200 bg-white p-8">
          <div className="flex flex-col items-center space-y-3">
            <div className="h-8 w-8 animate-spin rounded-full border-3 border-blue-600 border-t-transparent" />
            <p className="text-sm text-slate-500">Đang tải lịch sử đọc...</p>
          </div>
        </div>
      ) : error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 p-6 text-center text-red-700">
          <p className="font-semibold">{error}</p>
        </div>
      ) : historyItems.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-300 bg-white p-12 text-center">
          <span className="inline-block text-4xl mb-3">📚</span>
          <h3 className="text-base font-semibold text-slate-800">Chưa có lịch sử đọc</h3>
          <p className="mt-1 text-sm text-slate-500">
            Hãy khám phá kho tài liệu số để bắt đầu đọc tài liệu học tập.
          </p>
        </div>
      ) : (
        <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-700">
              <thead className="border-b border-slate-200 bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-500">
                <tr>
                  <th scope="col" className="px-6 py-4">Tài liệu</th>
                  <th scope="col" className="px-6 py-4">Thời gian đọc</th>
                  <th scope="col" className="px-6 py-4">Số phiên</th>
                  <th scope="col" className="px-6 py-4">Lần đọc gần nhất</th>
                  <th scope="col" className="px-6 py-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {historyItems.map((item) => (
                  <tr key={item.documentId} className="hover:bg-slate-50/80 transition">
                    <td className="px-6 py-4">
                      <div className="font-semibold text-slate-900">{item.title}</div>
                      {item.publisher && (
                        <div className="text-xs text-slate-500">{item.publisher}</div>
                      )}
                    </td>
                    <td className="px-6 py-4 font-mono font-medium text-blue-600">
                      ⏱️ {formatReadingTime(item.activeSeconds)}
                    </td>
                    <td className="px-6 py-4 font-mono text-slate-600">
                      {item.sessionCount} phiên
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-500">
                      {item.lastActiveAt
                        ? new Date(item.lastActiveAt).toLocaleString('vi-VN')
                        : '—'}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        type="button"
                        onClick={() => handleOpenDoc(item.documentId)}
                        className="rounded-lg bg-blue-50 px-3 py-1.5 text-xs font-semibold text-blue-600 hover:bg-blue-100 transition"
                      >
                        Đọc tiếp
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="p-4 border-t border-slate-200">
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
            />
          </div>
        </div>
      )}

      {/* Embedded Document Viewer Modal */}
      {selectedDoc && (
        <DigitalDocViewerModal
          isOpen={isViewerOpen}
          document={selectedDoc}
          currentUser={user}
          onClose={() => {
            setIsViewerOpen(false);
            setSelectedDoc(null);
            // Refresh history after reading session ends
            fetchMyReadingHistory(currentPage, 10).then((data) => {
              setHistoryItems(data.content);
              setTotalPages(data.totalPages);
              setTotalElements(data.totalElements);
            }).catch(() => {});
          }}
        />
      )}
    </div>
  );
}
