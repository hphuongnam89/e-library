package vn.edu.phuxuan.elib.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.notification.dto.NotificationDto;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService service;

    private AppUser user;
    private AppUser otherUser;

    @BeforeEach
    void setUp() {
        user = new AppUser("sub-1", "user1@pxu.edu.vn", "Nguyễn Văn A", UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", 10L);

        otherUser = new AppUser("sub-2", "user2@pxu.edu.vn", "Trần Thị B", UserRole.STUDENT);
        otherUser.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(otherUser, "id", 20L);
    }

    @Test
    void createNotificationSuccessWhenKeyIsNew() {
        when(notificationRepository.existsByDeduplicationKey("KEY-123")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            ReflectionTestUtils.setField(n, "id", 1L);
            return n;
        });

        Optional<Notification> result = service.createNotification(
                user, null, NotificationType.DUE_REMINDER, NotificationChannel.EMAIL,
                "Tiêu đề", "Nội dung", "KEY-123"
        );

        assertThat(result).isPresent();
        assertThat(result.get().getDeduplicationKey()).isEqualTo("KEY-123");
        assertThat(result.get().getStatus()).isEqualTo(NotificationStatus.PENDING);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createNotificationDuplicateIsSkipped() {
        when(notificationRepository.existsByDeduplicationKey("KEY-EXISTS")).thenReturn(true);

        Optional<Notification> result = service.createNotification(
                user, null, NotificationType.DUE_REMINDER, NotificationChannel.EMAIL,
                "Tiêu đề", "Nội dung", "KEY-EXISTS"
        );

        assertThat(result).isEmpty();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void dispatchPendingNotificationsSuccessSetsSentStatus() {
        Notification notification = new Notification(
                user, null, NotificationType.DUE_REMINDER, NotificationChannel.EMAIL,
                "Tiêu đề", "Nội dung", "KEY-DISPATCH-1"
        );
        ReflectionTestUtils.setField(notification, "id", 100L);

        when(notificationRepository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(any(), any()))
                .thenReturn(List.of(notification));

        int count = service.dispatchPendingNotifications();

        assertThat(count).isEqualTo(1);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
        verify(emailService).sendEmail("user1@pxu.edu.vn", "Tiêu đề", "Nội dung");
        verify(notificationRepository).save(notification);
    }

    @Test
    void dispatchPendingNotificationsErrorIncrementsAttemptsAndBackoff() {
        Notification notification = new Notification(
                user, null, NotificationType.DUE_REMINDER, NotificationChannel.EMAIL,
                "Tiêu đề", "Nội dung", "KEY-DISPATCH-ERR"
        );
        ReflectionTestUtils.setField(notification, "id", 101L);

        when(notificationRepository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(any(), any()))
                .thenReturn(List.of(notification));
        doThrow(new RuntimeException("SMTP connection timeout"))
                .when(emailService).sendEmail(any(), any(), any());

        int count = service.dispatchPendingNotifications();

        assertThat(count).isEqualTo(0);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getAttempts()).isEqualTo(1);
        assertThat(notification.getNextAttemptAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    void dispatchPendingNotificationsPermanentFailureAfter3Attempts() {
        Notification notification = new Notification(
                user, null, NotificationType.DUE_REMINDER, NotificationChannel.EMAIL,
                "Tiêu đề", "Nội dung", "KEY-FAIL-3"
        );
        notification.setAttempts(2);
        ReflectionTestUtils.setField(notification, "id", 102L);

        when(notificationRepository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(any(), any()))
                .thenReturn(List.of(notification));
        doThrow(new RuntimeException("Mailbox full"))
                .when(emailService).sendEmail(any(), any(), any());

        int count = service.dispatchPendingNotifications();

        assertThat(count).isEqualTo(0);
        assertThat(notification.getAttempts()).isEqualTo(3);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsReadSuccess() {
        Notification notification = new Notification(
                user, null, NotificationType.DUE_REMINDER, NotificationChannel.IN_APP,
                "Tiêu đề", "Nội dung", "KEY-READ"
        );
        ReflectionTestUtils.setField(notification, "id", 200L);
        when(notificationRepository.findById(200L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationDto dto = service.markAsRead(200L, user);

        assertThat(dto.readAt()).isNotNull();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    void markAsReadForbiddenWhenNotificationOwnedByOtherUser() {
        Notification notification = new Notification(
                user, null, NotificationType.DUE_REMINDER, NotificationChannel.IN_APP,
                "Tiêu đề", "Nội dung", "KEY-FORBIDDEN"
        );
        ReflectionTestUtils.setField(notification, "id", 201L);
        when(notificationRepository.findById(201L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> service.markAsRead(201L, otherUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Thông báo không thuộc về bạn");
    }

    @Test
    void getUnreadCountReturnsTotal() {
        when(notificationRepository.countByUserIdAndReadAtIsNull(10L)).thenReturn(5L);

        long unread = service.getUnreadCount(user);
        assertThat(unread).isEqualTo(5L);
    }
}
