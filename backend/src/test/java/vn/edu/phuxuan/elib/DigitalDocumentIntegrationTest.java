package vn.edu.phuxuan.elib;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import vn.edu.phuxuan.elib.catalog.Category;
import vn.edu.phuxuan.elib.catalog.CategoryRepository;
import vn.edu.phuxuan.elib.digital.DigitalDocument;
import vn.edu.phuxuan.elib.digital.DigitalDocumentPermission;
import vn.edu.phuxuan.elib.digital.DigitalDocumentRepository;
import vn.edu.phuxuan.elib.digital.DigitalDocumentStatus;
import vn.edu.phuxuan.elib.digital.DocumentGrantRepository;
import vn.edu.phuxuan.elib.digital.dto.ApproveDocumentRequest;
import vn.edu.phuxuan.elib.digital.dto.GrantTargetRequest;
import vn.edu.phuxuan.elib.digital.dto.UpdateDocumentGrantsRequest;
import vn.edu.phuxuan.elib.digital.storage.StorageService;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.organization.Campus;
import vn.edu.phuxuan.elib.organization.CampusRepository;
import vn.edu.phuxuan.elib.organization.Department;
import vn.edu.phuxuan.elib.organization.DepartmentRepository;
import vn.edu.phuxuan.elib.organization.Institution;
import vn.edu.phuxuan.elib.organization.InstitutionRepository;
import vn.edu.phuxuan.elib.organization.Library;
import vn.edu.phuxuan.elib.organization.LibraryRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class DigitalDocumentIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "digital-test-redis");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry props) {
        props.add("spring.datasource.url", postgres::getJdbcUrl);
        props.add("spring.datasource.username", postgres::getUsername);
        props.add("spring.datasource.password", postgres::getPassword);
        props.add("spring.data.redis.host", redis::getHost);
        props.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        props.add("spring.data.redis.password", () -> "digital-test-redis");
        props.add("elib.storage.local-dir", () -> "./target/test-storage-" + System.currentTimeMillis());
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    DigitalDocumentRepository documentRepository;

    @Autowired
    DocumentGrantRepository grantRepository;

    @Autowired
    CategoryRepository categoryRepository;

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

    @Autowired
    StorageService storageService;

    private Institution institution;
    private Campus campus;
    private Library library;
    private Department deptCs;
    private Department deptBiz;
    private AppUser librarianUser;
    private AppUser studentCs;
    private AppUser studentBiz;
    private Category category;

    @BeforeEach
    void setUp() {
        grantRepository.deleteAll();
        documentRepository.deleteAll();
        appUserRepository.deleteAll();
        departmentRepository.deleteAll();
        libraryRepository.deleteAll();
        campusRepository.deleteAll();
        institutionRepository.deleteAll();
        categoryRepository.deleteAll();

        institution = institutionRepository.save(new Institution("Trường Đại học Phú Xuân"));
        campus = campusRepository.save(new Campus(institution, "Cơ sở 1"));
        library = libraryRepository.save(new Library(campus, "Thư viện Trung tâm", "176 Trần Phú, Huế"));

        deptCs = departmentRepository.save(new Department(library, "Khoa Công nghệ Thông tin"));
        deptBiz = departmentRepository.save(new Department(library, "Khoa Quản trị Kinh doanh"));

        category = categoryRepository.save(new Category(null, "Giáo trình CNTT"));

        librarianUser = new AppUser("sub-librarian", "librarian@pxu.edu.vn", "Thủ Thư", UserRole.LIBRARIAN);
        librarianUser.setStatus(UserStatus.ACTIVE);
        librarianUser = appUserRepository.save(librarianUser);

        studentCs = new AppUser("sub-student-cs", "student1@pxu.edu.vn", "Nguyễn Văn A", UserRole.STUDENT);
        studentCs.setDepartment(deptCs);
        studentCs.setStatus(UserStatus.ACTIVE);
        studentCs = appUserRepository.save(studentCs);

        studentBiz = new AppUser("sub-student-biz", "student2@pxu.edu.vn", "Trần Thị B", UserRole.STUDENT);
        studentBiz.setDepartment(deptBiz);
        studentBiz.setStatus(UserStatus.ACTIVE);
        studentBiz = appUserRepository.save(studentBiz);
    }

    @AfterEach
    void cleanUp() {
        grantRepository.deleteAll();
        documentRepository.deleteAll();
        appUserRepository.deleteAll();
        departmentRepository.deleteAll();
        libraryRepository.deleteAll();
        campusRepository.deleteAll();
        institutionRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    private byte[] createSamplePdfBytes(int targetSize) {
        byte[] header = "%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII);
        byte[] trailer = "\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
        int paddingSize = Math.max(0, targetSize - header.length - trailer.length);
        byte[] padding = new byte[paddingSize];
        Arrays.fill(padding, (byte) 'A');

        byte[] result = new byte[header.length + padding.length + trailer.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(padding, 0, result, header.length, padding.length);
        System.arraycopy(trailer, 0, result, header.length + padding.length, trailer.length);
        return result;
    }

    // ==========================================
    // 1. Security & RBAC Tests
    // ==========================================

    @Test
    void unauthenticatedAccessToDigitalDocumentEndpointsIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/digital-documents"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/digital-documents/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/digital-documents/1/stream"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotPerformManagementActions() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", createSamplePdfBytes(100));

        // Upload
        mockMvc.perform(multipart("/api/v1/digital-documents")
                        .file(file)
                        .param("libraryId", library.getId().toString())
                        .param("title", "Giáo trình Cấu trúc dữ liệu")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentCs.getEmail()).subject(studentCs.getGoogleSubject()))))
                .andExpect(status().isForbidden());

        // Submit
        mockMvc.perform(post("/api/v1/digital-documents/1/submit")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentCs.getEmail()).subject(studentCs.getGoogleSubject()))))
                .andExpect(status().isForbidden());

        // Approve
        mockMvc.perform(post("/api/v1/digital-documents/1/approve")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentCs.getEmail()).subject(studentCs.getGoogleSubject())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveDocumentRequest(true, "ok"))))
                .andExpect(status().isForbidden());

        // Publish
        mockMvc.perform(post("/api/v1/digital-documents/1/publish")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentCs.getEmail()).subject(studentCs.getGoogleSubject()))))
                .andExpect(status().isForbidden());

        // Grants
        mockMvc.perform(get("/api/v1/digital-documents/1/grants")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentCs.getEmail()).subject(studentCs.getGoogleSubject()))))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 2. Document Lifecycle & State Machine (D019)
    // ==========================================

    @Test
    void fullDocumentLifecycleAndStateTransitions() throws Exception {
        byte[] pdfBytes = createSamplePdfBytes(200);
        MockMultipartFile file = new MockMultipartFile("file", "giaotrinh.pdf", "application/pdf", pdfBytes);

        // 1. Librarian uploads document -> status DRAFT
        String uploadJson = mockMvc.perform(multipart("/api/v1/digital-documents")
                        .file(file)
                        .param("libraryId", library.getId().toString())
                        .param("title", "Giáo trình Lập trình Java")
                        .param("description", "Tài liệu cơ bản")
                        .param("publisher", "ĐH Phú Xuân")
                        .param("categoryId", category.getId().toString())
                        .param("permission", "RESTRICTED")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.permission").value("RESTRICTED"))
                .andExpect(jsonPath("$.title").value("Giáo trình Lập trình Java"))
                .andReturn().getResponse().getContentAsString();

        Long docId = objectMapper.readTree(uploadJson).get("id").asLong();

        // Cannot publish directly from DRAFT
        mockMvc.perform(post("/api/v1/digital-documents/" + docId + "/publish")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject()))))
                .andExpect(status().isBadRequest());

        // 2. Submit -> PENDING
        mockMvc.perform(post("/api/v1/digital-documents/" + docId + "/submit")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // 3. Reject/rework back to DRAFT
        mockMvc.perform(post("/api/v1/digital-documents/" + docId + "/approve")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveDocumentRequest(false, "Cần chỉnh sửa mô tả"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        // 4. Submit again -> PENDING
        mockMvc.perform(post("/api/v1/digital-documents/" + docId + "/submit")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // 5. Approve -> APPROVED
        mockMvc.perform(post("/api/v1/digital-documents/" + docId + "/approve")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveDocumentRequest(true, "Đạt chuẩn"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 6. Publish -> PUBLISHED
        mockMvc.perform(post("/api/v1/digital-documents/" + docId + "/publish")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // 7. Cannot rework after PUBLISHED (D019)
        mockMvc.perform(post("/api/v1/digital-documents/" + docId + "/approve")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveDocumentRequest(false, "Yêu cầu thu hồi"))))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 3. File Validation & Magic Bytes
    // ==========================================

    @Test
    void pdfValidationRejectsInvalidFilesAndMagicBytes() throws Exception {
        // Disguised non-PDF file (magic bytes mismatch)
        byte[] badBytes = "NOT A VALID PDF FILE AT ALL".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile fakePdf = new MockMultipartFile("file", "fake.pdf", "application/pdf", badBytes);

        mockMvc.perform(multipart("/api/v1/digital-documents")
                        .file(fakePdf)
                        .param("libraryId", library.getId().toString())
                        .param("title", "Fake PDF Title")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject()))))
                .andExpect(status().isBadRequest());

        // Empty file
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        mockMvc.perform(multipart("/api/v1/digital-documents")
                        .file(emptyFile)
                        .param("libraryId", library.getId().toString())
                        .param("title", "Empty PDF")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject()))))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 4. Restricted Grants Enforcement
    // ==========================================

    @Test
    void restrictedDocumentGrantsEnforceAccessControl() throws Exception {
        byte[] pdfBytes = createSamplePdfBytes(300);
        MockMultipartFile file = new MockMultipartFile("file", "cs_special.pdf", "application/pdf", pdfBytes);

        // Upload, approve and publish document restricted to CS department
        String uploadJson = mockMvc.perform(multipart("/api/v1/digital-documents")
                        .file(file)
                        .param("libraryId", library.getId().toString())
                        .param("title", "Giáo trình Chuyên ngành CNTT")
                        .param("permission", "RESTRICTED")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long docId = objectMapper.readTree(uploadJson).get("id").asLong();

        // Submit, approve, publish
        mockMvc.perform(post("/api/v1/digital-documents/" + docId + "/submit").with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                        .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject()))));
        mockMvc.perform(post("/api/v1/digital-documents/" + docId + "/approve").with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                        .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ApproveDocumentRequest(true, "OK"))));
        mockMvc.perform(post("/api/v1/digital-documents/" + docId + "/publish").with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                        .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject()))));

        // 1. Before granting: studentCs has NO grant -> 403
        mockMvc.perform(get("/api/v1/digital-documents/" + docId)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentCs.getEmail()).subject(studentCs.getGoogleSubject()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/digital-documents/" + docId + "/stream")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentCs.getEmail()).subject(studentCs.getGoogleSubject()))))
                .andExpect(status().isForbidden());

        // 2. Librarian grants access to deptCs
        UpdateDocumentGrantsRequest grantsReq = new UpdateDocumentGrantsRequest(
                List.of(new GrantTargetRequest(null, null, deptCs.getId(), null)));

        mockMvc.perform(put("/api/v1/digital-documents/" + docId + "/grants")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grantsReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].departmentId").value(deptCs.getId()));

        // 3. studentCs (Dept CS) can now access metadata and stream
        mockMvc.perform(get("/api/v1/digital-documents/" + docId)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentCs.getEmail()).subject(studentCs.getGoogleSubject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId));

        mockMvc.perform(get("/api/v1/digital-documents/" + docId + "/stream")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentCs.getEmail()).subject(studentCs.getGoogleSubject()))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));

        // 4. studentBiz (Dept Biz) is STILL forbidden -> 403
        mockMvc.perform(get("/api/v1/digital-documents/" + docId)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentBiz.getEmail()).subject(studentBiz.getGoogleSubject()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/digital-documents/" + docId + "/stream")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentBiz.getEmail()).subject(studentBiz.getGoogleSubject()))))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 5. HTTP Range Streaming & Partial Content (206)
    // ==========================================

    @Test
    void httpRangeStreamingSupportsPartialContent() throws Exception {
        byte[] pdfBytes = createSamplePdfBytes(2048); // 2KB PDF
        MockMultipartFile file = new MockMultipartFile("file", "streaming_test.pdf", "application/pdf", pdfBytes);

        // Upload and publish public document
        String uploadJson = mockMvc.perform(multipart("/api/v1/digital-documents")
                        .file(file)
                        .param("libraryId", library.getId().toString())
                        .param("title", "Tài liệu Kiểm thử Streaming")
                        .param("permission", "AUTHENTICATED")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long docId = objectMapper.readTree(uploadJson).get("id").asLong();

        // Submit, approve, publish
        mockMvc.perform(post("/api/v1/digital-documents/" + docId + "/submit").with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                        .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject()))));
        mockMvc.perform(post("/api/v1/digital-documents/" + docId + "/approve").with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                        .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ApproveDocumentRequest(true, "OK"))));
        mockMvc.perform(post("/api/v1/digital-documents/" + docId + "/publish").with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                        .idToken(t -> t.claim("email", librarianUser.getEmail()).subject(librarianUser.getGoogleSubject()))));

        // 1. Request full file (without Range header) -> 200 OK
        byte[] fullResponse = mockMvc.perform(get("/api/v1/digital-documents/" + docId + "/stream")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentCs.getEmail()).subject(studentCs.getGoogleSubject()))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("Cache-Control", "private, no-store, must-revalidate"))
                .andExpect(header().string("Content-Length", String.valueOf(pdfBytes.length)))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(fullResponse).isEqualTo(pdfBytes);

        // 2. Request byte range: Range: bytes=0-499 -> 206 Partial Content
        byte[] rangeResponse = mockMvc.perform(get("/api/v1/digital-documents/" + docId + "/stream")
                        .header("Range", "bytes=0-499")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentCs.getEmail()).subject(studentCs.getGoogleSubject()))))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("Content-Range", "bytes 0-499/" + pdfBytes.length))
                .andExpect(header().string("Content-Length", "500"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(rangeResponse.length).isEqualTo(500);
        assertThat(rangeResponse).isEqualTo(Arrays.copyOfRange(pdfBytes, 0, 500));

        // 3. Request suffix range: Range: bytes=1000- -> 206 Partial Content
        byte[] tailResponse = mockMvc.perform(get("/api/v1/digital-documents/" + docId + "/stream")
                        .header("Range", "bytes=1000-")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentCs.getEmail()).subject(studentCs.getGoogleSubject()))))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 1000-" + (pdfBytes.length - 1) + "/" + pdfBytes.length))
                .andExpect(header().string("Content-Length", String.valueOf(pdfBytes.length - 1000)))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(tailResponse.length).isEqualTo(pdfBytes.length - 1000);
        assertThat(tailResponse).isEqualTo(Arrays.copyOfRange(pdfBytes, 1000, pdfBytes.length));

        // 4. Request invalid range: Range: bytes=9000-9500 -> 416 Range Not Satisfiable
        mockMvc.perform(get("/api/v1/digital-documents/" + docId + "/stream")
                        .header("Range", "bytes=9000-9500")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(t -> t.claim("email", studentCs.getEmail()).subject(studentCs.getGoogleSubject()))))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string("Content-Range", "bytes */" + pdfBytes.length));
    }
}
