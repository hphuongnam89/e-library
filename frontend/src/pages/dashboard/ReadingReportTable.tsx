import type { ReadingReportItemDto } from '../../types/report';
import { formatDate } from '../../lib/formatters';

interface ReadingReportTableProps {
  data: ReadingReportItemDto[];
  pageOffset: number;
}

export function ReadingReportTable({ data, pageOffset }: ReadingReportTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-xs text-slate-600">
        <thead className="bg-slate-50 text-slate-700 font-semibold border-b border-slate-200">
          <tr>
            <th className="py-2.5 px-3">STT</th>
            <th className="py-2.5 px-3">Tài liệu số</th>
            <th className="py-2.5 px-3">Mã SV/CB</th>
            <th className="py-2.5 px-3">Độc giả</th>
            <th className="py-2.5 px-3">Khoa / Đơn vị</th>
            <th className="py-2.5 px-3">Thời gian đọc</th>
            <th className="py-2.5 px-3">Số phiên</th>
            <th className="py-2.5 px-3">Lần đọc cuối</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {data.length === 0 ? (
            <tr>
              <td colSpan={8} className="py-8 text-center text-slate-400">
                Không có dữ liệu phù hợp
              </td>
            </tr>
          ) : (
            data.map((item, idx) => (
              <tr key={`${item.documentId}-${item.userId}`} className="hover:bg-slate-50/50">
                <td className="py-2.5 px-3">{pageOffset + idx + 1}</td>
                <td className="py-2.5 px-3 font-medium text-slate-900 max-w-xs truncate">
                  {item.documentTitle}
                </td>
                <td className="py-2.5 px-3 font-semibold text-slate-800">
                  {item.studentCode || '—'}
                </td>
                <td className="py-2.5 px-3">{item.userName || '—'}</td>
                <td className="py-2.5 px-3">{item.departmentName || '—'}</td>
                <td className="py-2.5 px-3 font-mono font-bold text-indigo-700">
                  {Math.max(1, Math.round(item.totalActiveSeconds / 60))} phút
                </td>
                <td className="py-2.5 px-3">{item.sessionCount}</td>
                <td className="py-2.5 px-3">
                  {formatDate(item.lastActiveAt, { includeTime: true })}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
