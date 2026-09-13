package vn.edu.phuxuan.elib.catalog.dto;

import jakarta.validation.constraints.Size;
import vn.edu.phuxuan.elib.catalog.BookCopyStatus;

public record UpdateBookCopyRequest(
        Long libraryId,
        @Size(max = 255, message = "Location must not exceed 255 characters")
        String location,
        BookCopyStatus status
) {}
