package vn.edu.phuxuan.elib.admin;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.phuxuan.elib.admin.dto.SystemSettingDto;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.UserRole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSettingServiceTest {

    @Mock
    private SystemSettingRepository settingRepository;

    @Mock
    private AuditService auditService;

    private AdminSettingService settingService;

    @BeforeEach
    void setUp() {
        settingService = new AdminSettingService(settingRepository, auditService);
    }

    @Test
    @DisplayName("Lấy danh sách cấu hình phải che giấu giá trị bí mật")
    void getAllSettings_shouldMaskSecretSettings() {
        SystemSetting normal = new SystemSetting("library.name", "Đại học Phú Xuân", "Tên thư viện", false);
        SystemSetting secret = new SystemSetting("mail.password", "SuperSecret123", "Mật khẩu Mail", true);

        when(settingRepository.findAllByOrderByKeyAsc()).thenReturn(List.of(normal, secret));

        List<SystemSettingDto> result = settingService.getAllSettings();

        assertEquals(2, result.size());
        assertEquals("Đại học Phú Xuân", result.get(0).value());
        assertEquals("********", result.get(1).value());
    }

    @Test
    @DisplayName("Lấy giá trị thô của cấu hình nội bộ")
    void getSettingValue_shouldReturnRawValueOrDefault() {
        SystemSetting setting = new SystemSetting("auth.allowed_domains", "pxu.edu.vn", "Domain", false);
        when(settingRepository.findByKey("auth.allowed_domains")).thenReturn(Optional.of(setting));
        when(settingRepository.findByKey("non.existent")).thenReturn(Optional.empty());

        assertEquals("pxu.edu.vn", settingService.getSettingValue("auth.allowed_domains", "default.com"));
        assertEquals("default.com", settingService.getSettingValue("non.existent", "default.com"));
    }

    @Test
    @DisplayName("Cập nhật cấu hình thành công và ghi log kiểm toán")
    void updateSetting_shouldUpdateAndLogAudit() {
        AppUser admin = new AppUser("sub-1", "admin@pxu.edu.vn", "Admin User", UserRole.ADMIN);
        admin.setId(1L);

        SystemSetting setting = new SystemSetting("borrow.max_items_student", "3", "Số sách mượn", false);
        when(settingRepository.findByKey("borrow.max_items_student")).thenReturn(Optional.of(setting));
        when(settingRepository.save(any(SystemSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemSettingDto updated = settingService.updateSetting("borrow.max_items_student", "5", admin);

        assertEquals("5", updated.value());
        verify(settingRepository).save(setting);
        verify(auditService).log(eq(admin), eq("SETTING_UPDATE"), eq("SYSTEM_SETTING"), eq("borrow.max_items_student"), any());
    }

    @Test
    @DisplayName("Cập nhật cấu hình không tồn tại ném ngoại lệ")
    void updateSetting_notFound_shouldThrowException() {
        AppUser admin = new AppUser("sub-1", "admin@pxu.edu.vn", "Admin", UserRole.ADMIN);
        when(settingRepository.findByKey("invalid.key")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                settingService.updateSetting("invalid.key", "value", admin)
        );
    }
}
