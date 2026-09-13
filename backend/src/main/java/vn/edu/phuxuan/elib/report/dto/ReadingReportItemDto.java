package vn.edu.phuxuan.elib.report.dto;

import java.time.Instant;

public record ReadingReportItemDto(
        Long documentId,
        String documentTitle,
        Long userId,
        String studentCode,
        String userName,
        String departmentName,
        long totalActiveSeconds,
        long sessionCount,
        Instant lastActiveAt
) {}
