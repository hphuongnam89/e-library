package vn.edu.phuxuan.elib.admin;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.edu.phuxuan.elib.admin.dto.AuditLogDto;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AppUserRepository userRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository, userRepository);
    }

    @Test
    @DisplayName("Ghi log kiểm toán thành công kèm correlation request ID từ MDC")
    void logWithUser_shouldCaptureMdcAndSave() {
        MDC.put("requestId", "req-12345");
        try {
            AppUser user = new AppUser("sub-1", "admin@pxu.edu.vn", "Admin User", UserRole.ADMIN);
            user.setId(1L);

            auditService.log(user, "USER_UPDATE_ROLE", "USER", "2", "Changed role to LIBRARIAN");

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLog saved = captor.getValue();
            assertNotNull(saved);
            assertEquals(user, saved.getUser());
            assertEquals("USER_UPDATE_ROLE", saved.getAction());
            assertEquals("USER", saved.getResourceType());
            assertEquals("2", saved.getResourceId());
            assertEquals("req-12345", saved.getRequestId());
            assertEquals("Changed role to LIBRARIAN", saved.getDetails());
        } finally {
            MDC.clear();
        }
    }

    @Test
    @DisplayName("Ghi log kiểm toán bằng userId")
    void logWithUserId_shouldFindUserAndSave() {
        AppUser user = new AppUser("sub-1", "admin@pxu.edu.vn", "Admin User", UserRole.ADMIN);
        user.setId(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        auditService.log(10L, "SETTING_UPDATE", "SETTING", "key1", "Updated key");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(user, captor.getValue().getUser());
    }

    @Test
    @DisplayName("Tìm kiếm audit log phân trang và chuyển đổi sang DTO")
    void searchAuditLogs_shouldReturnMappedDtoPage() {
        AppUser user = new AppUser("sub-1", "admin@pxu.edu.vn", "Admin User", UserRole.ADMIN);
        user.setId(1L);

        AuditLog log = new AuditLog(user, "USER_UPDATE_ROLE", "USER", "2", "req-1", "Details");
        Page<AuditLog> mockPage = new PageImpl<>(List.of(log));

        when(auditLogRepository.searchAuditLogs(eq("USER_UPDATE_ROLE"), any(), any(), any(), any(), any()))
                .thenReturn(mockPage);

        Page<AuditLogDto> result = auditService.searchAuditLogs("USER_UPDATE_ROLE", null, null, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        AuditLogDto dto = result.getContent().get(0);
        assertEquals("USER_UPDATE_ROLE", dto.action());
        assertEquals("admin@pxu.edu.vn", dto.userEmail());
        assertEquals("Admin User", dto.userFullName());
    }
}
