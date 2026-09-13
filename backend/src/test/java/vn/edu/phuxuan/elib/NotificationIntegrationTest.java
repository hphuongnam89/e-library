package vn.edu.phuxuan.elib;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import vn.edu.phuxuan.elib.notification.Notification;
import vn.edu.phuxuan.elib.notification.NotificationChannel;
import vn.edu.phuxuan.elib.notification.NotificationRepository;
import vn.edu.phuxuan.elib.notification.NotificationType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class NotificationIntegrationTest {

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
        registry.add("elib.storage.base-dir", () -> "./target/test-storage-notif-" + UUID.randomUUID());
        registry.add("elib.notifications.mock-email", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private AppUser studentUser;
    private AppUser librarianUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        appUserRepository.deleteAll();

        studentUser = new AppUser("sub-notif-stu", "student.notif@pxu.edu.vn", "Sinh Viên Thông Báo", UserRole.STUDENT);
        studentUser.setStatus(UserStatus.ACTIVE);
        studentUser = appUserRepository.save(studentUser);

        librarianUser = new AppUser("sub-notif-lib", "librarian.notif@pxu.edu.vn", "Thủ Thư Thông Báo", UserRole.LIBRARIAN);
        librarianUser.setStatus(UserStatus.ACTIVE);
        librarianUser = appUserRepository.save(librarianUser);
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    void unauthenticatedNotificationsFailsWith401() throws Exception {
        mockMvc.perform(get("/api/v1/me/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyNotificationsAndUnreadCountAndMarkAsRead() throws Exception {
        Notification notif = new Notification(
                studentUser, null, NotificationType.DUE_REMINDER, NotificationChannel.IN_APP,
                "Sách sắp đến hạn trả", "Vui lòng trả sách đúng hẹn", "DUE_REMINDER:TEST:1"
        );
        notif = notificationRepository.save(notif);

        // 1. Get unread count -> 1
        mockMvc.perform(get("/api/v1/me/notifications/unread-count")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(token -> token.claim("sub", studentUser.getGoogleSubject()).claim("email", studentUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        // 2. Get notifications list
        mockMvc.perform(get("/api/v1/me/notifications")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(token -> token.claim("sub", studentUser.getGoogleSubject()).claim("email", studentUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Sách sắp đến hạn trả"))
                .andExpect(jsonPath("$.content[0].readAt").isEmpty());

        // 3. Mark as read
        mockMvc.perform(patch("/api/v1/me/notifications/{id}/read", notif.getId())
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(token -> token.claim("sub", studentUser.getGoogleSubject()).claim("email", studentUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        // 4. Get unread count -> 0
        mockMvc.perform(get("/api/v1/me/notifications/unread-count")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(token -> token.claim("sub", studentUser.getGoogleSubject()).claim("email", studentUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void triggerRemindersForbiddenForStudentAllowedForLibrarian() throws Exception {
        // Student forbidden
        mockMvc.perform(post("/api/v1/librarian/notifications/trigger-reminders")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                                .idToken(token -> token.claim("sub", studentUser.getGoogleSubject()).claim("email", studentUser.getEmail()))))
                .andExpect(status().isForbidden());

        // Librarian allowed
        mockMvc.perform(post("/api/v1/librarian/notifications/trigger-reminders")
                        .with(csrf())
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                                .idToken(token -> token.claim("sub", librarianUser.getGoogleSubject()).claim("email", librarianUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueRemindersCreated").isNumber())
                .andExpect(jsonPath("$.overdueCreated").isNumber());
    }
}
