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
import vn.edu.phuxuan.elib.catalog.dto.BookTitleDto;
import vn.edu.phuxuan.elib.catalog.dto.CreateBookTitleRequest;
import vn.edu.phuxuan.elib.catalog.dto.UpdateBookTitleRequest;

@RestController
@RequestMapping("/api/v1/book-titles")
public class BookTitleController {

    private final CatalogService catalogService;

    public BookTitleController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public Page<BookTitleDto> getBookTitles(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            Pageable pageable
    ) {
        return catalogService.getBookTitles(query, categoryId, pageable);
    }

    @GetMapping("/{id}")
    public BookTitleDto getBookTitleById(@PathVariable Long id) {
        return catalogService.getBookTitleById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookTitleDto createBookTitle(@Valid @RequestBody CreateBookTitleRequest req) {
        return catalogService.createBookTitle(req);
    }

    @PatchMapping("/{id}")
    public BookTitleDto updateBookTitle(@PathVariable Long id, @Valid @RequestBody UpdateBookTitleRequest req) {
        return catalogService.updateBookTitle(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBookTitle(@PathVariable Long id) {
        catalogService.deleteBookTitle(id);
    }
}
