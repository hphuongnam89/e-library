package vn.edu.phuxuan.elib.admin.dto;

import java.io.Serializable;
import java.time.Instant;

public record SystemSettingDto(
        Long id,
        String key,
        String value,
        String description,
        boolean isSecret,
        Instant updatedAt,
        String updatedByEmail
) implements Serializable {}
