package vn.edu.phuxuan.elib.organization.dto;

import jakarta.validation.constraints.Size;

public record UpdateLibraryRequest(
        Long campusId,
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address
) {}
