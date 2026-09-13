package vn.edu.phuxuan.elib;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import vn.edu.phuxuan.elib.digital.DigitalDocument;
import vn.edu.phuxuan.elib.digital.DigitalDocumentPermission;
import vn.edu.phuxuan.elib.digital.DigitalDocumentRepository;
import vn.edu.phuxuan.elib.digital.DigitalDocumentStatus;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.organization.Campus;
import vn.edu.phuxuan.elib.organization.CampusRepository;
import vn.edu.phuxuan.elib.organization.Institution;
import vn.edu.phuxuan.elib.organization.InstitutionRepository;
import vn.edu.phuxuan.elib.organization.Library;
import vn.edu.phuxuan.elib.organization.LibraryRepository;
import vn.edu.phuxuan.elib.reading.DigitalReadingSessionRepository;
import vn.edu.phuxuan.elib.reading.DigitalReadingSummaryRepository;
import vn.edu.phuxuan.elib.reading.ReadingHeartbeatRepository;
import vn.edu.phuxuan.elib.reading.dto.HeartbeatRequest;
import vn.edu.phuxuan.elib.reading.dto.StartReadingSessionRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class ReadingSessionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("elib_test")
            .withUsername("elib_test")
            .withPassword("elib_test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("elib.storage.base-dir", () -> "./target/test-storage-reading-" + UUID.randomUUID());
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstitutionRepository institutionRepository;
    @Autowired
    private CampusRepository campusRepository;
    @Autowired
    private LibraryRepository libraryRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private DigitalDocumentRepository documentRepository;
    @Autowired
    private DigitalReadingSessionRepository sessionRepository;
    @Autowired
    private ReadingHeartbeatRepository heartbeatRepository;
    @Autowired
    private DigitalReadingSummaryRepository summaryRepository;

    private AppUser studentUser;
    private DigitalDocument publicDoc;

    @BeforeEach
    void setUp() {
        cleanup();

        Institution inst = institutionRepository.save(new Institution("Đại học Phú Xuân"));
        Campus campus = campusRepository.save(new Campus(inst, "Cơ sở 1"));
        Library lib = libraryRepository.save(new Library(campus, "Thư viện Khoa học", "Huế"));

        studentUser = new AppUser("sub-stu-read", "student.read@pxu.edu.vn", "Sinh Viên Đọc Sách", UserRole.STUDENT);
        studentUser.setStudentCode("PXU-READ-01");
        studentUser.setStatus(UserStatus.ACTIVE);
        studentUser = appUserRepository.save(studentUser);

        publicDoc = new DigitalDocument(
                lib, "Giáo trình Cấu trúc Dữ liệu & Giải thuật", "Mô tả", "NXB KHKT", null,
                "key-dsa-" + UUID.randomUUID() + ".pdf", "application/pdf", 2048L, DigitalDocumentPermission.AUTHENTICATED
        );
        publicDoc.setStatus(DigitalDocumentStatus.PUBLISHED);
        publicDoc = documentRepository.save(publicDoc);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        heartbeatRepository.deleteAll();
        sessionRepository.deleteAll();
        summaryRepository.deleteAll();
        documentRepository.deleteAll();
        appUserRepository.deleteAll();
        libraryRepository.deleteAll();
        campusRepository.deleteAll();
        institutionRepository.deleteAll();
    }

    @Test
    void unauthenticatedReadingSessionFailsWith401() throws Exception {
        mockMvc.perform(post("/api/v1/reading/sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartReadingSessionRequest(publicDoc.getId()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullReadingSessionAndHeartbeatLifecycle() throws Exception {
        // 1. Start Session
        MvcResult startResult = mockMvc.perform(post("/api/v1/reading/sessions")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(token -> token.claim("sub", studentUser.getGoogleSubject()).claim("email", studentUser.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartReadingSessionRequest(publicDoc.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.heartbeatIntervalSeconds").value(15))
                .andExpect(jsonPath("$.idleTimeoutSeconds").value(60))
                .andReturn();

        String responseBody = startResult.getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(responseBody).get("sessionId").asText();

        // 2. Send Heartbeat 1 (Active & Visible)
        HeartbeatRequest hb1 = new HeartbeatRequest(1L, true, true);
        mockMvc.perform(post("/api/v1/reading/sessions/{id}/heartbeat", sessionId)
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(token -> token.claim("sub", studentUser.getGoogleSubject()).claim("email", studentUser.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hb1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.sessionEnded").value(false));

        // 3. Send Heartbeat with Hidden Tab (active=true, visible=false) -> no active time added
        HeartbeatRequest hb2 = new HeartbeatRequest(2L, true, false);
        mockMvc.perform(post("/api/v1/reading/sessions/{id}/heartbeat", sessionId)
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(token -> token.claim("sub", studentUser.getGoogleSubject()).claim("email", studentUser.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hb2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true));

        // 4. End Session (Idempotent)
        mockMvc.perform(post("/api/v1/reading/sessions/{id}/end", sessionId)
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(token -> token.claim("sub", studentUser.getGoogleSubject()).claim("email", studentUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 5. Subsequent Heartbeat is rejected
        HeartbeatRequest hb3 = new HeartbeatRequest(3L, true, true);
        mockMvc.perform(post("/api/v1/reading/sessions/{id}/heartbeat", sessionId)
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(token -> token.claim("sub", studentUser.getGoogleSubject()).claim("email", studentUser.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hb3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.sessionEnded").value(true));

        // 6. Check Reading History
        mockMvc.perform(get("/api/v1/me/reading-history")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(token -> token.claim("sub", studentUser.getGoogleSubject()).claim("email", studentUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Giáo trình Cấu trúc Dữ liệu & Giải thuật"))
                .andExpect(jsonPath("$.content[0].sessionCount").value(1));
    }
}
