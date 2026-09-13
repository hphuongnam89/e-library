package vn.edu.phuxuan.elib.admin;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.admin.dto.SystemSettingDto;
import vn.edu.phuxuan.elib.admin.dto.UpdateSettingRequest;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.CustomOidcUser;

@RestController
@RequestMapping("/api/v1/admin/settings")
public class AdminSettingController {

    private final AdminSettingService adminSettingService;
    private final AppUserRepository appUserRepository;

    public AdminSettingController(AdminSettingService adminSettingService, AppUserRepository appUserRepository) {
        this.adminSettingService = adminSettingService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping
    public ResponseEntity<List<SystemSettingDto>> getSettings() {
        return ResponseEntity.ok(adminSettingService.getAllSettings());
    }

    @PatchMapping("/{key}")
    public ResponseEntity<SystemSettingDto> updateSetting(
            @PathVariable String key,
            @Valid @RequestBody UpdateSettingRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return ResponseEntity.ok(adminSettingService.updateSetting(key, request.value(), currentUser));
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
