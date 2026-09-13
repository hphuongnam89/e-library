package vn.edu.phuxuan.elib.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.phuxuan.elib.catalog.BookCopy;
import vn.edu.phuxuan.elib.catalog.BookCopyRepository;
import vn.edu.phuxuan.elib.catalog.BookCopyStatus;
import vn.edu.phuxuan.elib.catalog.BookTitle;
import vn.edu.phuxuan.elib.catalog.BookTitleRepository;
import vn.edu.phuxuan.elib.circulation.Borrow;
import vn.edu.phuxuan.elib.circulation.BorrowRepository;
import vn.edu.phuxuan.elib.circulation.BorrowStatus;
import vn.edu.phuxuan.elib.digital.DigitalDocumentRepository;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.reading.DigitalReadingSessionRepository;
import vn.edu.phuxuan.elib.reading.DigitalReadingSummaryRepository;
import vn.edu.phuxuan.elib.report.dto.DashboardSummaryDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private BookTitleRepository bookTitleRepository;
    @Mock
    private BookCopyRepository bookCopyRepository;
    @Mock
    private BorrowRepository borrowRepository;
    @Mock
    private DigitalDocumentRepository digitalDocumentRepository;
    @Mock
    private DigitalReadingSessionRepository digitalReadingSessionRepository;
    @Mock
    private DigitalReadingSummaryRepository digitalReadingSummaryRepository;
    @Mock
    private AppUserRepository appUserRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                bookTitleRepository,
                bookCopyRepository,
                borrowRepository,
                digitalDocumentRepository,
                digitalReadingSessionRepository,
                digitalReadingSummaryRepository,
                appUserRepository
        );
    }

    @Test
    @DisplayName("Tổng hợp dữ liệu Dashboard KPI thành công")
    void testGetDashboardSummary() {
        when(bookTitleRepository.count()).thenReturn(150L);
        when(bookCopyRepository.count()).thenReturn(500L);
        when(bookCopyRepository.countByStatus(BookCopyStatus.AVAILABLE)).thenReturn(350L);
        when(bookCopyRepository.countByStatus(BookCopyStatus.BORROWED)).thenReturn(100L);

        when(digitalDocumentRepository.count()).thenReturn(80L);
        when(digitalReadingSessionRepository.count()).thenReturn(450L);
        when(digitalReadingSummaryRepository.sumTotalActiveSeconds()).thenReturn(72000L); // 20 hours

        when(borrowRepository.countByStatus(BorrowStatus.BORROWED)).thenReturn(100L);
        when(borrowRepository.countByStatusAndDueAtBefore(eq(BorrowStatus.BORROWED), any())).thenReturn(15L);
        when(borrowRepository.countByStatus(BorrowStatus.RETURNED)).thenReturn(890L);
        when(borrowRepository.sumUnpaidFines()).thenReturn(BigDecimal.valueOf(75000));

        when(appUserRepository.count()).thenReturn(1200L);
        when(appUserRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(1150L);
        when(appUserRepository.countByRole(UserRole.STUDENT)).thenReturn(1100L);
        when(appUserRepository.countByRole(UserRole.LECTURER)).thenReturn(80L);

        // Top books
        java.util.List<Object[]> rawTopBooks = new java.util.ArrayList<>();
        rawTopBooks.add(new Object[]{1L, "Lập trình Spring Boot", "Nguyễn Văn A", 45L});
        when(borrowRepository.findTopBorrowedBooks(PageRequest.of(0, 5))).thenReturn(rawTopBooks);

        // Top docs
        java.util.List<Object[]> rawTopDocs = new java.util.ArrayList<>();
        rawTopDocs.add(new Object[]{10L, "Tài liệu React Native", "Trần B", 18000L, 35L});
        when(digitalReadingSummaryRepository.findTopReadDocuments(PageRequest.of(0, 5))).thenReturn(rawTopDocs);

        // Recent borrows
        AppUser user = new AppUser("123", "sv@phuxuan.edu.vn", "Nguyễn C", UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "studentCode", "SV001");

        BookTitle bt = new BookTitle("Java Core", "James Gosling", "NXB Trẻ", "978123", (short) 2022, null);
        ReflectionTestUtils.setField(bt, "id", 10L);

        BookCopy copy = new BookCopy(bt, null, "BC999", "Kệ 1", BookCopyStatus.BORROWED);
        ReflectionTestUtils.setField(copy, "id", 100L);

        Borrow borrow = new Borrow(user, copy, null, Instant.now(), Instant.now().plusSeconds(86400 * 14), BigDecimal.ZERO);
        ReflectionTestUtils.setField(borrow, "id", 500L);
        when(borrowRepository.findRecentBorrows(PageRequest.of(0, 5))).thenReturn(List.of(borrow));

        DashboardSummaryDto summary = dashboardService.getDashboardSummary();
        assertNotNull(summary);

        // Verify book stats
        assertEquals(150, summary.books().totalTitles());
        assertEquals(500, summary.books().totalCopies());
        assertEquals(350, summary.books().availableCopies());
        assertEquals(100, summary.books().borrowedCopies());
        assertEquals(50, summary.books().maintenanceOrDamagedCopies());

        // Verify digital stats
        assertEquals(80, summary.digital().totalDocuments());
        assertEquals(450, summary.digital().totalReadingSessions());
        assertEquals(20.0, summary.digital().totalReadingHours());

        // Verify circulation stats
        assertEquals(100, summary.circulation().activeBorrows());
        assertEquals(15, summary.circulation().overdueBorrows());
        assertEquals(890, summary.circulation().returnedBorrows());
        assertEquals(BigDecimal.valueOf(75000), summary.circulation().totalUnpaidFines());

        // Verify user stats
        assertEquals(1200, summary.users().totalUsers());
        assertEquals(1150, summary.users().activeUsers());
        assertEquals(1100, summary.users().studentUsers());
        assertEquals(80, summary.users().lecturerUsers());

        // Verify top lists
        assertEquals(1, summary.topBorrowedBooks().size());
        assertEquals("Lập trình Spring Boot", summary.topBorrowedBooks().get(0).title());
        assertEquals(1, summary.topReadDocuments().size());
        assertEquals("Tài liệu React Native", summary.topReadDocuments().get(0).title());
        assertEquals(1, summary.recentBorrows().size());
        assertEquals("SV001", summary.recentBorrows().get(0).studentCode());
    }
}
