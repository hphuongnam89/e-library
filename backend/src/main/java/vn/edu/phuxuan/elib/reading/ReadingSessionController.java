package vn.edu.phuxuan.elib.reading;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.CustomOidcUser;
import vn.edu.phuxuan.elib.reading.dto.EndReadingSessionResponse;
import vn.edu.phuxuan.elib.reading.dto.HeartbeatRequest;
import vn.edu.phuxuan.elib.reading.dto.HeartbeatResponse;
import vn.edu.phuxuan.elib.reading.dto.ReadingHistoryItemDto;
import vn.edu.phuxuan.elib.reading.dto.StartReadingSessionRequest;
import vn.edu.phuxuan.elib.reading.dto.StartReadingSessionResponse;

@RestController
@RequestMapping("/api/v1")
public class ReadingSessionController {

    private final ReadingSessionService readingSessionService;
    private final AppUserRepository appUserRepository;

    public ReadingSessionController(ReadingSessionService readingSessionService,
                                    AppUserRepository appUserRepository) {
        this.readingSessionService = readingSessionService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping("/reading/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public StartReadingSessionResponse startSession(
            @Valid @RequestBody StartReadingSessionRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return readingSessionService.startSession(request.documentId(), currentUser);
    }

    @PostMapping("/reading/sessions/{id}/heartbeat")
    public HeartbeatResponse sendHeartbeat(
            @PathVariable UUID id,
            @Valid @RequestBody HeartbeatRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return readingSessionService.processHeartbeat(id, request, currentUser);
    }

    @PostMapping("/reading/sessions/{id}/end")
    public EndReadingSessionResponse endSession(
            @PathVariable UUID id,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return readingSessionService.endSession(id, currentUser);
    }

    @GetMapping("/me/reading-history")
    public Page<ReadingHistoryItemDto> getMyReadingHistory(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return readingSessionService.getMyReadingHistory(currentUser, pageable);
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
