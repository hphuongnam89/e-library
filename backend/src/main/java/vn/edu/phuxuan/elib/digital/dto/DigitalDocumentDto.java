package vn.edu.phuxuan.elib.digital.dto;

import java.time.Instant;
import vn.edu.phuxuan.elib.digital.DigitalDocument;
import vn.edu.phuxuan.elib.digital.DigitalDocumentPermission;
import vn.edu.phuxuan.elib.digital.DigitalDocumentStatus;

public record DigitalDocumentDto(
        Long id,
        Long libraryId,
        String libraryName,
        String title,
        String description,
        String publisher,
        Long categoryId,
        String categoryName,
        String contentType,
        Long sizeBytes,
        DigitalDocumentStatus status,
        DigitalDocumentPermission permission,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static DigitalDocumentDto from(DigitalDocument doc) {
        return new DigitalDocumentDto(
                doc.getId(),
                doc.getLibrary().getId(),
                doc.getLibrary().getName(),
                doc.getTitle(),
                doc.getDescription(),
                doc.getPublisher(),
                doc.getCategory() != null ? doc.getCategory().getId() : null,
                doc.getCategory() != null ? doc.getCategory().getName() : null,
                doc.getContentType(),
                doc.getSizeBytes(),
                doc.getStatus(),
                doc.getPermission(),
                doc.isActive(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
