interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}

export function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  disabled = false,
}: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <nav
      role="navigation"
      aria-label="Phân trang"
      className="flex items-center justify-between border-t border-slate-200 px-4 py-3 sm:px-6 mt-6"
    >
      <div className="flex flex-1 justify-between sm:hidden">
        <button
          type="button"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={disabled || currentPage === 0}
          className="relative inline-flex items-center rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          Trước
        </button>
        <button
          type="button"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={disabled || currentPage >= totalPages - 1}
          className="relative ml-3 inline-flex items-center rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          Sau
        </button>
      </div>

      <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
        <div>
          <p className="text-sm text-slate-700">
            Trang <span className="font-semibold">{currentPage + 1}</span> /{' '}
            <span className="font-semibold">{totalPages}</span>
          </p>
        </div>
        <div>
          <ul className="isolate inline-flex -space-x-px rounded-md shadow-xs" aria-label="Danh sách trang">
            <li>
              <button
                type="button"
                onClick={() => onPageChange(currentPage - 1)}
                disabled={disabled || currentPage === 0}
                className="relative inline-flex items-center rounded-l-md px-3 py-2 text-slate-500 ring-1 ring-inset ring-slate-300 hover:bg-slate-50 focus:z-20 disabled:opacity-40 disabled:cursor-not-allowed text-sm"
                aria-label="Trang trước"
              >
                ← Trước
              </button>
            </li>

            {Array.from({ length: totalPages }, (_, i) => {
              // Show window around current page
              if (
                totalPages > 7 &&
                i !== 0 &&
                i !== totalPages - 1 &&
                Math.abs(i - currentPage) > 2
              ) {
                if (i === 1 || i === totalPages - 2) {
                  return (
                    <li key={i}>
                      <span className="relative inline-flex items-center px-3 py-2 text-sm text-slate-400">
                        …
                      </span>
                    </li>
                  );
                }
                return null;
              }

              const isCurrent = i === currentPage;
              return (
                <li key={i}>
                  <button
                    type="button"
                    onClick={() => onPageChange(i)}
                    disabled={disabled}
                    aria-current={isCurrent ? 'page' : undefined}
                    className={`relative inline-flex items-center px-4 py-2 text-sm font-semibold focus:z-20 ${
                      isCurrent
                        ? 'z-10 bg-blue-900 text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-900'
                        : 'text-slate-900 ring-1 ring-inset ring-slate-300 hover:bg-slate-50'
                    }`}
                  >
                    {i + 1}
                  </button>
                </li>
              );
            })}

            <li>
              <button
                type="button"
                onClick={() => onPageChange(currentPage + 1)}
                disabled={disabled || currentPage >= totalPages - 1}
                className="relative inline-flex items-center rounded-r-md px-3 py-2 text-slate-500 ring-1 ring-inset ring-slate-300 hover:bg-slate-50 focus:z-20 disabled:opacity-40 disabled:cursor-not-allowed text-sm"
                aria-label="Trang sau"
              >
                Sau →
              </button>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  );
}
