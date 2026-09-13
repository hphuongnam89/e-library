package vn.edu.phuxuan.elib.digital.dto;

import vn.edu.phuxuan.elib.digital.DigitalDocumentPermission;

public record UpdateDigitalDocumentRequest(
        String title,
        String description,
        String publisher,
        Long categoryId,
        DigitalDocumentPermission permission,
        Boolean isActive
) {}
