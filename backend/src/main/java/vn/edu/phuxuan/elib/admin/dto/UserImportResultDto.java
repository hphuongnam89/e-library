package vn.edu.phuxuan.elib.admin.dto;

import java.util.List;

public record UserImportResultDto(
        int totalRows,
        int importedCount,
        int failedCount,
        List<RowError> errors
) {
    public record RowError(int rowNumber, String email, String message) {}
}
