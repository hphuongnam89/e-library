package vn.edu.phuxuan.elib;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
import vn.edu.phuxuan.elib.circulation.Borrow;
import vn.edu.phuxuan.elib.circulation.BorrowRepository;
import vn.edu.phuxuan.elib.circulation.BorrowStatus;
import vn.edu.phuxuan.elib.circulation.BorrowingPolicy;
import vn.edu.phuxuan.elib.circulation.BorrowingPolicyRepository;
import vn.edu.phuxuan.elib.circulation.dto.CheckoutRequest;
import vn.edu.phuxuan.elib.circulation.dto.CreateBorrowingPolicyRequest;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
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
class CirculationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "circ-test-redis");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry props) {
        props.add("spring.datasource.url", postgres::getJdbcUrl);
        props.add("spring.datasource.username", postgres::getUsername);
        props.add("spring.datasource.password", postgres::getPassword);
        props.add("spring.data.redis.host", redis::getHost);
        props.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        props.add("spring.data.redis.password", () -> "circ-test-redis");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    BorrowRepository borrowRepository;

    @Autowired
    BorrowingPolicyRepository borrowingPolicyRepository;

    @Autowired
    BookCopyRepository bookCopyRepository;

    @Autowired
    BookTitleRepository bookTitleRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    LibraryRepository libraryRepository;

    @Autowired
    CampusRepository campusRepository;

    @Autowired
    InstitutionRepository institutionRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        borrowRepository.deleteAll();
        borrowingPolicyRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookTitleRepository.deleteAll();
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
    void unauthenticatedAccessToCirculationEndpointsIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/borrows").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/borrows"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me/borrows"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotPerformLibrarianCirculationOperations() throws Exception {
        CheckoutRequest req = new CheckoutRequest("SV2024001", List.of("PXU-BC-100001"));
        mockMvc.perform(post("/api/v1/borrows")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/borrows")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/borrows/1/return")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/borrows/1/fine-payment")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 2. Policy Creation & Batch Checkout Happy Path
    // ==========================================

