package vn.edu.phuxuan.elib.reading.dto;

public record EndReadingSessionResponse(
        boolean success,
        Long totalActiveSeconds
) {}
