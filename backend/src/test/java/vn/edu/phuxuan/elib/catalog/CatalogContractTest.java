package vn.edu.phuxuan.elib.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.phuxuan.elib.catalog.dto.BookCopyDto;
import vn.edu.phuxuan.elib.catalog.dto.BookTitleDto;
import vn.edu.phuxuan.elib.catalog.dto.CategoryDto;
import vn.edu.phuxuan.elib.web.GlobalExceptionHandler;
import vn.edu.phuxuan.elib.web.ProblemResponses;
import vn.edu.phuxuan.elib.web.RequestIdFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogContractTest {

    private MockMvc mvc;

    @Mock
    private CatalogService catalogService;

    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        BookTitleController titleController = new BookTitleController(catalogService);
        BookCopyController copyController = new BookCopyController(catalogService);
        CategoryController categoryController = new CategoryController(catalogService);

        mvc = MockMvcBuilders.standaloneSetup(titleController, copyController, categoryController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler(new ProblemResponses(mapper)))
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    @DisplayName("Contract GET /api/v1/book-titles (OPAC search)")
    void searchBookTitlesContract() throws Exception {
        BookTitleDto title = new BookTitleDto(
                1L, "Lập Trình Web Hiện Đại", "Tác giả X", "NXB Công Nghệ",
                "978-604-0-12345-6", (short) 2026, 10L, "Công nghệ thông tin",
                Instant.now(), Instant.now()
        );
        when(catalogService.getBookTitles(eq("Web"), any(), any()))
                .thenReturn(new PageImpl<>(List.of(title), PageRequest.of(0, 20), 1));

        mvc.perform(get("/api/v1/book-titles?query=Web").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Lập Trình Web Hiện Đại"))
                .andExpect(jsonPath("$.content[0].isbn").value("978-604-0-12345-6"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Contract GET /api/v1/book-copies (tra cứu bản sao)")
    void getBookCopiesContract() throws Exception {
        BookCopyDto copy = new BookCopyDto(
                50L, 1L, "Lập Trình Web", 2L, "Thư viện Cơ sở 1",
                "PXU-00000001", "Kệ A1", BookCopyStatus.AVAILABLE,
                0L, Instant.now(), Instant.now()
        );
        when(catalogService.getBookCopies(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(copy), PageRequest.of(0, 20), 1));

        mvc.perform(get("/api/v1/book-copies").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].barcode").value("PXU-00000001"))
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("Contract GET /api/v1/categories (danh mục)")
    void getCategoriesContract() throws Exception {
        CategoryDto c1 = new CategoryDto(10L, null, null, "Công nghệ thông tin");
        when(catalogService.getCategories(any(), any()))
                .thenReturn(new PageImpl<>(List.of(c1), PageRequest.of(0, 50), 1));

        mvc.perform(get("/api/v1/categories").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].name").value("Công nghệ thông tin"));
    }
}
