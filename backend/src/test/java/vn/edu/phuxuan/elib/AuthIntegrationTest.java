package vn.edu.phuxuan.elib;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "auth-test-redis");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry props) {
        props.add("spring.datasource.url", postgres::getJdbcUrl);
        props.add("spring.datasource.username", postgres::getUsername);
        props.add("spring.datasource.password", postgres::getPassword);
        props.add("spring.data.redis.host", redis::getHost);
        props.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        props.add("spring.data.redis.password", () -> "auth-test-redis");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void unauthenticatedGetMeReturns401WithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void csrfEndpointProvidesTokenAndSetsXsrfCookie() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").isNotEmpty())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void csrfRejectsPostWithoutXsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutWithCsrfReturns204NoContent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void authenticatedOidcUserGetsProfile() throws Exception {
        AppUser user = new AppUser("sub-12345", "sinhvien@phuxuan.edu.vn", "Nguyễn Văn A", UserRole.STUDENT);
        user.setStudentCode("SV001");
        user = userRepository.save(user);

        mockMvc.perform(get("/api/v1/auth/me").with(oidcLogin()
                        .idToken(token -> token.subject("sub-12345").claim("email", "sinhvien@phuxuan.edu.vn"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("sinhvien@phuxuan.edu.vn"))
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.studentCode").value("SV001"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void studentRoleIsForbiddenFromAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoleCanPassAdminSecurityGate() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound()); // Security check passed, 404 because controller isn't mapped
    }

    @Test
    void librarianRoleIsForbiddenFromAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void librarianRoleCanPassLibrarianSecurityGate() throws Exception {
        mockMvc.perform(get("/api/v1/librarian/books")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))))
                .andExpect(status().isNotFound()); // Security check passed
    }

    @Test
    void appUserEntityEnforcesUniquenessAndConstraints() {
        AppUser user1 = new AppUser("google-sub-1", "test@phuxuan.edu.vn", "Test User", UserRole.STUDENT);
        userRepository.saveAndFlush(user1);

        // Duplicate google_subject should fail
        AppUser user2 = new AppUser("google-sub-1", "different@phuxuan.edu.vn", "Another Name", UserRole.LECTURER);
        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void appUserEntityEnforcesEmailCaseInsensitiveUniqueness() {
        AppUser user1 = new AppUser("google-sub-a", "user@phuxuan.edu.vn", "User A", UserRole.STUDENT);
        userRepository.saveAndFlush(user1);

        // Duplicate email with different case should fail via uq_user_email index
        AppUser user2 = new AppUser("google-sub-b", "USER@PHUXUAN.EDU.VN", "User B", UserRole.STUDENT);
        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
