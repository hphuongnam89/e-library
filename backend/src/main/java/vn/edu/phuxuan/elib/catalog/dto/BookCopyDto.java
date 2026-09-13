package vn.edu.phuxuan.elib.catalog.dto;

import java.time.Instant;
import vn.edu.phuxuan.elib.catalog.BookCopy;
import vn.edu.phuxuan.elib.catalog.BookCopyStatus;

public record BookCopyDto(
        Long id,
        Long bookTitleId,
        String bookTitle,
        Long libraryId,
        String libraryName,
        String barcode,
        String location,
        BookCopyStatus status,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static BookCopyDto from(BookCopy entity) {
        return new BookCopyDto(
                entity.getId(),
                entity.getBookTitle().getId(),
                entity.getBookTitle().getTitle(),
                entity.getLibrary().getId(),
                entity.getLibrary().getName(),
                entity.getBarcode(),
                entity.getLocation(),
                entity.getStatus(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
