package vn.edu.phuxuan.elib.report.dto;

import java.time.Instant;

public record UserReportItemDto(
        Long userId,
        String email,
        String fullName,
        String studentCode,
        String role,
        String status,
        String departmentName,
        long activeBorrowsCount,
        long totalReadingSeconds,
        Instant createdAt
) {}
