package vn.edu.phuxuan.elib.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phuxuan.elib.catalog.BookCopyRepository;
import vn.edu.phuxuan.elib.catalog.BookCopyStatus;
import vn.edu.phuxuan.elib.catalog.BookTitleRepository;
import vn.edu.phuxuan.elib.circulation.Borrow;
import vn.edu.phuxuan.elib.circulation.BorrowRepository;
import vn.edu.phuxuan.elib.circulation.BorrowStatus;
import vn.edu.phuxuan.elib.digital.DigitalDocumentRepository;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.reading.DigitalReadingSessionRepository;
import vn.edu.phuxuan.elib.reading.DigitalReadingSummaryRepository;
import vn.edu.phuxuan.elib.report.dto.DashboardSummaryDto;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final BookTitleRepository bookTitleRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BorrowRepository borrowRepository;
    private final DigitalDocumentRepository digitalDocumentRepository;
    private final DigitalReadingSessionRepository digitalReadingSessionRepository;
    private final DigitalReadingSummaryRepository digitalReadingSummaryRepository;
    private final AppUserRepository appUserRepository;

    public DashboardService(
            BookTitleRepository bookTitleRepository,
            BookCopyRepository bookCopyRepository,
            BorrowRepository borrowRepository,
            DigitalDocumentRepository digitalDocumentRepository,
            DigitalReadingSessionRepository digitalReadingSessionRepository,
            DigitalReadingSummaryRepository digitalReadingSummaryRepository,
            AppUserRepository appUserRepository
    ) {
        this.bookTitleRepository = bookTitleRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.borrowRepository = borrowRepository;
        this.digitalDocumentRepository = digitalDocumentRepository;
        this.digitalReadingSessionRepository = digitalReadingSessionRepository;
        this.digitalReadingSummaryRepository = digitalReadingSummaryRepository;
        this.appUserRepository = appUserRepository;
    }

    @Cacheable("dashboardSummary")
    public DashboardSummaryDto getDashboardSummary() {
        // Book metrics
        long totalTitles = bookTitleRepository.count();
        long totalCopies = bookCopyRepository.count();
        long availableCopies = bookCopyRepository.countByStatus(BookCopyStatus.AVAILABLE);
        long borrowedCopies = bookCopyRepository.countByStatus(BookCopyStatus.BORROWED);
        long otherCopies = Math.max(0, totalCopies - (availableCopies + borrowedCopies));

        DashboardSummaryDto.BookStats bookStats = new DashboardSummaryDto.BookStats(
                totalTitles, totalCopies, availableCopies, borrowedCopies, otherCopies
        );

        // Digital metrics
        long totalDocs = digitalDocumentRepository.count();
        long totalSessions = digitalReadingSessionRepository.count();
        long totalActiveSecs = digitalReadingSummaryRepository.sumTotalActiveSeconds();
        double totalHours = Math.round((totalActiveSecs / 3600.0) * 10.0) / 10.0;

        DashboardSummaryDto.DigitalStats digitalStats = new DashboardSummaryDto.DigitalStats(
                totalDocs, totalSessions, totalHours
        );

        // Circulation metrics
        Instant now = Instant.now();
        long activeBorrows = borrowRepository.countByStatus(BorrowStatus.BORROWED);
        long overdueBorrows = borrowRepository.countByStatusAndDueAtBefore(BorrowStatus.BORROWED, now);
        long returnedBorrows = borrowRepository.countByStatus(BorrowStatus.RETURNED);
        BigDecimal unpaidFines = borrowRepository.sumUnpaidFines();

        DashboardSummaryDto.CirculationStats circStats = new DashboardSummaryDto.CirculationStats(
                activeBorrows, overdueBorrows, returnedBorrows, unpaidFines
        );

        // User metrics
        long totalUsers = appUserRepository.count();
        long activeUsers = appUserRepository.countByStatus(UserStatus.ACTIVE);
        long studentUsers = appUserRepository.countByRole(UserRole.STUDENT);
        long lecturerUsers = appUserRepository.countByRole(UserRole.LECTURER);

        DashboardSummaryDto.UserStats userStats = new DashboardSummaryDto.UserStats(
                totalUsers, activeUsers, studentUsers, lecturerUsers
        );

        // Top borrowed books
        List<Object[]> rawTopBooks = borrowRepository.findTopBorrowedBooks(PageRequest.of(0, 5));
        List<DashboardSummaryDto.TopBorrowedBookDto> topBooks = new ArrayList<>();
        for (Object[] row : rawTopBooks) {
            Long titleId = (Long) row[0];
            String title = (String) row[1];
            String author = (String) row[2];
            long count = ((Number) row[3]).longValue();
            topBooks.add(new DashboardSummaryDto.TopBorrowedBookDto(titleId, title, author, count));
        }

        // Top read documents
        List<Object[]> rawTopDocs = digitalReadingSummaryRepository.findTopReadDocuments(PageRequest.of(0, 5));
        List<DashboardSummaryDto.TopReadDocumentDto> topDocs = new ArrayList<>();
        for (Object[] row : rawTopDocs) {
            Long docId = (Long) row[0];
            String title = (String) row[1];
            String author = (String) row[2];
            long activeSeconds = ((Number) row[3]).longValue();
            long sessionCount = ((Number) row[4]).longValue();
            topDocs.add(new DashboardSummaryDto.TopReadDocumentDto(docId, title, author, activeSeconds, sessionCount));
        }

        // Recent borrows
        List<Borrow> recent = borrowRepository.findRecentBorrows(PageRequest.of(0, 5));
        List<DashboardSummaryDto.RecentBorrowDto> recentList = recent.stream().map(b ->
                new DashboardSummaryDto.RecentBorrowDto(
                        b.getId(),
                        b.getUser() != null ? b.getUser().getStudentCode() : null,
                        b.getUser() != null ? b.getUser().getFullName() : null,
                        b.getBookCopy() != null && b.getBookCopy().getBookTitle() != null
                                ? b.getBookCopy().getBookTitle().getTitle() : null,
                        b.getBookCopy() != null ? b.getBookCopy().getBarcode() : null,
                        b.getBorrowedAt(),
                        b.getDueAt(),
                        b.getStatus() != null ? b.getStatus().name() : null
                )
        ).toList();

        return new DashboardSummaryDto(
                bookStats, digitalStats, circStats, userStats, topBooks, topDocs, recentList
        );
    }
}
