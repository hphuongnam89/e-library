package vn.edu.phuxuan.elib.admin.dto;

import java.time.Instant;

public record AdminUserDto(
        Long id,
        String email,
        String fullName,
        String studentCode,
        String role,
        String status,
        Long departmentId,
        String departmentName,
        Instant createdAt
) {}
