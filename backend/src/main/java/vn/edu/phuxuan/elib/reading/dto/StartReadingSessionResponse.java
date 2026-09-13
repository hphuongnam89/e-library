package vn.edu.phuxuan.elib.reading.dto;

import java.util.UUID;

public record StartReadingSessionResponse(
        UUID sessionId,
        int heartbeatIntervalSeconds,
        int idleTimeoutSeconds
) {}
