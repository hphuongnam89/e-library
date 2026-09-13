package vn.edu.phuxuan.elib.catalog;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import vn.edu.phuxuan.elib.catalog.dto.CategoryDto;
import vn.edu.phuxuan.elib.organization.LibraryRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BookTitleRepository bookTitleRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private LibraryRepository libraryRepository;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(
                categoryRepository,
                bookTitleRepository,
                bookCopyRepository,
                libraryRepository
        );
    }

    @Test
    @DisplayName("getCategories with parentId uses DB-level Pageable query")
    void getCategories_withParentId_delegatesToRepositoryWithPageable() {
        Long parentId = 5L;
        Pageable pageable = PageRequest.of(0, 10);
        Category c1 = new Category(null, "Khoa học máy tính");
        c1.setId(10L);

        Page<Category> repoPage = new PageImpl<>(List.of(c1), pageable, 1);
        when(categoryRepository.findByParentId(parentId, pageable)).thenReturn(repoPage);

        Page<CategoryDto> result = catalogService.getCategories(parentId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Khoa học máy tính", result.getContent().get(0).name());
        verify(categoryRepository).findByParentId(parentId, pageable);
    }

    @Test
    @DisplayName("getCategories without parentId uses findAll(pageable)")
    void getCategories_withoutParentId_delegatesToFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Category c1 = new Category(null, "Tổng quan");
        c1.setId(1L);

        Page<Category> repoPage = new PageImpl<>(List.of(c1), pageable, 1);
        when(categoryRepository.findAll(pageable)).thenReturn(repoPage);

        Page<CategoryDto> result = catalogService.getCategories(null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Tổng quan", result.getContent().get(0).name());
        verify(categoryRepository).findAll(pageable);
    }
}
