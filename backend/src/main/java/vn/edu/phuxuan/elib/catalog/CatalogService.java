package vn.edu.phuxuan.elib.catalog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.catalog.dto.BookCopyDto;
import vn.edu.phuxuan.elib.catalog.dto.BookTitleDto;
import vn.edu.phuxuan.elib.catalog.dto.CategoryDto;
import vn.edu.phuxuan.elib.catalog.dto.CreateBookCopyRequest;
import vn.edu.phuxuan.elib.catalog.dto.CreateBookTitleRequest;
import vn.edu.phuxuan.elib.catalog.dto.CreateCategoryRequest;
import vn.edu.phuxuan.elib.catalog.dto.UpdateBookCopyRequest;
import vn.edu.phuxuan.elib.catalog.dto.UpdateBookTitleRequest;
import vn.edu.phuxuan.elib.catalog.dto.UpdateCategoryRequest;
import vn.edu.phuxuan.elib.organization.Library;
import vn.edu.phuxuan.elib.organization.LibraryRepository;

@Service
@Transactional
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final BookTitleRepository bookTitleRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LibraryRepository libraryRepository;

    public CatalogService(
            CategoryRepository categoryRepository,
            BookTitleRepository bookTitleRepository,
            BookCopyRepository bookCopyRepository,
            LibraryRepository libraryRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.bookTitleRepository = bookTitleRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.libraryRepository = libraryRepository;
    }

    // ==================== CATEGORY ====================

    @Transactional(readOnly = true)
    public Page<CategoryDto> getCategories(Long parentId, Pageable pageable) {
        if (parentId != null) {
            return categoryRepository.findByParentId(parentId, pageable).map(CategoryDto::from);
        }
        return categoryRepository.findAll(pageable).map(CategoryDto::from);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "rootCategories")
    public List<CategoryDto> getRootCategories() {
        return categoryRepository.findByParentIsNull().stream().map(CategoryDto::from).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categoryChildren", key = "#parentId")
    public List<CategoryDto> getCategoryChildren(Long parentId) {
        return categoryRepository.findByParentId(parentId).stream().map(CategoryDto::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(CategoryDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + id));
    }

    @CacheEvict(value = {"rootCategories", "categoryChildren"}, allEntries = true)
    public CategoryDto createCategory(CreateCategoryRequest req) {
        Category parent = null;
        if (req.parentId() != null) {
            parent = categoryRepository.findById(req.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent category not found with id: " + req.parentId()));
            if (categoryRepository.existsByNameIgnoreCaseAndParentId(req.name(), req.parentId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Category with name '" + req.name() + "' already exists under this parent");
            }
        } else {
            if (categoryRepository.existsByNameIgnoreCaseAndParentIsNull(req.name())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Root category with name '" + req.name() + "' already exists");
            }
        }

        Category entity = new Category(parent, req.name());
        return CategoryDto.from(categoryRepository.save(entity));
    }

    @CacheEvict(value = {"rootCategories", "categoryChildren"}, allEntries = true)
    public CategoryDto updateCategory(Long id, UpdateCategoryRequest req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + id));

        if (req.parentId() != null) {
            if (id.equals(req.parentId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(req.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent category not found with id: " + req.parentId()));

            // Cycle detection: traverse ancestors of new parent to ensure 'id' is not reached
            Set<Long> visited = new HashSet<>();
            Category current = parent;
            while (current != null) {
                if (current.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Circular category hierarchy detected");
                }
                visited.add(current.getId());
                current = current.getParent();
            }

            category.setParent(parent);
        }

        if (req.name() != null && !req.name().isBlank()) {
            Long parentId = category.getParent() != null ? category.getParent().getId() : null;
            if (parentId != null) {
                if (categoryRepository.existsByNameIgnoreCaseAndParentId(req.name(), parentId) && !category.getName().equalsIgnoreCase(req.name())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Category with name '" + req.name() + "' already exists under this parent");
                }
            } else {
                if (categoryRepository.existsByNameIgnoreCaseAndParentIsNull(req.name()) && !category.getName().equalsIgnoreCase(req.name())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Root category with name '" + req.name() + "' already exists");
                }
            }
            category.setName(req.name());
        }

        return CategoryDto.from(categoryRepository.save(category));
    }

    @CacheEvict(value = {"rootCategories", "categoryChildren"}, allEntries = true)
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id: " + id);
        }
        if (categoryRepository.existsByParentId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete category because it has subcategories");
        }
        if (bookTitleRepository.existsByCategoryId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete category because book titles are assigned to it");
        }
        categoryRepository.deleteById(id);
    }

    // ==================== BOOK TITLE ====================

    @Transactional(readOnly = true)
    public Page<BookTitleDto> getBookTitles(String query, Long categoryId, Pageable pageable) {
        if (query != null && !query.trim().isEmpty()) {
            return bookTitleRepository.searchFts(query.trim(), categoryId, pageable).map(BookTitleDto::from);
        }
        if (categoryId != null) {
            return bookTitleRepository.findByCategoryId(categoryId, pageable).map(BookTitleDto::from);
        }
        return bookTitleRepository.findAll(pageable).map(BookTitleDto::from);
    }

    @Transactional(readOnly = true)
    public BookTitleDto getBookTitleById(Long id) {
        return bookTitleRepository.findById(id)
                .map(BookTitleDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book title not found with id: " + id));
    }

    public BookTitleDto createBookTitle(CreateBookTitleRequest req) {
        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found with id: " + req.categoryId()));
        }

        BookTitle entity = new BookTitle(
                req.title(),
                req.author(),
                req.publisher(),
                req.isbn(),
                req.publicationYear(),
                category
        );
        return BookTitleDto.from(bookTitleRepository.save(entity));
    }

    public BookTitleDto updateBookTitle(Long id, UpdateBookTitleRequest req) {
        BookTitle bookTitle = bookTitleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book title not found with id: " + id));

        if (req.categoryId() != null) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found with id: " + req.categoryId()));
            bookTitle.setCategory(category);
        }

        if (req.title() != null && !req.title().isBlank()) {
            bookTitle.setTitle(req.title());
        }
        if (req.author() != null) {
            bookTitle.setAuthor(req.author());
        }
        if (req.publisher() != null) {
            bookTitle.setPublisher(req.publisher());
        }
        if (req.isbn() != null) {
            bookTitle.setIsbn(req.isbn());
        }
        if (req.publicationYear() != null) {
            bookTitle.setPublicationYear(req.publicationYear());
        }

        return BookTitleDto.from(bookTitleRepository.save(bookTitle));
    }

    public void deleteBookTitle(Long id) {
        if (!bookTitleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book title not found with id: " + id);
        }
        if (bookCopyRepository.existsByBookTitleId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete book title because book copies exist for it");
        }
        bookTitleRepository.deleteById(id);
    }

    // ==================== BOOK COPY ====================

    @Transactional(readOnly = true)
    public Page<BookCopyDto> getBookCopies(Long bookTitleId, Long libraryId, BookCopyStatus status, Pageable pageable) {
        if (bookTitleId != null) {
            return bookCopyRepository.findByBookTitleId(bookTitleId, pageable).map(BookCopyDto::from);
        }
        if (libraryId != null && status != null) {
            return bookCopyRepository.findByLibraryIdAndStatus(libraryId, status, pageable).map(BookCopyDto::from);
        }
        if (libraryId != null) {
            return bookCopyRepository.findByLibraryId(libraryId, pageable).map(BookCopyDto::from);
        }
        return bookCopyRepository.findAll(pageable).map(BookCopyDto::from);
    }

    @Transactional(readOnly = true)
    public BookCopyDto getBookCopyById(Long id) {
        return bookCopyRepository.findById(id)
                .map(BookCopyDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book copy not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public BookCopyDto getBookCopyByBarcode(String barcode) {
        return bookCopyRepository.findByBarcode(barcode)
                .map(BookCopyDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book copy not found with barcode: " + barcode));
    }

    public BookCopyDto createBookCopy(CreateBookCopyRequest req) {
        BookTitle bookTitle = bookTitleRepository.findById(req.bookTitleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book title not found with id: " + req.bookTitleId()));

        Library library = libraryRepository.findById(req.libraryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Library not found with id: " + req.libraryId()));

        String barcode = req.barcode();
        if (barcode != null && !barcode.isBlank()) {
            if (bookCopyRepository.existsByBarcode(barcode)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Barcode already exists: " + barcode);
            }
        } else {
            Long seq = bookCopyRepository.getNextBarcodeSequence();
            barcode = "PXU-BC-" + seq;
        }

        BookCopyStatus status = req.status() != null ? req.status() : BookCopyStatus.AVAILABLE;
        BookCopy entity = new BookCopy(bookTitle, library, barcode, req.location(), status);
        return BookCopyDto.from(bookCopyRepository.save(entity));
    }

    public BookCopyDto updateBookCopy(Long id, UpdateBookCopyRequest req) {
        BookCopy copy = bookCopyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book copy not found with id: " + id));

        if (req.libraryId() != null) {
            Library library = libraryRepository.findById(req.libraryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Library not found with id: " + req.libraryId()));
            copy.setLibrary(library);
        }
        if (req.location() != null) {
            copy.setLocation(req.location());
        }
        if (req.status() != null) {
            copy.setStatus(req.status());
        }

        return BookCopyDto.from(bookCopyRepository.save(copy));
    }

    public void deleteBookCopy(Long id) {
        BookCopy copy = bookCopyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book copy not found with id: " + id));

        if (copy.getStatus() == BookCopyStatus.BORROWED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a book copy that is currently borrowed");
        }

        bookCopyRepository.deleteById(id);
    }
}
