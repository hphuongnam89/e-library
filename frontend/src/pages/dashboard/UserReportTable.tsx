import type { UserReportItemDto } from '../../types/report';
import { formatDate } from '../../lib/formatters';

interface UserReportTableProps {
  data: UserReportItemDto[];
  pageOffset: number;
}

export function UserReportTable({ data, pageOffset }: UserReportTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-xs text-slate-600">
        <thead className="bg-slate-50 text-slate-700 font-semibold border-b border-slate-200">
          <tr>
            <th className="py-2.5 px-3">STT</th>
            <th className="py-2.5 px-3">Mã SV/CB</th>
            <th className="py-2.5 px-3">Họ và tên</th>
            <th className="py-2.5 px-3">Email</th>
            <th className="py-2.5 px-3">Vai trò</th>
            <th className="py-2.5 px-3">Trạng thái</th>
            <th className="py-2.5 px-3">Khoa / Đơn vị</th>
            <th className="py-2.5 px-3">Sách đang mượn</th>
            <th className="py-2.5 px-3">Ngày tạo</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {data.length === 0 ? (
            <tr>
              <td colSpan={9} className="py-8 text-center text-slate-400">
                Không có dữ liệu phù hợp
              </td>
            </tr>
          ) : (
            data.map((item, idx) => (
              <tr key={item.userId} className="hover:bg-slate-50/50">
                <td className="py-2.5 px-3">{pageOffset + idx + 1}</td>
                <td className="py-2.5 px-3 font-semibold text-slate-800">
                  {item.studentCode || '—'}
                </td>
                <td className="py-2.5 px-3 font-medium text-slate-900">
                  {item.fullName}
                </td>
                <td className="py-2.5 px-3 font-mono text-slate-500">
                  {item.email}
                </td>
                <td className="py-2.5 px-3">
                  <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-semibold text-slate-700">
                    {item.role}
                  </span>
                </td>
                <td className="py-2.5 px-3">
                  <span
                    className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                      item.status === 'ACTIVE'
                        ? 'bg-emerald-50 text-emerald-700'
                        : 'bg-red-50 text-red-700'
                    }`}
                  >
                    {item.status}
                  </span>
                </td>
                <td className="py-2.5 px-3">{item.departmentName || '—'}</td>
                <td className="py-2.5 px-3 font-bold text-blue-900">
                  {item.activeBorrowsCount}
                </td>
                <td className="py-2.5 px-3">{formatDate(item.createdAt)}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
