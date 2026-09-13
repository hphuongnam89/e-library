package vn.edu.phuxuan.elib.notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByDeduplicationKey(String deduplicationKey);

    Optional<Notification> findByDeduplicationKey(String deduplicationKey);

    List<Notification> findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            List<NotificationStatus> statuses, Instant now
    );

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
           "AND (:type IS NULL OR n.notificationType = :type) " +
           "AND (:channel IS NULL OR n.channel = :channel) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findUserNotifications(
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            @Param("channel") NotificationChannel channel,
            Pageable pageable
    );

    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.user.id = :userId AND n.readAt IS NULL")
    int markAllAsReadForUser(@Param("userId") Long userId, @Param("readAt") Instant readAt);
}
