package vn.edu.phuxuan.elib.report.dto;

import java.time.Instant;

public record BookReportItemDto(
        Long copyId,
        Long titleId,
        String barcode,
        String title,
        String author,
        String isbn,
        String categoryName,
        String status,
        String libraryName,
        Instant createdAt
) {}
