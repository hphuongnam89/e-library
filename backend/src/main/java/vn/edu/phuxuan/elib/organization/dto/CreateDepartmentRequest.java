package vn.edu.phuxuan.elib.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotNull(message = "Library ID is required")
        Long libraryId,
        @NotBlank(message = "Department name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name
) {}
