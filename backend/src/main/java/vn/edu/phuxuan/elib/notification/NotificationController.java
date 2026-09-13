package vn.edu.phuxuan.elib.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.CustomOidcUser;
import vn.edu.phuxuan.elib.notification.dto.NotificationDto;
import vn.edu.phuxuan.elib.notification.dto.TriggerRemindersResponse;
import vn.edu.phuxuan.elib.notification.dto.UnreadCountDto;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    private final NotificationService notificationService;
    private final OverdueReminderScheduler overdueReminderScheduler;
    private final AppUserRepository appUserRepository;

    public NotificationController(
            NotificationService notificationService,
            OverdueReminderScheduler overdueReminderScheduler,
            AppUserRepository appUserRepository
    ) {
        this.notificationService = notificationService;
        this.overdueReminderScheduler = overdueReminderScheduler;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/me/notifications")
    public Page<NotificationDto> getMyNotifications(
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationChannel channel,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return notificationService.getUserNotifications(currentUser, type, channel, pageable);
    }

    @GetMapping("/me/notifications/unread-count")
    public UnreadCountDto getUnreadCount(
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        long count = notificationService.getUnreadCount(currentUser);
        return new UnreadCountDto(count);
    }

    @PatchMapping("/me/notifications/{id}/read")
    public NotificationDto markNotificationAsRead(
            @PathVariable Long id,
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return notificationService.markAsRead(id, currentUser);
    }

    @PostMapping("/me/notifications/read-all")
    public int markAllNotificationsAsRead(
            Authentication authentication,
            @AuthenticationPrincipal Object principal
    ) {
        AppUser currentUser = resolveUser(authentication, principal);
        return notificationService.markAllAsRead(currentUser);
    }

    @PostMapping("/librarian/notifications/trigger-reminders")
    public TriggerRemindersResponse triggerReminders() {
        return overdueReminderScheduler.runScanNow();
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
