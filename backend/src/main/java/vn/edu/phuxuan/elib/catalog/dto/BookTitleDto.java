package vn.edu.phuxuan.elib.catalog.dto;

import java.time.Instant;
import vn.edu.phuxuan.elib.catalog.BookTitle;

public record BookTitleDto(
        Long id,
        String title,
        String author,
        String publisher,
        String isbn,
        Short publicationYear,
        Long categoryId,
        String categoryName,
        Instant createdAt,
        Instant updatedAt
) {
    public static BookTitleDto from(BookTitle entity) {
        return new BookTitleDto(
                entity.getId(),
                entity.getTitle(),
                entity.getAuthor(),
                entity.getPublisher(),
                entity.getIsbn(),
                entity.getPublicationYear(),
                entity.getCategory() != null ? entity.getCategory().getId() : null,
                entity.getCategory() != null ? entity.getCategory().getName() : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
