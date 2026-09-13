package vn.edu.phuxuan.elib.digital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vn.edu.phuxuan.elib.digital.DigitalDocumentPermission;

public record CreateDigitalDocumentRequest(
        @NotNull(message = "libraryId is required")
        Long libraryId,

        @NotBlank(message = "title is required")
        String title,

        String description,
        String publisher,
        Long categoryId,
        DigitalDocumentPermission permission
) {}
