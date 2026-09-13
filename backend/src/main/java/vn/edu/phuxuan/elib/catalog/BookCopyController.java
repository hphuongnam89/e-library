package vn.edu.phuxuan.elib.catalog;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.phuxuan.elib.catalog.dto.BookCopyDto;
import vn.edu.phuxuan.elib.catalog.dto.CreateBookCopyRequest;
import vn.edu.phuxuan.elib.catalog.dto.UpdateBookCopyRequest;

@RestController
@RequestMapping("/api/v1/book-copies")
public class BookCopyController {

    private final CatalogService catalogService;

    public BookCopyController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public Page<BookCopyDto> getBookCopies(
            @RequestParam(required = false) Long bookTitleId,
            @RequestParam(required = false) Long libraryId,
            @RequestParam(required = false) BookCopyStatus status,
            Pageable pageable
    ) {
        return catalogService.getBookCopies(bookTitleId, libraryId, status, pageable);
    }

    @GetMapping("/{id}")
    public BookCopyDto getBookCopyById(@PathVariable Long id) {
        return catalogService.getBookCopyById(id);
    }

    @GetMapping("/barcode/{barcode}")
    public BookCopyDto getBookCopyByBarcode(@PathVariable String barcode) {
        return catalogService.getBookCopyByBarcode(barcode);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookCopyDto createBookCopy(@Valid @RequestBody CreateBookCopyRequest req) {
        return catalogService.createBookCopy(req);
    }

    @PatchMapping("/{id}")
    public BookCopyDto updateBookCopy(@PathVariable Long id, @Valid @RequestBody UpdateBookCopyRequest req) {
        return catalogService.updateBookCopy(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBookCopy(@PathVariable Long id) {
        catalogService.deleteBookCopy(id);
    }
}
