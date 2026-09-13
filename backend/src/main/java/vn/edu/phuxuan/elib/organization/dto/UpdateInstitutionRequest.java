package vn.edu.phuxuan.elib.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateInstitutionRequest(
        @NotBlank(message = "Institution name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name
) {}
