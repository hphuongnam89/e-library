package vn.edu.phuxuan.elib;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import vn.edu.phuxuan.elib.catalog.BookCopy;
import vn.edu.phuxuan.elib.catalog.BookCopyRepository;
import vn.edu.phuxuan.elib.catalog.BookCopyStatus;
import vn.edu.phuxuan.elib.catalog.BookTitle;
import vn.edu.phuxuan.elib.catalog.BookTitleRepository;
import vn.edu.phuxuan.elib.catalog.Category;
import vn.edu.phuxuan.elib.catalog.CategoryRepository;
import vn.edu.phuxuan.elib.catalog.dto.CreateBookCopyRequest;
import vn.edu.phuxuan.elib.catalog.dto.CreateBookTitleRequest;
import vn.edu.phuxuan.elib.catalog.dto.CreateCategoryRequest;
import vn.edu.phuxuan.elib.catalog.dto.UpdateBookCopyRequest;
import vn.edu.phuxuan.elib.catalog.dto.UpdateBookTitleRequest;
import vn.edu.phuxuan.elib.catalog.dto.UpdateCategoryRequest;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.organization.Campus;
import vn.edu.phuxuan.elib.organization.CampusRepository;
import vn.edu.phuxuan.elib.organization.DepartmentRepository;
import vn.edu.phuxuan.elib.organization.Institution;
import vn.edu.phuxuan.elib.organization.InstitutionRepository;
import vn.edu.phuxuan.elib.organization.Library;
import vn.edu.phuxuan.elib.organization.LibraryRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class CatalogIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "catalog-test-redis");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry props) {
        props.add("spring.datasource.url", postgres::getJdbcUrl);
        props.add("spring.datasource.username", postgres::getUsername);
        props.add("spring.datasource.password", postgres::getPassword);
        props.add("spring.data.redis.host", redis::getHost);
        props.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        props.add("spring.data.redis.password", () -> "catalog-test-redis");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    BookTitleRepository bookTitleRepository;

    @Autowired
    BookCopyRepository bookCopyRepository;

    @Autowired
    LibraryRepository libraryRepository;

    @Autowired
    CampusRepository campusRepository;

    @Autowired
    InstitutionRepository institutionRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        bookCopyRepository.deleteAll();
        bookTitleRepository.deleteAll();
        // Break parent links before deleting categories to avoid self-referencing FK constraints
        categoryRepository.findAll().forEach(cat -> {
            cat.setParent(null);
            categoryRepository.save(cat);
        });
        categoryRepository.deleteAll();
        appUserRepository.deleteAll();
        departmentRepository.deleteAll();
        libraryRepository.deleteAll();
        campusRepository.deleteAll();
        institutionRepository.deleteAll();
    }

    // ==========================================
    // 1. Security & RBAC Tests
    // ==========================================

    @Test
    void unauthenticatedAccessToCatalogEndpointsIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/book-titles"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/book-copies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotPerformWriteOperations() throws Exception {
        CreateCategoryRequest catReq = new CreateCategoryRequest(null, "Test Category");
        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isForbidden());

        CreateBookTitleRequest titleReq = new CreateBookTitleRequest("Test Book", "Author", "Pub", "123", (short) 2024, null);
        mockMvc.perform(post("/api/v1/book-titles")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(titleReq)))
                .andExpect(status().isForbidden());

        CreateBookCopyRequest copyReq = new CreateBookCopyRequest(1L, 1L, "PXU-001", "Rack 1", BookCopyStatus.AVAILABLE);
        mockMvc.perform(post("/api/v1/book-copies")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(copyReq)))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 2. Category Hierarchy & Cycle Prevention
    // ==========================================

    @Test
    void categoryHierarchyLifecycleAndCycleDetection() throws Exception {
        // 1. Librarian creates root category
        CreateCategoryRequest rootReq = new CreateCategoryRequest(null, "Công nghệ Thông tin");
        String rootJson = mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rootReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Công nghệ Thông tin"))
                .andExpect(jsonPath("$.parentId").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        Long rootId = objectMapper.readTree(rootJson).get("id").asLong();

        // 2. Librarian creates child category
        CreateCategoryRequest childReq = new CreateCategoryRequest(rootId, "Khoa học Dữ liệu");
        String childJson = mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Khoa học Dữ liệu"))
                .andExpect(jsonPath("$.parentId").value(rootId))
                .andReturn().getResponse().getContentAsString();

        Long childId = objectMapper.readTree(childJson).get("id").asLong();

        // 3. Librarian creates grandchild category
        CreateCategoryRequest grandChildReq = new CreateCategoryRequest(childId, "Học sâu & Trí tuệ nhân tạo");
        String grandChildJson = mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grandChildReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(childId))
                .andReturn().getResponse().getContentAsString();

        Long grandChildId = objectMapper.readTree(grandChildJson).get("id").asLong();

        // 4. Roots and Children querying
        mockMvc.perform(get("/api/v1/categories/roots")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Công nghệ Thông tin"));

        mockMvc.perform(get("/api/v1/categories/" + rootId + "/children")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Khoa học Dữ liệu"));

        // 5. Circular hierarchy prevention:
        // Case A: Cannot set category to be its own parent
        mockMvc.perform(patch("/api/v1/categories/" + rootId)
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCategoryRequest(rootId, null))))
                .andExpect(status().isBadRequest());

        // Case B: Cannot set root category parent to its descendant (rootId -> childId -> grandChildId)
        mockMvc.perform(patch("/api/v1/categories/" + rootId)
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCategoryRequest(grandChildId, null))))
                .andExpect(status().isBadRequest());

        // 6. Duplicate category name prevention under same parent
        CreateCategoryRequest dupChildReq = new CreateCategoryRequest(rootId, "Khoa học Dữ liệu");
        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupChildReq)))
                .andExpect(status().isConflict());
    }

    // ==========================================
    // 3. Book Title & PostgreSQL 17 FTS Search
    // ==========================================

    @Test
    void bookTitleCrudAndFullTextSearch() throws Exception {
        Category cat = categoryRepository.save(new Category(null, "Kỹ thuật phần mềm"));

        // Create Book Title 1
        CreateBookTitleRequest title1Req = new CreateBookTitleRequest(
                "Kiến trúc phần mềm Clean Architecture",
                "Robert C. Martin",
                "NXB Khoa Học Kỹ Thuật",
                "978-0134494166",
                (short) 2017,
                cat.getId()
        );
        String t1Json = mockMvc.perform(post("/api/v1/book-titles")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(title1Req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Kiến trúc phần mềm Clean Architecture"))
                .andExpect(jsonPath("$.categoryName").value("Kỹ thuật phần mềm"))
                .andReturn().getResponse().getContentAsString();
        Long t1Id = objectMapper.readTree(t1Json).get("id").asLong();

        // Create Book Title 2
        CreateBookTitleRequest title2Req = new CreateBookTitleRequest(
                "Thiết kế Cơ sở dữ liệu Thực hành",
                "Nguyễn Văn Bình",
                "NXB Giáo Dục",
                "978-6040123456",
                (short) 2022,
                cat.getId()
        );
        mockMvc.perform(post("/api/v1/book-titles")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(title2Req)))
                .andExpect(status().isCreated());

        // Test FTS Search by Title Keyword
        mockMvc.perform(get("/api/v1/book-titles")
                        .param("query", "Architecture")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(t1Id))
                .andExpect(jsonPath("$.content[0].title").value("Kiến trúc phần mềm Clean Architecture"));

        // Test FTS Search by Author Keyword
        mockMvc.perform(get("/api/v1/book-titles")
                        .param("query", "Martin")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].author").value("Robert C. Martin"));

        // Test FTS Search by Publisher Keyword
        mockMvc.perform(get("/api/v1/book-titles")
                        .param("query", "Giáo Dục")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Thiết kế Cơ sở dữ liệu Thực hành"));

        // Test Update Book Title
        mockMvc.perform(patch("/api/v1/book-titles/" + t1Id)
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateBookTitleRequest(
                                "Clean Architecture - Tiếng Việt", null, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Architecture - Tiếng Việt"));
    }

    // ==========================================
    // 4. Book Copy, Barcode Sequence & Barcode Scan
    // ==========================================

    @Test
    void bookCopySequenceGenerationAndBarcodeScanLookup() throws Exception {
        Institution inst = institutionRepository.save(new Institution("Đại học Phú Xuân"));
        Campus campus = campusRepository.save(new Campus(inst, "Cơ sở 1"));
        Library library = libraryRepository.save(new Library(campus, "Thư viện Cơ sở 1", "Tầng 2 Nhà A"));
        Category cat = categoryRepository.save(new Category(null, "Khoa học Máy tính"));
        BookTitle title = bookTitleRepository.save(new BookTitle("Hệ điều hành hiện đại", "Andrew S. Tanenbaum", "NXB KHKT", "978-0133591620", (short) 2015, cat));

        // 1. Create copy with NULL barcode -> auto-generates via PostgreSQL sequence
        CreateBookCopyRequest autoCopyReq1 = new CreateBookCopyRequest(title.getId(), library.getId(), null, "Kệ A-01", BookCopyStatus.AVAILABLE);
        String copy1Json = mockMvc.perform(post("/api/v1/book-copies")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(autoCopyReq1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barcode").value(org.hamcrest.Matchers.startsWith("PXU-BC-")))
                .andReturn().getResponse().getContentAsString();

        String barcode1 = objectMapper.readTree(copy1Json).get("barcode").asText();

        // 2. Create second copy with NULL barcode -> sequence increments
        CreateBookCopyRequest autoCopyReq2 = new CreateBookCopyRequest(title.getId(), library.getId(), null, "Kệ A-02", BookCopyStatus.AVAILABLE);
        String copy2Json = mockMvc.perform(post("/api/v1/book-copies")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(autoCopyReq2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barcode").value(org.hamcrest.Matchers.startsWith("PXU-BC-")))
                .andReturn().getResponse().getContentAsString();

        String barcode2 = objectMapper.readTree(copy2Json).get("barcode").asText();
        assertThat(barcode1).isNotEqualTo(barcode2);

        // 3. Create copy with custom barcode
        String customBarcode = "PXU-CUSTOM-LIB1-9999";
        CreateBookCopyRequest customCopyReq = new CreateBookCopyRequest(title.getId(), library.getId(), customBarcode, "Kệ B-05", BookCopyStatus.AVAILABLE);
        mockMvc.perform(post("/api/v1/book-copies")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customCopyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barcode").value(customBarcode));

        // 4. Duplicate barcode throws 409 Conflict
        mockMvc.perform(post("/api/v1/book-copies")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customCopyReq)))
                .andExpect(status().isConflict());

        // 5. Barcode Scan endpoint (USB HID Barcode scanner emulation)
        mockMvc.perform(get("/api/v1/book-copies/barcode/" + customBarcode)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value(customBarcode))
                .andExpect(jsonPath("$.bookTitle").value("Hệ điều hành hiện đại"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.location").value("Kệ B-05"));

        // 6. Barcode scan for non-existing barcode throws 404
        mockMvc.perform(get("/api/v1/book-copies/barcode/NON_EXISTING_BARCODE")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // 5. Referential Integrity & Deletion Rules
    // ==========================================

    @Test
    void referentialIntegrityEnforcedOnDeletion() throws Exception {
        Institution inst = institutionRepository.save(new Institution("PXU Test"));
        Campus campus = campusRepository.save(new Campus(inst, "Campus Test"));
        Library library = libraryRepository.save(new Library(campus, "Lib Test", "Address"));

        Category parentCat = categoryRepository.save(new Category(null, "Khoa học"));
        Category childCat = categoryRepository.save(new Category(parentCat, "Vật lý"));
        BookTitle title = bookTitleRepository.save(new BookTitle("Vật lý Đại cương", "Lê Văn C", "NXB GD", "978-0001", (short) 2020, childCat));
        BookCopy copy = bookCopyRepository.save(new BookCopy(title, library, "BC-TEST-001", "Kệ 1", BookCopyStatus.BORROWED));

        // 1. Cannot delete copy while its status is BORROWED -> 409
        mockMvc.perform(delete("/api/v1/book-copies/" + copy.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict());

        // 2. Cannot delete title while copies exist -> 409
        mockMvc.perform(delete("/api/v1/book-titles/" + title.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict());

        // 3. Cannot delete child category while titles exist -> 409
        mockMvc.perform(delete("/api/v1/categories/" + childCat.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict());

        // 4. Cannot delete parent category while children exist -> 409
        mockMvc.perform(delete("/api/v1/categories/" + parentCat.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict());

        // 5. Update copy to AVAILABLE -> now deletion succeeds in cascade order
        mockMvc.perform(patch("/api/v1/book-copies/" + copy.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateBookCopyRequest(null, null, BookCopyStatus.AVAILABLE))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/book-copies/" + copy.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/book-titles/" + title.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/categories/" + childCat.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/categories/" + parentCat.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        assertThat(bookCopyRepository.count()).isZero();
        assertThat(bookTitleRepository.count()).isZero();
        assertThat(categoryRepository.count()).isZero();
    }
}
