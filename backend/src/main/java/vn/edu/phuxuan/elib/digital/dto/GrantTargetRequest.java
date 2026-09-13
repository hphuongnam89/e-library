package vn.edu.phuxuan.elib.digital.dto;

public record GrantTargetRequest(
        Long institutionId,
        Long campusId,
        Long departmentId,
        Long userId
) {}
