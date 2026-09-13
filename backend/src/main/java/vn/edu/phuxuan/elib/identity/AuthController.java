package vn.edu.phuxuan.elib.identity;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AppUserRepository userRepository;

    public AuthController(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserDto me(Authentication authentication, @AuthenticationPrincipal Object principal) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        if (principal instanceof CustomOidcUser customUser) {
            return userRepository.findById(customUser.getUserId())
                    .map(UserDto::from)
                    .orElseGet(() -> new UserDto(
                            customUser.getUserId(),
                            customUser.getEmail(),
                            customUser.getFullName(),
                            customUser.getStudentCode(),
                            customUser.getRole(),
                            customUser.getStatus(),
                            null
                    ));
        }

        if (principal instanceof OidcUser oidcUser) {
            return userRepository.findByGoogleSubject(oidcUser.getSubject())
                    .or(() -> userRepository.findByEmailIgnoreCase(oidcUser.getEmail()))
                    .map(UserDto::from)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
        }

        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByEmailIgnoreCase(userDetails.getUsername())
                    .or(() -> userRepository.findByGoogleSubject(userDetails.getUsername()))
                    .map(UserDto::from)
                    .orElseGet(() -> new UserDto(
                            0L,
                            userDetails.getUsername(),
                            userDetails.getUsername(),
                            null,
                            UserRole.STUDENT,
                            UserStatus.ACTIVE,
                            null
                    ));
        }

        // Fallback using authentication name (e.g. mock test usernames)
        String name = authentication.getName();
        return userRepository.findByEmailIgnoreCase(name)
                .or(() -> userRepository.findByGoogleSubject(name))
                .map(UserDto::from)
                .orElseGet(() -> new UserDto(
                        0L,
                        name,
                        name,
                        null,
                        UserRole.STUDENT,
                        UserStatus.ACTIVE,
                        null
                ));
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken, jakarta.servlet.http.HttpServletResponse response) {
        if (csrfToken == null) {
            return Map.of();
        }
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("XSRF-TOKEN", csrfToken.getToken());
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        response.addCookie(cookie);

        return Map.of(
                "parameterName", csrfToken.getParameterName(),
                "headerName", csrfToken.getHeaderName(),
                "token", csrfToken.getToken()
        );
    }
}
