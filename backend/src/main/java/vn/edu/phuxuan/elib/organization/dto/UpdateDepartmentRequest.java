package vn.edu.phuxuan.elib.organization.dto;

import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(
        Long libraryId,
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name
) {}
