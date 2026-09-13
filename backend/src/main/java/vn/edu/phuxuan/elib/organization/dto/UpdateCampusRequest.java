package vn.edu.phuxuan.elib.organization.dto;

import jakarta.validation.constraints.Size;

public record UpdateCampusRequest(
        Long institutionId,
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name
) {}
