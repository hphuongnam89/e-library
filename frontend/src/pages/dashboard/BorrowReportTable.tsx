import type { BorrowReportItemDto } from '../../types/report';
import { formatDate, formatCurrency } from '../../lib/formatters';

interface BorrowReportTableProps {
  data: BorrowReportItemDto[];
  pageOffset: number;
}

export function BorrowReportTable({ data, pageOffset }: BorrowReportTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-xs text-slate-600">
        <thead className="bg-slate-50 text-slate-700 font-semibold border-b border-slate-200">
          <tr>
            <th className="py-2.5 px-3">STT</th>
            <th className="py-2.5 px-3">Mã SV/CB</th>
            <th className="py-2.5 px-3">Độc giả</th>
            <th className="py-2.5 px-3">Khoa / Đơn vị</th>
            <th className="py-2.5 px-3">Tên sách</th>
            <th className="py-2.5 px-3">Mã vạch</th>
            <th className="py-2.5 px-3">Ngày mượn</th>
            <th className="py-2.5 px-3">Hạn trả</th>
            <th className="py-2.5 px-3">Trạng thái</th>
            <th className="py-2.5 px-3">Tiền phạt</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {data.length === 0 ? (
            <tr>
              <td colSpan={10} className="py-8 text-center text-slate-400">
                Không có dữ liệu phù hợp
              </td>
            </tr>
          ) : (
            data.map((item, idx) => (
              <tr key={item.borrowId} className="hover:bg-slate-50/50">
                <td className="py-2.5 px-3">{pageOffset + idx + 1}</td>
                <td className="py-2.5 px-3 font-semibold text-slate-800">
                  {item.studentCode || '—'}
                </td>
                <td className="py-2.5 px-3">{item.userName || '—'}</td>
                <td className="py-2.5 px-3">{item.departmentName || '—'}</td>
                <td className="py-2.5 px-3 font-medium text-slate-900 max-w-xs truncate">
                  {item.bookTitle}
                </td>
                <td className="py-2.5 px-3 font-mono">{item.barcode}</td>
                <td className="py-2.5 px-3">{formatDate(item.borrowedAt)}</td>
                <td className="py-2.5 px-3">{formatDate(item.dueAt)}</td>
                <td className="py-2.5 px-3">
                  <span
                    className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                      item.status === 'BORROWED'
                        ? 'bg-blue-50 text-blue-700'
                        : 'bg-emerald-50 text-emerald-700'
                    }`}
                  >
                    {item.status === 'BORROWED' ? 'Đang mượn' : 'Đã trả'}
                  </span>
                </td>
                <td className="py-2.5 px-3 font-mono font-semibold text-red-600">
                  {formatCurrency(item.fineAmount)}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
