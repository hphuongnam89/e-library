package vn.edu.phuxuan.elib.notification;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.circulation.Borrow;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.notification.dto.NotificationDto;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    public static final int MAX_ATTEMPTS = 3;

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository, EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    public Optional<Notification> createNotification(
            AppUser user,
            Borrow borrow,
            NotificationType type,
            NotificationChannel channel,
            String title,
            String message,
            String deduplicationKey
    ) {
        if (notificationRepository.existsByDeduplicationKey(deduplicationKey)) {
            log.info("Deduplication key '{}' already exists. Skipping duplicate notification.", deduplicationKey);
            return Optional.empty();
        }

        Notification notification = new Notification(
                user, borrow, type, channel, title, message, deduplicationKey
        );
        return Optional.of(notificationRepository.save(notification));
    }

    public int dispatchPendingNotifications() {
        Instant now = Instant.now();
        List<Notification> pending = notificationRepository
                .findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        List.of(NotificationStatus.PENDING, NotificationStatus.FAILED), now
                );

        int dispatchedCount = 0;
        for (Notification n : pending) {
            if (n.getChannel() == NotificationChannel.EMAIL) {
                try {
                    emailService.sendEmail(n.getUser().getEmail(), n.getTitle(), n.getMessage());
                    n.setStatus(NotificationStatus.SENT);
                    n.setSentAt(Instant.now());
                    dispatchedCount++;
                } catch (Exception e) {
                    int attempts = n.getAttempts() + 1;
                    n.setAttempts(attempts);
                    if (attempts >= MAX_ATTEMPTS) {
                        n.setStatus(NotificationStatus.FAILED);
                        log.warn("Notification ID {} permanently failed after {} attempts.", n.getId(), attempts);
                    } else {
                        // Exponential backoff: 2^attempts * 5 minutes (10m, 20m...)
                        long backoffMinutes = (long) Math.pow(2, attempts) * 5;
                        n.setNextAttemptAt(Instant.now().plus(Duration.ofMinutes(backoffMinutes)));
                    }
                }
            } else {
                // IN_APP is immediately visible
                n.setStatus(NotificationStatus.SENT);
                n.setSentAt(Instant.now());
                dispatchedCount++;
            }
            notificationRepository.save(n);
        }
        return dispatchedCount;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(
            AppUser currentUser,
            NotificationType type,
            NotificationChannel channel,
            Pageable pageable
    ) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        Page<Notification> page = notificationRepository.findUserNotifications(
                currentUser.getId(), type, channel, pageable
        );
        return page.map(this::toDto);
    }

    public NotificationDto markAsRead(Long notificationId, AppUser currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo"));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Thông báo không thuộc về bạn");
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }

        return toDto(notification);
    }

    public int markAllAsRead(AppUser currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        return notificationRepository.markAllAsReadForUser(currentUser.getId(), Instant.now());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(AppUser currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        return notificationRepository.countByUserIdAndReadAtIsNull(currentUser.getId());
    }

    public NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getBorrow() != null ? n.getBorrow().getId() : null,
                n.getNotificationType(),
                n.getChannel(),
                n.getTitle(),
                n.getMessage(),
                n.getDeduplicationKey(),
                n.getStatus(),
                n.getSentAt(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}
