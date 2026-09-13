package vn.edu.phuxuan.elib.admin;

import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.admin.dto.AdminUserDto;
import vn.edu.phuxuan.elib.admin.dto.UpdateUserRequest;
import vn.edu.phuxuan.elib.admin.dto.UserImportResultDto;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.CustomOidcUser;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AppUserRepository appUserRepository;
    private final vn.edu.phuxuan.elib.organization.OrganizationService organizationService;

    public AdminUserController(AdminUserService adminUserService,
                               AppUserRepository appUserRepository,
                               vn.edu.phuxuan.elib.organization.OrganizationService organizationService) {
        this.adminUserService = adminUserService;
        this.appUserRepository = appUserRepository;
        this.organizationService = organizationService;
    }

    @PatchMapping("/{id}/department")
    public vn.edu.phuxuan.elib.identity.UserDto assignDepartment(
            @PathVariable Long id,
            @RequestBody vn.edu.phuxuan.elib.organization.dto.AssignDepartmentRequest req
    ) {
        return organizationService.assignDepartment(id, req.departmentId());
    }

    @GetMapping
    public ResponseEntity<Page<AdminUserDto>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Long departmentId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(adminUserService.searchUsers(search, role, status, departmentId, pageable));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminUserDto> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return ResponseEntity.ok(adminUserService.updateUser(id, request, currentUser));
    }

    @PostMapping("/import")
    public ResponseEntity<UserImportResultDto> importUsers(
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File tải lên không được để trống");
        }
        AppUser currentUser = resolveUser(authentication, principal);
        try {
            UserImportResultDto result = adminUserService.importUsers(file.getInputStream(), currentUser);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể đọc nội dung file tải lên", e);
        }
    }

    private AppUser resolveUser(Authentication authentication, Object principal) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        if (principal instanceof CustomOidcUser customUser) {
            return appUserRepository.findById(customUser.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }
        if (principal instanceof OidcUser oidcUser) {
            return appUserRepository.findByGoogleSubject(oidcUser.getSubject())
                    .or(() -> appUserRepository.findByEmailIgnoreCase(oidcUser.getEmail()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }
        if (principal instanceof UserDetails userDetails) {
            return appUserRepository.findByEmailIgnoreCase(userDetails.getUsername())
                    .or(() -> appUserRepository.findByGoogleSubject(userDetails.getUsername()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }
        String name = authentication.getName();
        return appUserRepository.findByEmailIgnoreCase(name)
                .or(() -> appUserRepository.findByGoogleSubject(name))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
