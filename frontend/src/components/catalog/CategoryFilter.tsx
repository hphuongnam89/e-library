import type { CategoryDto } from '../../types/catalog';

interface CategoryFilterProps {
  categories: CategoryDto[];
  selectedCategoryId: number | null;
  onSelectCategory: (id: number | null) => void;
  isLoading?: boolean;
  isLibrarianOrAdmin?: boolean;
  onAddCategory?: () => void;
}

export function CategoryFilter({
  categories,
  selectedCategoryId,
  onSelectCategory,
  isLoading = false,
  isLibrarianOrAdmin = false,
  onAddCategory,
}: CategoryFilterProps) {
  return (
    <aside className="w-full lg:w-64 shrink-0" aria-label="Bộ lọc danh mục">
      <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center justify-between">
            <span>Danh mục</span>
          </h3>
          {isLibrarianOrAdmin && onAddCategory && (
            <button
              type="button"
              onClick={onAddCategory}
              className="text-xs font-semibold text-blue-700 hover:text-blue-900 hover:underline"
            >
              + Thêm
            </button>
          )}
        </div>

        {selectedCategoryId !== null && (
          <div className="mb-2 text-right">
            <button
              type="button"
              onClick={() => onSelectCategory(null)}
              className="text-xs text-blue-700 hover:underline"
            >
              Xóa bộ lọc danh mục
            </button>
          </div>
        )}

        {isLoading ? (
          <div className="space-y-2 py-2">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-6 rounded-md bg-slate-100 animate-pulse" />
            ))}
          </div>
        ) : (
          <nav className="flex flex-row flex-wrap lg:flex-col gap-1">
            <button
              type="button"
              onClick={() => onSelectCategory(null)}
              className={`rounded-lg px-3 py-2 text-left text-sm font-medium transition-colors ${
                selectedCategoryId === null
                  ? 'bg-blue-900 text-white'
                  : 'text-slate-700 hover:bg-slate-100'
              }`}
            >
              Tất cả sách
            </button>
            {categories.map((cat) => (
              <button
                key={cat.id}
                type="button"
                onClick={() => onSelectCategory(cat.id)}
                className={`rounded-lg px-3 py-2 text-left text-sm font-medium transition-colors truncate ${
                  selectedCategoryId === cat.id
                    ? 'bg-blue-900 text-white'
                    : 'text-slate-700 hover:bg-slate-100'
                }`}
                title={cat.name}
              >
                {cat.name}
              </button>
            ))}
          </nav>
        )}
      </div>
    </aside>
  );
}
