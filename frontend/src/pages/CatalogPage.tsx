import { useCallback, useEffect, useState } from 'react';
import type { BookTitleDto, CategoryDto } from '../types/catalog';
import { deleteBookTitle, fetchBookTitles, fetchRootCategories } from '../api/catalog';
import { BookCard } from '../components/catalog/BookCard';
import { BookDetailModal } from '../components/catalog/BookDetailModal';
import { CategoryFilter } from '../components/catalog/CategoryFilter';
import { BookTitleModal } from '../components/catalog/BookTitleModal';
import { BookCopyModal } from '../components/catalog/BookCopyModal';
import { CategoryModal } from '../components/catalog/CategoryModal';
import { Pagination } from '../components/Pagination';
import { useAuth } from '../hooks/useAuth';
import { ApiError } from '../api/client';

export function CatalogPage() {
  const { user, isAuthenticated, isLoading: isAuthLoading, login } = useAuth();
  const isLibrarianOrAdmin =
    isAuthenticated && (user?.role === 'LIBRARIAN' || user?.role === 'ADMIN');

  const [books, setBooks] = useState<BookTitleDto[]>([]);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeQuery, setActiveQuery] = useState('');

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [isLoading, setIsLoading] = useState(false);
  const [isCategoriesLoading, setIsCategoriesLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isUnauthorized, setIsUnauthorized] = useState(false);

  // Modals state
  const [selectedBook, setSelectedBook] = useState<BookTitleDto | null>(null);
  const [isTitleModalOpen, setIsTitleModalOpen] = useState(false);
  const [bookToEdit, setBookToEdit] = useState<BookTitleDto | null>(null);
  const [isCopyModalOpen, setIsCopyModalOpen] = useState(false);
  const [copyBookTarget, setCopyBookTarget] = useState<BookTitleDto | null>(null);
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);

  const loadCategories = useCallback((signal?: AbortSignal) => {
    if (!isAuthenticated) return;
    setIsCategoriesLoading(true);
    fetchRootCategories(signal)
      .then((data) => setCategories(data))
      .catch((err: unknown) => {
        if (err instanceof Error && err.name === 'AbortError') return;
      })
      .finally(() => setIsCategoriesLoading(false));
  }, [isAuthenticated]);

  const loadBooks = useCallback((signal?: AbortSignal) => {
    if (!isAuthenticated) return;
    setIsLoading(true);
    setError(null);
    setIsUnauthorized(false);

    fetchBookTitles({
      query: activeQuery,
      categoryId: selectedCategoryId,
      page: currentPage,
      size: 12,
      signal,
    })
      .then((page) => {
        setBooks(page.content ?? []);
        setTotalPages(page.totalPages ?? 0);
        setTotalElements(page.totalElements ?? 0);
      })
      .catch((err: unknown) => {
        if (err instanceof Error && err.name === 'AbortError') return;
        if (err instanceof ApiError && err.status === 401) {
          setIsUnauthorized(true);
        } else {
          setError(err instanceof Error ? err.message : 'Không thể tải danh sách sách.');
        }
      })
      .finally(() => setIsLoading(false));
  }, [isAuthenticated, activeQuery, selectedCategoryId, currentPage]);

  useEffect(() => {
    const controller = new AbortController();
    loadCategories(controller.signal);
    return () => controller.abort();
  }, [loadCategories]);

  useEffect(() => {
    const controller = new AbortController();
    loadBooks(controller.signal);
    return () => controller.abort();
  }, [loadBooks]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    setActiveQuery(searchQuery);
  };

  const handleClearSearch = () => {
    setSearchQuery('');
    setActiveQuery('');
    setCurrentPage(0);
  };

  const handleSelectCategory = (id: number | null) => {
    setSelectedCategoryId(id);
    setCurrentPage(0);
  };

  const handleDeleteBook = async (book: BookTitleDto) => {
    if (!window.confirm(`Bạn có chắc muốn xóa nhan đề "${book.title}"? Không thể xóa nếu sách còn bản sao.`)) {
      return;
    }
    try {
      await deleteBookTitle(book.id);
      loadBooks();
      if (selectedBook?.id === book.id) setSelectedBook(null);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Không thể xóa nhan đề sách.');
    }
  };

  if (!isAuthLoading && !isAuthenticated) {
    return (
      <section className="max-w-2xl py-16 sm:py-24 mx-auto text-center">
        <div className="rounded-2xl border border-slate-200 bg-white p-8 shadow-xs">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-blue-100 text-2xl text-blue-900">
            📖
          </div>
          <h2 className="mt-4 text-2xl font-bold text-slate-900">Tra cứu danh mục sách E-LIB</h2>
          <p className="mt-3 text-sm leading-6 text-slate-600">
            Hệ thống thư viện yêu cầu bạn đăng nhập bằng tài khoản trường Phú Xuân để tra cứu và mượn sách.
          </p>
          <div className="mt-6">
            <button
              type="button"
              onClick={login}
              className="rounded-lg bg-blue-900 px-6 py-2.5 text-sm font-semibold text-white shadow-xs hover:bg-blue-800 transition-colors"
            >
              Đăng nhập Google để tiếp tục
            </button>
          </div>
        </div>
      </section>
    );
  }

  return (
    <div className="py-8">
      {/* Header & Search */}
      <div className="mb-8">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <p className="text-xs font-bold uppercase tracking-wider text-blue-700">Mục lục tra cứu trực tuyến (OPAC)</p>
            <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
              Tra cứu & Quản lý Danh mục Sách
            </h1>
            <p className="mt-1 text-sm text-slate-600">
              Tìm kiếm theo nhan đề, tác giả, NXB hoặc duyệt theo danh mục chuyên ngành.
            </p>
          </div>

          {isLibrarianOrAdmin && (
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => {
                  setBookToEdit(null);
                  setIsTitleModalOpen(true);
                }}
                className="rounded-xl bg-blue-900 px-4 py-2.5 text-xs font-bold text-white shadow-xs hover:bg-blue-800 transition-colors inline-flex items-center gap-1.5"
              >
                <span>+ Thêm nhan đề sách mới</span>
              </button>
            </div>
          )}
        </div>

        <form onSubmit={handleSearchSubmit} className="mt-6 flex max-w-3xl gap-2">
          <div className="relative flex-1">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Nhập tên sách, tác giả, nhà xuất bản..."
              className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 pr-10 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900 shadow-xs"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={handleClearSearch}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 text-sm"
                aria-label="Xóa từ khóa tìm kiếm"
              >
                ✕
              </button>
            )}
          </div>
          <button
            type="submit"
            className="rounded-xl bg-blue-900 px-6 py-3 text-sm font-semibold text-white shadow-xs hover:bg-blue-800 transition-colors"
          >
            Tìm kiếm
          </button>
        </form>

        {activeQuery && (
          <div className="mt-3 flex items-center gap-2 text-xs text-slate-600">
            <span>Kết quả cho từ khóa: <strong>"{activeQuery}"</strong></span>
            <button
              type="button"
              onClick={handleClearSearch}
              className="text-blue-700 hover:underline"
            >
              (Xóa bộ lọc tìm kiếm)
            </button>
          </div>
        )}
      </div>

      {/* Main Content: Category Sidebar + Books Grid */}
      <div className="flex flex-col lg:flex-row gap-8 items-start">
        <CategoryFilter
          categories={categories}
          selectedCategoryId={selectedCategoryId}
          onSelectCategory={handleSelectCategory}
          isLoading={isCategoriesLoading}
          isLibrarianOrAdmin={isLibrarianOrAdmin}
          onAddCategory={() => setIsCategoryModalOpen(true)}
        />

        <main className="flex-1 w-full">
          {isUnauthorized ? (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-6 text-center">
              <p className="text-sm font-semibold text-amber-800">Phiên đăng nhập đã hết hạn.</p>
              <button
                type="button"
                onClick={login}
                className="mt-3 rounded-lg bg-amber-700 px-4 py-2 text-xs font-semibold text-white hover:bg-amber-800"
              >
                Đăng nhập lại
              </button>
            </div>
          ) : error ? (
            <div className="rounded-xl border border-rose-200 bg-rose-50 p-6 text-center">
              <p className="text-sm text-rose-700">{error}</p>
              <button
                type="button"
                onClick={() => loadBooks()}
                className="mt-3 rounded-lg bg-rose-700 px-4 py-2 text-xs font-semibold text-white hover:bg-rose-800"
              >
                Thử lại
              </button>
            </div>
          ) : isLoading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div
                  key={i}
                  className="h-56 rounded-xl border border-slate-200 bg-white p-5 animate-pulse"
                >
                  <div className="h-4 w-1/3 bg-slate-200 rounded-sm mb-3" />
                  <div className="h-6 w-3/4 bg-slate-200 rounded-sm mb-2" />
                  <div className="h-4 w-1/2 bg-slate-200 rounded-sm mb-4" />
                  <div className="h-8 w-full bg-slate-100 rounded-lg mt-12" />
                </div>
              ))}
            </div>
          ) : books.length === 0 ? (
            <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-xl text-slate-500">
                🔍
              </div>
              <h3 className="mt-3 text-base font-semibold text-slate-900">Không tìm thấy sách phù hợp</h3>
              <p className="mt-1 text-xs text-slate-500">
                Hãy thử đổi từ khóa tìm kiếm hoặc chọn danh mục khác.
              </p>
              {(activeQuery || selectedCategoryId !== null) && (
                <button
                  type="button"
                  onClick={() => {
                    handleClearSearch();
                    handleSelectCategory(null);
                  }}
                  className="mt-4 inline-flex rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                >
                  Đặt lại tất cả bộ lọc
                </button>
              )}
            </div>
          ) : (
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="text-xs text-slate-500">
                  Tìm thấy <strong>{totalElements}</strong> cuốn sách
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
                {books.map((book) => (
                  <BookCard
                    key={book.id}
                    book={book}
                    onSelect={(selected) => setSelectedBook(selected)}
                    onEdit={
                      isLibrarianOrAdmin
                        ? (b) => {
                            setBookToEdit(b);
                            setIsTitleModalOpen(true);
                          }
                        : undefined
                    }
                    onDelete={isLibrarianOrAdmin ? handleDeleteBook : undefined}
                  />
                ))}
              </div>

              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={(page) => setCurrentPage(page)}
              />
            </div>
          )}
        </main>
      </div>

      {/* Book Copies Detail Modal */}
      <BookDetailModal
        book={selectedBook}
        isLibrarianOrAdmin={isLibrarianOrAdmin}
        onClose={() => setSelectedBook(null)}
        onAddCopy={(b) => {
          setCopyBookTarget(b);
          setIsCopyModalOpen(true);
        }}
      />

      {/* Add / Edit Book Title Modal */}
      <BookTitleModal
        isOpen={isTitleModalOpen}
        bookToEdit={bookToEdit}
        categories={categories}
        onClose={() => setIsTitleModalOpen(false)}
        onSuccess={() => loadBooks()}
      />

      {/* Add Book Copy Modal */}
      {copyBookTarget && (
        <BookCopyModal
          isOpen={isCopyModalOpen}
          book={copyBookTarget}
          copyToEdit={null}
          onClose={() => {
            setIsCopyModalOpen(false);
            setCopyBookTarget(null);
          }}
          onSuccess={() => {
            // Re-trigger load by re-setting selectedBook to same reference clone
            if (selectedBook) setSelectedBook({ ...selectedBook });
          }}
        />
      )}

      {/* Add Category Modal */}
      <CategoryModal
        isOpen={isCategoryModalOpen}
        categoryToEdit={null}
        categories={categories}
        onClose={() => setIsCategoryModalOpen(false)}
        onSuccess={() => loadCategories()}
      />
    </div>
  );
}
