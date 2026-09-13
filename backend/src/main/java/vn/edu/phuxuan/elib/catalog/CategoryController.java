package vn.edu.phuxuan.elib.catalog;

import jakarta.validation.Valid;
import java.util.List;
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
import vn.edu.phuxuan.elib.catalog.dto.CategoryDto;
import vn.edu.phuxuan.elib.catalog.dto.CreateCategoryRequest;
import vn.edu.phuxuan.elib.catalog.dto.UpdateCategoryRequest;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CatalogService catalogService;

    public CategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public Page<CategoryDto> getCategories(@RequestParam(required = false) Long parentId, Pageable pageable) {
        return catalogService.getCategories(parentId, pageable);
    }

    @GetMapping("/roots")
    public List<CategoryDto> getRootCategories() {
        return catalogService.getRootCategories();
    }

    @GetMapping("/{id}")
    public CategoryDto getCategoryById(@PathVariable Long id) {
        return catalogService.getCategoryById(id);
    }

    @GetMapping("/{id}/children")
    public List<CategoryDto> getCategoryChildren(@PathVariable Long id) {
        return catalogService.getCategoryChildren(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody CreateCategoryRequest req) {
        return catalogService.createCategory(req);
    }

    @PatchMapping("/{id}")
    public CategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest req) {
        return catalogService.updateCategory(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
    }
}
