package vn.edu.phuxuan.elib.admin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.edu.phuxuan.elib.admin.dto.AdminUserDto;
import vn.edu.phuxuan.elib.admin.dto.UpdateUserRequest;
import vn.edu.phuxuan.elib.admin.dto.UserImportResultDto;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.organization.Department;
import vn.edu.phuxuan.elib.organization.DepartmentRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AdminSettingService adminSettingService;

    @Mock
    private AuditService auditService;

    private AdminUserService userService;

    @BeforeEach
    void setUp() {
        userService = new AdminUserService(userRepository, departmentRepository, adminSettingService, auditService);
    }

    @Test
    @DisplayName("Tìm kiếm người dùng trả về danh sách phân trang AdminUserDto")
    void searchUsers_shouldReturnPagedDto() {
        AppUser u = new AppUser("sub-1", "user@pxu.edu.vn", "Nguyễn Văn A", UserRole.STUDENT);
        u.setId(1L);
        Page<AppUser> page = new PageImpl<>(List.of(u));

        when(userRepository.searchAdminUsers(eq("Nguyễn"), eq(UserRole.STUDENT), any(), any(), any()))
                .thenReturn(page);

        Page<AdminUserDto> result = userService.searchUsers("Nguyễn", UserRole.STUDENT, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("user@pxu.edu.vn", result.getContent().get(0).email());
        assertEquals("STUDENT", result.getContent().get(0).role());
    }

    @Test
    @DisplayName("Cập nhật người dùng thành công và ghi log kiểm toán")
    void updateUser_success_shouldLogAudit() {
        AppUser admin = new AppUser("sub-admin", "admin@pxu.edu.vn", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        AppUser target = new AppUser("sub-2", "target@pxu.edu.vn", "Target User", UserRole.STUDENT);
        target.setId(2L);

        Department dept = new Department();
        dept.setId(5L);
        dept.setName("CNTT");

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(departmentRepository.findById(5L)).thenReturn(Optional.of(dept));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRequest req = new UpdateUserRequest(UserRole.LECTURER, UserStatus.ACTIVE, 5L);
        AdminUserDto dto = userService.updateUser(2L, req, admin);

        assertEquals("LECTURER", dto.role());
        assertEquals("CNTT", dto.departmentName());
        verify(auditService).log(eq(admin), eq("USER_UPDATE_ROLE"), eq("USER"), eq("2"), any());
    }

    @Test
    @DisplayName("Bảo vệ chống tự hạ quyền của Admin")
    void updateUser_selfDemotion_shouldThrowException() {
        AppUser admin = new AppUser("sub-admin", "admin@pxu.edu.vn", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        UpdateUserRequest demoteReq = new UpdateUserRequest(UserRole.STUDENT, UserStatus.ACTIVE, null);
        IllegalStateException ex1 = assertThrows(IllegalStateException.class, () ->
                userService.updateUser(1L, demoteReq, admin)
        );
        assertTrue(ex1.getMessage().contains("tự hạ quyền"));

        UpdateUserRequest lockReq = new UpdateUserRequest(UserRole.ADMIN, UserStatus.INACTIVE, null);
        IllegalStateException ex2 = assertThrows(IllegalStateException.class, () ->
                userService.updateUser(1L, demoteReq, admin)
        );
    }

    @Test
    @DisplayName("Nhập người dùng hàng loạt từ Excel thành công")
    void importUsers_success() throws IOException {
        AppUser admin = new AppUser("sub-admin", "admin@pxu.edu.vn", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        when(adminSettingService.getSettingValue("auth.allowed_domains", "pxu.edu.vn"))
                .thenReturn("pxu.edu.vn");

        // Prepare Excel in memory
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Users");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Email");
            header.createCell(1).setCellValue("Họ và tên");
            header.createCell(2).setCellValue("Mã SV/GV");
            header.createCell(3).setCellValue("Vai trò");
            header.createCell(4).setCellValue("Khoa");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("sv1@pxu.edu.vn");
            r1.createCell(1).setCellValue("Sinh Viên 1");
            r1.createCell(2).setCellValue("SV001");
            r1.createCell(3).setCellValue("STUDENT");

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("gv1@pxu.edu.vn");
            r2.createCell(1).setCellValue("Giảng Viên 1");
            r2.createCell(2).setCellValue("GV001");
            r2.createCell(3).setCellValue("LECTURER");

            wb.write(bos);
        }

        UserImportResultDto result = userService.importUsers(new ByteArrayInputStream(bos.toByteArray()), admin);

        assertEquals(2, result.totalRows());
        assertEquals(2, result.importedCount());
        assertEquals(0, result.failedCount());
        verify(userRepository).saveAll(any());
        verify(auditService).log(eq(admin), eq("USER_BULK_IMPORT"), eq("USER"), any(), any());
    }

    @Test
    @DisplayName("Nhập người dùng từ Excel phát hiện domain không hợp lệ hoặc thiếu dữ liệu")
    void importUsers_withErrors() throws IOException {
        AppUser admin = new AppUser("sub-admin", "admin@pxu.edu.vn", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        when(adminSettingService.getSettingValue("auth.allowed_domains", "pxu.edu.vn"))
                .thenReturn("pxu.edu.vn");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Users");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Email");
            header.createCell(1).setCellValue("Họ và tên");

            // Row 1: invalid domain gmail.com
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("hacker@gmail.com");
            r1.createCell(1).setCellValue("Hacker");

            // Row 2: empty name
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("valid@pxu.edu.vn");
            r2.createCell(1).setCellValue("");

            wb.write(bos);
        }

        UserImportResultDto result = userService.importUsers(new ByteArrayInputStream(bos.toByteArray()), admin);

        assertEquals(2, result.totalRows());
        assertEquals(0, result.importedCount());
        assertEquals(2, result.failedCount());
        assertEquals(2, result.errors().size());
    }
}