    @Test
    void fullCheckoutAndReturnHappyPathWithZeroFines() throws Exception {
        Institution inst = institutionRepository.save(new Institution("Đại học Phú Xuân"));
        Campus campus = campusRepository.save(new Campus(inst, "Cơ sở 1"));
        Library library = libraryRepository.save(new Library(campus, "Thư viện Trung tâm", "Tầng 1"));
        Category category = categoryRepository.save(new Category(null, "CNTT"));
        BookTitle title = bookTitleRepository.save(new BookTitle("Java Concurrency in Practice", "Brian Goetz", "Addison-Wesley", "978-0321349606", (short) 2006, category));
        BookCopy copy1 = bookCopyRepository.save(new BookCopy(title, library, "PXU-BC-001", "Kệ A1", BookCopyStatus.AVAILABLE));
        BookCopy copy2 = bookCopyRepository.save(new BookCopy(title, library, "PXU-BC-002", "Kệ A2", BookCopyStatus.AVAILABLE));

        AppUser student = new AppUser("sub-student-1", "student1@phuxuan.edu.vn", "Nguyễn Văn Sinh Viên", UserRole.STUDENT);
        student.setStudentCode("SV2024001");
        student = appUserRepository.save(student);

        // 1. Admin creates borrowing policy for the library
        CreateBorrowingPolicyRequest policyReq = new CreateBorrowingPolicyRequest(
                14,
                new BigDecimal("5000.00"),
                5,
                Instant.now().minus(1, ChronoUnit.HOURS)
        );
        mockMvc.perform(post("/api/v1/libraries/" + library.getId() + "/borrowing-policies")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(policyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.loanDays").value(14))
                .andExpect(jsonPath("$.dailyFine").value(5000.00));

        // 2. Librarian checks out both copies for the student in a batch transaction
        CheckoutRequest checkoutReq = new CheckoutRequest("SV2024001", List.of("PXU-BC-001", "PXU-BC-002"));
        String checkoutJson = mockMvc.perform(post("/api/v1/borrows")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].studentCode").value("SV2024001"))
                .andExpect(jsonPath("$.items[0].status").value("BORROWED"))
                .andExpect(jsonPath("$.items[0].dailyFine").value(5000.00))
                .andExpect(jsonPath("$.items[0].fineAmount").value(0))
                .andReturn().getResponse().getContentAsString();

        Long borrow1Id = objectMapper.readTree(checkoutJson).get("items").get(0).get("id").asLong();

        // 3. Verify copy status updated to BORROWED
        assertThat(bookCopyRepository.findById(copy1.getId()).orElseThrow().getStatus()).isEqualTo(BookCopyStatus.BORROWED);
        assertThat(bookCopyRepository.findById(copy2.getId()).orElseThrow().getStatus()).isEqualTo(BookCopyStatus.BORROWED);

        // 4. Student can view their active borrows via /api/v1/me/borrows
        mockMvc.perform(get("/api/v1/me/borrows")
                        .with(oidcLogin().idToken(t -> t.subject("sub-student-1").claim("email", "student1@phuxuan.edu.vn"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].studentCode").value("SV2024001"));

        // 5. Librarian returns copy 1 within due date -> fine is 0
        mockMvc.perform(post("/api/v1/borrows/" + borrow1Id + "/return")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.fineAmount").value(0))
                .andExpect(jsonPath("$.returnedAt").isNotEmpty());

        // 6. Verify copy 1 status returned to AVAILABLE
        assertThat(bookCopyRepository.findById(copy1.getId()).orElseThrow().getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);

        // 7. Idempotent return test: returning copy 1 again returns current record unchanged
        mockMvc.perform(post("/api/v1/borrows/" + borrow1Id + "/return")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.fineAmount").value(0));
    }

    // ==========================================
    // 3. Overdue Fine Calculation & Payment
    // ==========================================

    @Test
    void overdueFineCalculationAndPaymentLifecycle() throws Exception {
        Institution inst = institutionRepository.save(new Institution("Đại học Phú Xuân"));
        Campus campus = campusRepository.save(new Campus(inst, "Cơ sở 1"));
        Library library = libraryRepository.save(new Library(campus, "Thư viện CS1", "Tầng 2"));
        Category category = categoryRepository.save(new Category(null, "Khoa học"));
        BookTitle title = bookTitleRepository.save(new BookTitle("Vật lý Lượng tử", "Richard Feynman", "NXB GD", "978-0002", (short) 2018, category));
        BookCopy copy = bookCopyRepository.save(new BookCopy(title, library, "BC-OVERDUE-01", "Kệ 1", BookCopyStatus.BORROWED));

        AppUser student = new AppUser("sub-student-fine", "student.fine@phuxuan.edu.vn", "Trần Văn Phạt", UserRole.STUDENT);
        student.setStudentCode("SV2024999");
        student = appUserRepository.save(student);

        BorrowingPolicy policy = borrowingPolicyRepository.save(new BorrowingPolicy(
                library, 7, new BigDecimal("10000.00"), 3, Instant.now().minus(30, ChronoUnit.DAYS)
        ));

        // Create a borrow record that was due 3 days ago
        Instant borrowedAt = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant dueAt = Instant.now().minus(3, ChronoUnit.DAYS);
        Borrow overdueBorrow = new Borrow(student, copy, policy, borrowedAt, dueAt, policy.getDailyFine());
        overdueBorrow = borrowRepository.save(overdueBorrow);

        // 1. Return the overdue borrow -> fine calculated as dailyFine * 3 = 30000.00
        mockMvc.perform(post("/api/v1/borrows/" + overdueBorrow.getId() + "/return")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.fineAmount").value(30000.00))
                .andExpect(jsonPath("$.finePaidAt").doesNotExist());

        // 2. Student attempts to borrow new book while having unpaid fines -> rejected 400
        BookCopy newCopy = bookCopyRepository.save(new BookCopy(title, library, "BC-NEW-BOOK", "Kệ 2", BookCopyStatus.AVAILABLE));
        CheckoutRequest newCheckoutReq = new CheckoutRequest("SV2024999", List.of("BC-NEW-BOOK"));
        mockMvc.perform(post("/api/v1/borrows")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCheckoutReq)))
                .andExpect(status().isBadRequest());

        // 3. Librarian collects fine payment
        mockMvc.perform(post("/api/v1/borrows/" + overdueBorrow.getId() + "/fine-payment")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fineAmount").value(30000.00))
                .andExpect(jsonPath("$.finePaidAt").isNotEmpty());

        // 4. Fine payment is idempotent
        mockMvc.perform(post("/api/v1/borrows/" + overdueBorrow.getId() + "/fine-payment")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finePaidAt").isNotEmpty());

        // 5. Now student can borrow successfully
        mockMvc.perform(post("/api/v1/borrows")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCheckoutReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].barcode").value("BC-NEW-BOOK"));
    }

    // ==========================================
    // 4. Policy Limit & Conflict Tests
    // ==========================================

    @Test
    void borrowingLimitExceededAndUnavailableCopyConflict() throws Exception {
        Institution inst = institutionRepository.save(new Institution("Đại học Phú Xuân"));
        Campus campus = campusRepository.save(new Campus(inst, "Cơ sở 1"));
        Library library = libraryRepository.save(new Library(campus, "Thư viện CS1", "Tầng 1"));
        Category category = categoryRepository.save(new Category(null, "Sách Giáo Trình"));
        BookTitle title = bookTitleRepository.save(new BookTitle("Kinh tế lượng", "GS. Lê", "NXB GD", "978-0003", (short) 2021, category));

        BookCopy copy1 = bookCopyRepository.save(new BookCopy(title, library, "BC-LIM-01", "Kệ 1", BookCopyStatus.AVAILABLE));
        BookCopy copy2 = bookCopyRepository.save(new BookCopy(title, library, "BC-LIM-02", "Kệ 2", BookCopyStatus.AVAILABLE));
        BookCopy copyDamaged = bookCopyRepository.save(new BookCopy(title, library, "BC-DAMAGED", "Kệ 3", BookCopyStatus.DAMAGED));

        AppUser student = new AppUser("sub-student-lim", "student.lim@phuxuan.edu.vn", "Lê Văn Hạn Mức", UserRole.STUDENT);
        student.setStudentCode("SV2024888");
        student = appUserRepository.save(student);

        // Max active loans is 1
        borrowingPolicyRepository.save(new BorrowingPolicy(
                library, 14, new BigDecimal("2000.00"), 1, Instant.now().minus(1, ChronoUnit.HOURS)
        ));

        // 1. Checkout copy 1 succeeds (now user has 1 active loan)
        CheckoutRequest req1 = new CheckoutRequest("SV2024888", List.of("BC-LIM-01"));
        mockMvc.perform(post("/api/v1/borrows")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // 2. Attempting to checkout copy 2 exceeds maxActiveLoans (1) -> 400 Bad Request
        CheckoutRequest req2 = new CheckoutRequest("SV2024888", List.of("BC-LIM-02"));
        mockMvc.perform(post("/api/v1/borrows")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest());

        // 3. Another user attempts to borrow copy 1 (which is now BORROWED) -> 409 Conflict
        AppUser student2 = new AppUser("sub-student-lim2", "student.lim2@phuxuan.edu.vn", "Phạm Văn Hai", UserRole.STUDENT);
        student2.setStudentCode("SV2024777");
        student2 = appUserRepository.save(student2);

        CheckoutRequest reqConflict = new CheckoutRequest("SV2024777", List.of("BC-LIM-01"));
        mockMvc.perform(post("/api/v1/borrows")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqConflict)))
                .andExpect(status().isConflict());

        // 4. Attempting to borrow DAMAGED copy -> 409 Conflict
        CheckoutRequest reqDamaged = new CheckoutRequest("SV2024777", List.of("BC-DAMAGED"));
        mockMvc.perform(post("/api/v1/borrows")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDamaged)))
                .andExpect(status().isConflict());
    }

    // ==========================================
    // 5. Database Partial Unique Constraint Enforcement
    // ==========================================

    @Test
    void databasePartialUniqueConstraintBlocksSimultaneousActiveBorrow() {
        Institution inst = institutionRepository.save(new Institution("Đại học Phú Xuân"));
        Campus campus = campusRepository.save(new Campus(inst, "Cơ sở 1"));
        Library library = libraryRepository.save(new Library(campus, "Thư viện CS1", "Tầng 1"));
        Category category = categoryRepository.save(new Category(null, "CNTT"));
        BookTitle title = bookTitleRepository.save(new BookTitle("Database Systems", "Silberschatz", "McGraw-Hill", "978-0004", (short) 2019, category));
        BookCopy copy = bookCopyRepository.save(new BookCopy(title, library, "BC-UNIQUE-TEST", "Kệ 1", BookCopyStatus.AVAILABLE));

        AppUser user1 = new AppUser("sub-u1", "u1@phuxuan.edu.vn", "User One", UserRole.STUDENT);
        user1 = appUserRepository.save(user1);
        AppUser user2 = new AppUser("sub-u2", "u2@phuxuan.edu.vn", "User Two", UserRole.STUDENT);
        user2 = appUserRepository.save(user2);

        BorrowingPolicy policy = borrowingPolicyRepository.save(new BorrowingPolicy(
                library, 14, new BigDecimal("2000.00"), 5, Instant.now().minus(1, ChronoUnit.HOURS)
        ));

        // 1. Insert first active borrow record
        Borrow borrow1 = new Borrow(user1, copy, policy, Instant.now(), Instant.now().plus(14, ChronoUnit.DAYS), policy.getDailyFine());
        borrowRepository.saveAndFlush(borrow1);

        // 2. Direct database insert of second active borrow on the SAME copy must throw DataIntegrityViolationException
        // due to partial unique index uq_active_borrow_copy ON borrow(book_copy_id) WHERE returned_at IS NULL
        Borrow borrow2 = new Borrow(user2, copy, policy, Instant.now(), Instant.now().plus(14, ChronoUnit.DAYS), policy.getDailyFine());
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> borrowRepository.saveAndFlush(borrow2)
        );
    }
}
