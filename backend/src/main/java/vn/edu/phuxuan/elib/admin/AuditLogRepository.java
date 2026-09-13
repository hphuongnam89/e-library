package vn.edu.phuxuan.elib.admin;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query(value = """
            SELECT a FROM AuditLog a
            LEFT JOIN FETCH a.user u
            WHERE (:action IS NULL OR a.action = :action)
              AND (:resourceType IS NULL OR a.resourceType = :resourceType)
              AND (:userId IS NULL OR u.id = :userId)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            ORDER BY a.id DESC
            """,
            countQuery = """
            SELECT COUNT(a) FROM AuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:resourceType IS NULL OR a.resourceType = :resourceType)
              AND (:userId IS NULL OR a.user.id = :userId)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            """)
    Page<AuditLog> searchAuditLogs(
            @Param("action") String action,
            @Param("resourceType") String resourceType,
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
