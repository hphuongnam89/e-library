package vn.edu.phuxuan.elib.reading.dto;

import java.time.Instant;

public record ReadingHistoryItemDto(
        Long documentId,
        String title,
        String publisher,
        Long activeSeconds,
        Long sessionCount,
        Instant lastActiveAt
) {}
