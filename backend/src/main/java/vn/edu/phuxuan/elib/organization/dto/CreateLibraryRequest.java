package vn.edu.phuxuan.elib.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLibraryRequest(
        @NotNull(message = "Campus ID is required")
        Long campusId,
        @NotBlank(message = "Library name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address
) {}
