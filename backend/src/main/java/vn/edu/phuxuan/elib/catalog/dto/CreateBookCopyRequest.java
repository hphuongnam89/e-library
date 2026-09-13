package vn.edu.phuxuan.elib.catalog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.phuxuan.elib.catalog.BookCopyStatus;

public record CreateBookCopyRequest(
        @NotNull(message = "Book title ID is required")
        Long bookTitleId,
        @NotNull(message = "Library ID is required")
        Long libraryId,
        @Size(max = 100, message = "Barcode must not exceed 100 characters")
        String barcode,
        @Size(max = 255, message = "Location must not exceed 255 characters")
        String location,
        BookCopyStatus status
) {}
