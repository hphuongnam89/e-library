package vn.edu.phuxuan.elib.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSettingRequest(
        @NotNull(message = "Giá trị cấu hình không được để trống")
        String value
) {}
