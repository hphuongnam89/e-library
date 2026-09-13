package vn.edu.phuxuan.elib.circulation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import vn.edu.phuxuan.elib.circulation.Borrow;
import vn.edu.phuxuan.elib.circulation.BorrowStatus;

public record BorrowDto(
        Long id,
        Long userId,
        String userEmail,
        String studentCode,
        String userFullName,
        Long bookCopyId,
        String barcode,
        Long bookTitleId,
        String bookTitle,
        Long libraryId,
        String libraryName,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt,
        BigDecimal dailyFine,
        BigDecimal fineAmount,
        Instant finePaidAt,
        BorrowStatus status,
        boolean overdue
) {
    public static BorrowDto from(Borrow b) {
        boolean isOverdue = b.getStatus() == BorrowStatus.BORROWED && Instant.now().isAfter(b.getDueAt());
        return new BorrowDto(
                b.getId(),
                b.getUser() != null ? b.getUser().getId() : null,
                b.getUser() != null ? b.getUser().getEmail() : null,
                b.getUser() != null ? b.getUser().getStudentCode() : null,
                b.getUser() != null ? b.getUser().getFullName() : null,
                b.getBookCopy() != null ? b.getBookCopy().getId() : null,
                b.getBookCopy() != null ? b.getBookCopy().getBarcode() : null,
                b.getBookCopy() != null && b.getBookCopy().getBookTitle() != null ? b.getBookCopy().getBookTitle().getId() : null,
                b.getBookCopy() != null && b.getBookCopy().getBookTitle() != null ? b.getBookCopy().getBookTitle().getTitle() : null,
                b.getBookCopy() != null && b.getBookCopy().getLibrary() != null ? b.getBookCopy().getLibrary().getId() : null,
                b.getBookCopy() != null && b.getBookCopy().getLibrary() != null ? b.getBookCopy().getLibrary().getName() : null,
                b.getBorrowedAt(),
                b.getDueAt(),
                b.getReturnedAt(),
                b.getDailyFine(),
                b.getFineAmount(),
                b.getFinePaidAt(),
                b.getStatus(),
                isOverdue
        );
    }
}
