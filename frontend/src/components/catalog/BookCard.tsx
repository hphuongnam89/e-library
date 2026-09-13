import type { BookTitleDto } from '../../types/catalog';

interface BookCardProps {
  book: BookTitleDto;
  onSelect: (book: BookTitleDto) => void;
  onEdit?: (book: BookTitleDto) => void;
  onDelete?: (book: BookTitleDto) => void;
}

export function BookCard({ book, onSelect, onEdit, onDelete }: BookCardProps) {
  return (
    <article className="flex flex-col justify-between rounded-xl border border-slate-200 bg-white p-5 shadow-xs hover:shadow-md transition-shadow">
      <div>
        <div className="flex items-start justify-between gap-2 mb-2">
          {book.categoryName && (
            <span className="inline-block rounded-md bg-blue-50 px-2 py-0.5 text-xs font-semibold text-blue-700">
              {book.categoryName}
            </span>
          )}
          {book.publicationYear && (
            <span className="text-xs text-slate-500">{book.publicationYear}</span>
          )}
        </div>

        <h3 className="text-base font-bold text-slate-900 line-clamp-2 hover:text-blue-900 transition-colors">
          {book.title}
        </h3>

        <div className="mt-2 space-y-1 text-xs text-slate-600">
          <p>
            <span className="font-medium text-slate-500">Tác giả:</span>{' '}
            <span className="font-semibold text-slate-800">{book.author || 'Đang cập nhật'}</span>
          </p>
          <p>
            <span className="font-medium text-slate-500">NXB:</span> {book.publisher || '—'}
          </p>
          {book.isbn && (
            <p className="font-mono text-slate-500">
              <span className="font-sans font-medium">ISBN:</span> {book.isbn}
            </p>
          )}
        </div>
      </div>

      <div className="mt-5 pt-3 border-t border-slate-100 flex flex-col gap-2">
        <button
          type="button"
          onClick={() => onSelect(book)}
          className="inline-flex w-full items-center justify-center rounded-lg bg-slate-100 px-3 py-2 text-xs font-semibold text-blue-900 hover:bg-blue-900 hover:text-white transition-colors"
        >
          Xem tình trạng bản sách →
        </button>

        {(onEdit || onDelete) && (
          <div className="flex items-center justify-end gap-2 pt-1">
            {onEdit && (
              <button
                type="button"
                onClick={() => onEdit(book)}
                className="text-xs font-semibold text-blue-700 hover:text-blue-900 hover:underline"
              >
                Sửa
              </button>
            )}
            {onDelete && (
              <button
                type="button"
                onClick={() => onDelete(book)}
                className="text-xs font-semibold text-rose-600 hover:text-rose-800 hover:underline ml-2"
              >
                Xóa
              </button>
            )}
          </div>
        )}
      </div>
    </article>
  );
}
