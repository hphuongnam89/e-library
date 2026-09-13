package vn.edu.phuxuan.elib.reading.dto;

import jakarta.validation.constraints.NotNull;

public record HeartbeatRequest(
        @NotNull(message = "sequenceNumber không được để trống")
        Long sequenceNumber,
        Boolean active,
        Boolean visible
) {}
