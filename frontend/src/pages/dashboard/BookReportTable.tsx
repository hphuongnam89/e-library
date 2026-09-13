import type { BookReportItemDto } from '../../types/report';

interface BookReportTableProps {
  data: BookReportItemDto[];
  pageOffset: number;
}

export function BookReportTable({ data, pageOffset }: BookReportTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-xs text-slate-600">
        <thead className="bg-slate-50 text-slate-700 font-semibold border-b border-slate-200">
          <tr>
            <th className="py-2.5 px-3">STT</th>
            <th className="py-2.5 px-3">Mã vạch</th>
            <th className="py-2.5 px-3">Tên sách</th>
            <th className="py-2.5 px-3">Tác giả</th>
            <th className="py-2.5 px-3">ISBN</th>
            <th className="py-2.5 px-3">Thể loại</th>
            <th className="py-2.5 px-3">Trạng thái</th>
            <th className="py-2.5 px-3">Thư viện</th>
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
              <tr key={item.copyId} className="hover:bg-slate-50/50">
                <td className="py-2.5 px-3">{pageOffset + idx + 1}</td>
                <td className="py-2.5 px-3 font-mono font-semibold text-slate-800">
                  {item.barcode}
                </td>
                <td className="py-2.5 px-3 font-medium text-slate-900 max-w-xs truncate">
                  {item.title}
                </td>
                <td className="py-2.5 px-3">{item.author}</td>
                <td className="py-2.5 px-3 font-mono">{item.isbn}</td>
                <td className="py-2.5 px-3">{item.categoryName || '—'}</td>
                <td className="py-2.5 px-3">
                  <span
                    className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                      item.status === 'AVAILABLE'
                        ? 'bg-emerald-50 text-emerald-700'
                        : item.status === 'BORROWED'
                        ? 'bg-blue-50 text-blue-700'
                        : 'bg-slate-100 text-slate-700'
                    }`}
                  >
                    {item.status}
                  </span>
                </td>
                <td className="py-2.5 px-3">{item.libraryName || '—'}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
