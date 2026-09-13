import type { BookCopyStatus } from '../types/catalog';
import type { BorrowStatus } from '../types/circulation';

interface BookCopyStatusBadgeProps {
  status: BookCopyStatus;
}

export function BookCopyStatusBadge({ status }: BookCopyStatusBadgeProps) {
  switch (status) {
    case 'AVAILABLE':
      return (
        <span className="inline-flex items-center rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 ring-1 ring-inset ring-emerald-600/20">
          <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-emerald-500" />
          Có sẵn
        </span>
      );
    case 'BORROWED':
      return (
        <span className="inline-flex items-center rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-semibold text-amber-700 ring-1 ring-inset ring-amber-600/20">
          <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-amber-500" />
          Đang mượn
        </span>
      );
    case 'LOST':
      return (
        <span className="inline-flex items-center rounded-full bg-rose-50 px-2.5 py-0.5 text-xs font-semibold text-rose-700 ring-1 ring-inset ring-rose-600/20">
          <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-rose-500" />
          Thất lạc
        </span>
      );
    case 'DAMAGED':
      return (
        <span className="inline-flex items-center rounded-full bg-orange-50 px-2.5 py-0.5 text-xs font-semibold text-orange-700 ring-1 ring-inset ring-orange-600/20">
          <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-orange-500" />
          Hư hỏng
        </span>
      );
    case 'MAINTENANCE':
      return (
        <span className="inline-flex items-center rounded-full bg-slate-50 px-2.5 py-0.5 text-xs font-semibold text-slate-700 ring-1 ring-inset ring-slate-600/20">
          <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-slate-400" />
          Bảo trì
        </span>
      );
    default:
      return (
        <span className="inline-flex items-center rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-600">
          {status}
        </span>
      );
  }
}

interface BorrowStatusBadgeProps {
  status: BorrowStatus;
  overdue?: boolean;
}

export function BorrowStatusBadge({ status, overdue = false }: BorrowStatusBadgeProps) {
  if (status === 'BORROWED') {
    if (overdue) {
      return (
        <span className="inline-flex items-center rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-bold text-red-800 ring-1 ring-inset ring-red-600/20 animate-pulse">
          <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-red-600" />
          Quá hạn
        </span>
      );
    }
    return (
      <span className="inline-flex items-center rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-semibold text-blue-700 ring-1 ring-inset ring-blue-600/20">
        <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-blue-500" />
        Đang mượn
      </span>
    );
  }

  return (
    <span className="inline-flex items-center rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 ring-1 ring-inset ring-emerald-600/20">
      <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-emerald-500" />
      Đã trả
    </span>
  );
}
