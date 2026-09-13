package vn.edu.phuxuan.elib.report.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DashboardSummaryDto(
        BookStats books,
        DigitalStats digital,
        CirculationStats circulation,
        UserStats users,
        List<TopBorrowedBookDto> topBorrowedBooks,
        List<TopReadDocumentDto> topReadDocuments,
        List<RecentBorrowDto> recentBorrows
) implements Serializable {
    public record BookStats(
            long totalTitles,
            long totalCopies,
            long availableCopies,
            long borrowedCopies,
            long maintenanceOrDamagedCopies
    ) implements Serializable {}

    public record DigitalStats(
            long totalDocuments,
            long totalReadingSessions,
            double totalReadingHours
    ) implements Serializable {}

    public record CirculationStats(
            long activeBorrows,
            long overdueBorrows,
            long returnedBorrows,
            BigDecimal totalUnpaidFines
    ) implements Serializable {}

    public record UserStats(
            long totalUsers,
            long activeUsers,
            long studentUsers,
            long lecturerUsers
    ) implements Serializable {}

    public record TopBorrowedBookDto(
            Long bookTitleId,
            String title,
            String author,
            long borrowCount
    ) implements Serializable {}

    public record TopReadDocumentDto(
            Long documentId,
            String title,
            String author,
            long activeSeconds,
            long sessionCount
    ) implements Serializable {}

    public record RecentBorrowDto(
            Long borrowId,
            String studentCode,
            String userName,
            String bookTitle,
            String barcode,
            Instant borrowedAt,
            Instant dueAt,
            String status
    ) implements Serializable {}
}
