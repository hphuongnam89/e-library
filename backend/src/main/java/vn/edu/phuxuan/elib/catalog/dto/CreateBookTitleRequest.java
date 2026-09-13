package vn.edu.phuxuan.elib.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBookTitleRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 500, message = "Title must not exceed 500 characters")
        String title,
        @Size(max = 255, message = "Author must not exceed 255 characters")
        String author,
        @Size(max = 255, message = "Publisher must not exceed 255 characters")
        String publisher,
        @Size(max = 20, message = "ISBN must not exceed 20 characters")
        String isbn,
        Short publicationYear,
        Long categoryId
) {}
