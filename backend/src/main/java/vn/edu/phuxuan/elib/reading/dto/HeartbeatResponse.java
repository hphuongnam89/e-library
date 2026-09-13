package vn.edu.phuxuan.elib.reading.dto;

public record HeartbeatResponse(
        boolean accepted,
        Long activeSeconds,
        boolean sessionEnded
) {}
