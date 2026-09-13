package vn.edu.phuxuan.elib.admin.dto;

import java.time.Instant;

public record AuditLogDto(
        Long id,
        Long userId,
        String userEmail,
        String userFullName,
        String action,
        String resourceType,
        String resourceId,
        String requestId,
        String details,
        Instant createdAt
) {}
