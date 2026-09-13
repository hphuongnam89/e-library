package vn.edu.phuxuan.elib.notification.dto;

import java.time.Instant;
import vn.edu.phuxuan.elib.notification.NotificationChannel;
import vn.edu.phuxuan.elib.notification.NotificationStatus;
import vn.edu.phuxuan.elib.notification.NotificationType;

public record NotificationDto(
        Long id,
        Long borrowId,
        NotificationType notificationType,
        NotificationChannel channel,
        String title,
        String message,
        String deduplicationKey,
        NotificationStatus status,
        Instant sentAt,
        Instant readAt,
        Instant createdAt
) {}
