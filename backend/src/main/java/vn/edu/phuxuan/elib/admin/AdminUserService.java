package vn.edu.phuxuan.elib.admin;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phuxuan.elib.admin.dto.AdminUserDto;
import vn.edu.phuxuan.elib.admin.dto.UpdateUserRequest;
import vn.edu.phuxuan.elib.admin.dto.UserImportResultDto;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.organization.Department;
import vn.edu.phuxuan.elib.organization.DepartmentRepository;

@Service
public class AdminUserService {

    private final AppUserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AdminSettingService adminSettingService;
    private final AuditService auditService;

    public AdminUserService(AppUserRepository userRepository,
                            DepartmentRepository departmentRepository,
                            AdminSettingService adminSettingService,
                            AuditService auditService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.adminSettingService = adminSettingService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> searchUsers(String search, UserRole role, UserStatus status, Long departmentId, Pageable pageable) {
        return userRepository.searchAdminUsers(search, role, status, departmentId, pageable)
                .map(this::toDto);
    }

    @Transactional
    public AdminUserDto updateUser(Long userId, UpdateUserRequest request, AppUser currentUser) {
        AppUser targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));

        // Self-demotion / Self-lock protection
        if (currentUser != null && currentUser.getId().equals(userId)) {
            if (request.role() != null && request.role() != UserRole.ADMIN) {
                throw new IllegalStateException("Quản trị viên không thể tự hạ quyền của chính mình");
            }
            if (request.status() != null && request.status() != UserStatus.ACTIVE) {
                throw new IllegalStateException("Quản trị viên không thể tự khóa tài khoản của chính mình");
            }
        }

        if (request.role() != null && request.role() != targetUser.getRole()) {
            UserRole oldRole = targetUser.getRole();
            targetUser.setRole(request.role());
            auditService.log(
                    currentUser,
                    "USER_UPDATE_ROLE",
                    "USER",
                    String.valueOf(userId),
                    "Thay đổi vai trò người dùng [" + targetUser.getEmail() + "] từ " + oldRole + " thành " + request.role()
            );
        }

        if (request.status() != null && request.status() != targetUser.getStatus()) {
            UserStatus oldStatus = targetUser.getStatus();
            targetUser.setStatus(request.status());
            auditService.log(
                    currentUser,
                    "USER_UPDATE_STATUS",
                    "USER",
                    String.valueOf(userId),
                    "Thay đổi trạng thái người dùng [" + targetUser.getEmail() + "] từ " + oldStatus + " thành " + request.status()
            );
        }

        if (request.departmentId() != null) {
            Department dept = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoa/phòng ban với ID: " + request.departmentId()));
            targetUser.setDepartment(dept);
        }

        AppUser saved = userRepository.save(targetUser);
        return toDto(saved);
    }

    @Transactional
    public UserImportResultDto importUsers(InputStream inputStream, AppUser currentUser) {
        List<UserImportResultDto.RowError> errors = new ArrayList<>();
        List<AppUser> validUsers = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();
        Set<String> seenStudentCodes = new HashSet<>();

        String allowedDomainsSetting = adminSettingService.getSettingValue("auth.allowed_domains", "pxu.edu.vn");
        List<String> allowedDomains = Arrays.stream(allowedDomainsSetting.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .toList();

        int totalRows = 0;
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            for (int r = 1; r <= lastRowNum; r++) { // skip header row 0
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                String email = getCellValue(row.getCell(0), formatter);
                String fullName = getCellValue(row.getCell(1), formatter);
                String studentCode = getCellValue(row.getCell(2), formatter);
                String roleStr = getCellValue(row.getCell(3), formatter);
                String deptStr = getCellValue(row.getCell(4), formatter);

                // Skip entirely empty rows
                if (email.isEmpty() && fullName.isEmpty() && studentCode.isEmpty()) {
                    continue;
                }

                totalRows++;
                int rowNum = r + 1; // 1-based index for human display

                if (email.isEmpty()) {
                    errors.add(new UserImportResultDto.RowError(rowNum, "", "Email không được để trống"));
                    continue;
                }

                String emailLower = email.toLowerCase();
                boolean domainAllowed = allowedDomains.isEmpty() || allowedDomains.stream().anyMatch(d -> emailLower.endsWith("@" + d));
                if (!domainAllowed) {
                    errors.add(new UserImportResultDto.RowError(rowNum, email, "Domain email không thuộc danh sách cho phép (" + allowedDomainsSetting + ")"));
                    continue;
                }

                if (seenEmails.contains(emailLower) || userRepository.existsByEmailIgnoreCase(emailLower)) {
                    errors.add(new UserImportResultDto.RowError(rowNum, email, "Email đã tồn tại trong hệ thống hoặc file tải lên"));
                    continue;
                }

                if (fullName.isEmpty()) {
                    errors.add(new UserImportResultDto.RowError(rowNum, email, "Họ và tên không được để trống"));
                    continue;
                }

                if (!studentCode.isEmpty()) {
                    String codeLower = studentCode.toLowerCase();
                    if (seenStudentCodes.contains(codeLower) || userRepository.existsByStudentCode(studentCode)) {
                        errors.add(new UserImportResultDto.RowError(rowNum, email, "Mã số SV/GV đã tồn tại: " + studentCode));
                        continue;
                    }
                }

                UserRole role = UserRole.STUDENT;
                if (!roleStr.isEmpty()) {
                    try {
                        role = UserRole.valueOf(roleStr.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        errors.add(new UserImportResultDto.RowError(rowNum, email, "Vai trò không hợp lệ: " + roleStr));
                        continue;
                    }
                }

                Department department = null;
                if (!deptStr.isEmpty()) {
                    try {
                        Long deptId = Long.parseLong(deptStr);
                        department = departmentRepository.findById(deptId).orElse(null);
                    } catch (NumberFormatException ex) {
                        department = departmentRepository.findByNameIgnoreCase(deptStr).orElse(null);
                    }
                    if (department == null) {
                        errors.add(new UserImportResultDto.RowError(rowNum, email, "Không tìm thấy khoa/phòng ban: " + deptStr));
                        continue;
                    }
                }

                AppUser user = new AppUser();
                user.setGoogleSubject("IMPORT:" + UUID.randomUUID());
                user.setEmail(emailLower);
                user.setFullName(fullName);
                if (!studentCode.isEmpty()) {
                    user.setStudentCode(studentCode);
                    seenStudentCodes.add(studentCode.toLowerCase());
                }
                user.setRole(role);
                user.setStatus(UserStatus.ACTIVE);
                user.setDepartment(department);

                seenEmails.add(emailLower);
                validUsers.add(user);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể đọc file Excel: " + e.getMessage(), e);
        }

        if (!validUsers.isEmpty()) {
            userRepository.saveAll(validUsers);
            auditService.log(
                    currentUser,
                    "USER_BULK_IMPORT",
                    "USER",
                    null,
                    "Nhập thành công " + validUsers.size() + " người dùng từ file Excel (Thất bại: " + errors.size() + ")"
            );
        }

        return new UserImportResultDto(totalRows, validUsers.size(), errors.size(), errors);
    }

    private String getCellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private AdminUserDto toDto(AppUser u) {
        Long deptId = u.getDepartment() != null ? u.getDepartment().getId() : null;
        String deptName = u.getDepartment() != null ? u.getDepartment().getName() : null;
        return new AdminUserDto(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getStudentCode(),
                u.getRole().name(),
                u.getStatus().name(),
                deptId,
                deptName,
                u.getCreatedAt()
        );
    }
}
