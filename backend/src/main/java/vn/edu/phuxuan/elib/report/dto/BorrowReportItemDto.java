package vn.edu.phuxuan.elib.report.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BorrowReportItemDto(
        Long borrowId,
        String studentCode,
        String userName,
        String departmentName,
        String bookTitle,
        String barcode,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt,
        String status,
        BigDecimal fineAmount,
        Instant finePaidAt
) {}
