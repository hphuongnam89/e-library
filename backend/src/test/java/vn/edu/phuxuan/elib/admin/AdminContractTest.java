package vn.edu.phuxuan.elib.admin;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.phuxuan.elib.admin.dto.AdminUserDto;
import vn.edu.phuxuan.elib.admin.dto.AuditLogDto;
import vn.edu.phuxuan.elib.admin.dto.SystemSettingDto;
import vn.edu.phuxuan.elib.admin.dto.UserImportResultDto;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.organization.OrganizationService;
import vn.edu.phuxuan.elib.web.GlobalExceptionHandler;
import vn.edu.phuxuan.elib.web.ProblemResponses;
import vn.edu.phuxuan.elib.web.RequestIdFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminContractTest {

    private MockMvc mvc;

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private AdminSettingService adminSettingService;

    @Mock
    private AuditService auditService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OrganizationService organizationService;

    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        AdminUserController userController = new AdminUserController(adminUserService, appUserRepository, organizationService);
        AdminAuditController auditController = new AdminAuditController(auditService);
        AdminSettingController settingController = new AdminSettingController(adminSettingService, appUserRepository);

        mvc = MockMvcBuilders.standaloneSetup(userController, auditController, settingController)
                .setCustomArgumentResolvers(
                        new org.springframework.data.web.PageableHandlerMethodArgumentResolver(),
                        new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver()
                )
                .setControllerAdvice(new GlobalExceptionHandler(new ProblemResponses(mapper)))
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    @DisplayName("Contract GET /api/v1/admin/users trả về Page AdminUserDto")
    void getUsersContract() throws Exception {
        AdminUserDto dto = new AdminUserDto(
                1L, "user@pxu.edu.vn", "Nguyễn Văn A", "SV001",
                "STUDENT", "ACTIVE", 10L, "Khoa CNTT", Instant.now()
        );
        when(adminUserService.searchUsers(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mvc.perform(get("/api/v1/admin/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("user@pxu.edu.vn"))
                .andExpect(jsonPath("$.content[0].role").value("STUDENT"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].departmentName").value("Khoa CNTT"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Contract POST /api/v1/admin/users/import trả về UserImportResultDto")
    void importUsersContract() throws Exception {
        AppUser admin = new AppUser("sub", "admin@pxu.edu.vn", "Admin", UserRole.ADMIN);
        admin.setId(99L);
        when(appUserRepository.findByEmailIgnoreCase(any())).thenReturn(java.util.Optional.of(admin));

        UserImportResultDto result = new UserImportResultDto(
                5, 4, 1,
                List.of(new UserImportResultDto.RowError(5, "bad@gmail.com", "Domain không hợp lệ"))
        );
        when(adminUserService.importUsers(any(), any())).thenReturn(result);

        org.springframework.security.authentication.TestingAuthenticationToken adminAuth =
                new org.springframework.security.authentication.TestingAuthenticationToken("admin@pxu.edu.vn", "pwd", "ROLE_ADMIN");
        adminAuth.setAuthenticated(true);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(adminAuth);

        MockMultipartFile file = new MockMultipartFile(
                "file", "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        mvc.perform(multipart("/api/v1/admin/users/import")
                        .file(file)
                        .principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(5))
                .andExpect(jsonPath("$.importedCount").value(4))
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.errors[0].email").value("bad@gmail.com"));
    }

    @Test
    @DisplayName("Contract GET /api/v1/admin/audit trả về Page AuditLogDto")
    void getAuditLogsContract() throws Exception {
        AuditLogDto log = new AuditLogDto(
                101L, 1L, "admin@pxu.edu.vn", "Admin",
                "USER_UPDATE_ROLE", "USER", "2", "req-12345",
                "Changed role", Instant.now()
        );
        when(auditService.searchAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        mvc.perform(get("/api/v1/admin/audit").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("USER_UPDATE_ROLE"))
                .andExpect(jsonPath("$.content[0].requestId").value("req-12345"))
                .andExpect(jsonPath("$.content[0].userEmail").value("admin@pxu.edu.vn"));
    }

    @Test
    @DisplayName("Contract GET & PATCH /api/v1/admin/settings")
    void settingsContract() throws Exception {
        org.springframework.security.authentication.TestingAuthenticationToken adminAuth =
                new org.springframework.security.authentication.TestingAuthenticationToken("admin@pxu.edu.vn", "pwd", "ROLE_ADMIN");
        adminAuth.setAuthenticated(true);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(adminAuth);

        SystemSettingDto s1 = new SystemSettingDto(
                1L, "auth.allowed_domains", "pxu.edu.vn",
                "Domain cho phép", false, Instant.now(), "admin@pxu.edu.vn"
        );
        SystemSettingDto s2 = new SystemSettingDto(
                2L, "mail.password", "********",
                "Mật khẩu", true, Instant.now(), "admin@pxu.edu.vn"
        );
        when(adminSettingService.getAllSettings()).thenReturn(List.of(s1, s2));

        mvc.perform(get("/api/v1/admin/settings").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("auth.allowed_domains"))
                .andExpect(jsonPath("$[0].value").value("pxu.edu.vn"))
                .andExpect(jsonPath("$[1].key").value("mail.password"))
                .andExpect(jsonPath("$[1].value").value("********"))
                .andExpect(jsonPath("$[1].isSecret").value(true));

        AppUser admin = new AppUser("sub", "admin@pxu.edu.vn", "Admin", UserRole.ADMIN);
        admin.setId(99L);
        when(appUserRepository.findByEmailIgnoreCase(any())).thenReturn(java.util.Optional.of(admin));

        SystemSettingDto updated = new SystemSettingDto(
                1L, "auth.allowed_domains", "pxu.edu.vn,newdomain.edu.vn",
                "Domain cho phép", false, Instant.now(), "admin@pxu.edu.vn"
        );
        when(adminSettingService.updateSetting(eq("auth.allowed_domains"), any(), any())).thenReturn(updated);

        mvc.perform(patch("/api/v1/admin/settings/auth.allowed_domains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"pxu.edu.vn,newdomain.edu.vn\"}")
                        .principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("pxu.edu.vn,newdomain.edu.vn"));
    }
}
