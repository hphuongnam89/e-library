package vn.edu.phuxuan.elib.reading.dto;

import jakarta.validation.constraints.NotNull;

public record StartReadingSessionRequest(
        @NotNull(message = "documentId không được để trống")
        Long documentId
) {}
