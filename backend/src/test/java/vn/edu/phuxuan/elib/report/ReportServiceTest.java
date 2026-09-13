package vn.edu.phuxuan.elib.report;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.edu.phuxuan.elib.catalog.BookCopy;
import vn.edu.phuxuan.elib.catalog.BookCopyRepository;
import vn.edu.phuxuan.elib.catalog.BookCopyStatus;
import vn.edu.phuxuan.elib.catalog.BookTitle;
import vn.edu.phuxuan.elib.circulation.Borrow;
import vn.edu.phuxuan.elib.circulation.BorrowRepository;
import vn.edu.phuxuan.elib.circulation.BorrowStatus;
import vn.edu.phuxuan.elib.digital.DigitalDocument;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.reading.DigitalReadingSummary;
import vn.edu.phuxuan.elib.reading.DigitalReadingSummaryRepository;
import vn.edu.phuxuan.elib.report.dto.BookReportItemDto;
import vn.edu.phuxuan.elib.report.dto.BorrowReportItemDto;
import vn.edu.phuxuan.elib.report.dto.ReadingReportItemDto;
import vn.edu.phuxuan.elib.report.dto.UserReportItemDto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private BookCopyRepository bookCopyRepository;
    @Mock
    private BorrowRepository borrowRepository;
    @Mock
    private DigitalReadingSummaryRepository digitalReadingSummaryRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private ExcelExportService excelExportService;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                bookCopyRepository,
                borrowRepository,
                digitalReadingSummaryRepository,
                appUserRepository,
                excelExportService
        );
    }

    @Test
    @DisplayName("Kiểm tra whitelist tên báo cáo: hợp lệ và không hợp lệ")
    void testValidateReportType() {
        assertDoesNotThrow(() -> reportService.validateReportType("books"));
        assertDoesNotThrow(() -> reportService.validateReportType("borrows"));
        assertDoesNotThrow(() -> reportService.validateReportType("reading"));
        assertDoesNotThrow(() -> reportService.validateReportType("users"));
        assertDoesNotThrow(() -> reportService.validateReportType("BOOKS"));

        assertThrows(IllegalArgumentException.class, () -> reportService.validateReportType("invalid"));
        assertThrows(IllegalArgumentException.class, () -> reportService.validateReportType(null));
        assertThrows(IllegalArgumentException.class, () -> reportService.validateReportType(""));
    }

    @Test
    @DisplayName("Lấy dữ liệu báo cáo Sách")
    void testGetBooksReport() {
        BookTitle title = new BookTitle("Title A", "Author A", "Publisher", "ISBN123", (short) 2020, null);
        org.springframework.test.util.ReflectionTestUtils.setField(title, "id", 1L);
        BookCopy copy = new BookCopy(title, null, "BC001", "Kệ 1", BookCopyStatus.AVAILABLE);
        org.springframework.test.util.ReflectionTestUtils.setField(copy, "id", 10L);
        Page<BookCopy> copyPage = new PageImpl<>(List.of(copy));

        when(bookCopyRepository.findReportCopies(eq(BookCopyStatus.AVAILABLE), eq(1L), any(), any(), any()))
                .thenReturn(copyPage);

        Page<BookReportItemDto> result = reportService.getBooksReport(
                BookCopyStatus.AVAILABLE, 1L, null, null, PageRequest.of(0, 10)
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("BC001", result.getContent().get(0).barcode());
        assertEquals("Title A", result.getContent().get(0).title());
    }

    @Test
    @DisplayName("Lấy dữ liệu báo cáo Mượn - Trả")
    void testGetBorrowsReport() {
        AppUser user = new AppUser("123", "sv@phuxuan.edu.vn", "Nguyễn C", UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "studentCode", "SV001");

        BookTitle bt = new BookTitle("Java Core", "James Gosling", "NXB Trẻ", "978123", (short) 2022, null);
        org.springframework.test.util.ReflectionTestUtils.setField(bt, "id", 10L);

        BookCopy copy = new BookCopy(bt, null, "BC999", "Kệ 1", BookCopyStatus.BORROWED);
        org.springframework.test.util.ReflectionTestUtils.setField(copy, "id", 100L);

        Borrow borrow = new Borrow(user, copy, null, Instant.now(), Instant.now().plusSeconds(86400 * 14), null);
        org.springframework.test.util.ReflectionTestUtils.setField(borrow, "id", 500L);
        Page<Borrow> borrowPage = new PageImpl<>(List.of(borrow));

        when(borrowRepository.findReportBorrows(eq(BorrowStatus.BORROWED), eq(false), any(), eq(2L), any(), any(), any()))
                .thenReturn(borrowPage);

        Page<BorrowReportItemDto> result = reportService.getBorrowsReport(
                BorrowStatus.BORROWED, false, 2L, null, null, PageRequest.of(0, 10)
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("SV001", result.getContent().get(0).studentCode());
    }

    @Test
    @DisplayName("Lấy dữ liệu báo cáo Đọc tài liệu số")
    void testGetReadingReport() {
        DigitalDocument doc = new DigitalDocument(null, "Doc A", "Desc", "Publisher", null, "storage-key", "application/pdf", 1024L, vn.edu.phuxuan.elib.digital.DigitalDocumentPermission.AUTHENTICATED);
        org.springframework.test.util.ReflectionTestUtils.setField(doc, "id", 5L);

        AppUser user = new AppUser("123", "sv@phuxuan.edu.vn", "Nguyễn C", UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "studentCode", "SV001");

        DigitalReadingSummary summary = new DigitalReadingSummary(user, doc);
        summary.setActiveSeconds(120L);
        summary.setSessionCount(2L);

        when(digitalReadingSummaryRepository.findReportSummaries(eq(5L), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(summary)));

        Page<ReadingReportItemDto> result = reportService.getReadingReport(5L, null, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Doc A", result.getContent().get(0).documentTitle());
        assertEquals(120L, result.getContent().get(0).totalActiveSeconds());
    }

    @Test
    @DisplayName("Lấy dữ liệu báo cáo Độc giả")
    void testGetUsersReport() {
        AppUser user = new AppUser("123", "sv@phuxuan.edu.vn", "Nguyễn C", UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "studentCode", "SV001");

        when(appUserRepository.findReportUsers(eq(UserRole.STUDENT), eq(UserStatus.ACTIVE), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(borrowRepository.countByUserIdAndStatus(any(), eq(BorrowStatus.BORROWED))).thenReturn(2L);

        Page<UserReportItemDto> result = reportService.getUsersReport(
                UserRole.STUDENT, UserStatus.ACTIVE, null, null, null, PageRequest.of(0, 10)
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("SV001", result.getContent().get(0).studentCode());
        assertEquals(2L, result.getContent().get(0).activeBorrowsCount());
    }

    @Test
    @DisplayName("Xuất báo cáo Excel delegating tới ExcelExportService")
    void testExportReportXlsx() throws IOException {
        BookTitle title = new BookTitle("Title A", "Author A", "Publisher", "ISBN123", (short) 2020, null);
        BookCopy copy = new BookCopy(title, null, "BC001", "Kệ 1", BookCopyStatus.AVAILABLE);
        Page<BookCopy> copyPage = new PageImpl<>(List.of(copy));

        when(bookCopyRepository.findReportCopies(any(), any(), any(), any(), any())).thenReturn(copyPage);
        when(excelExportService.exportBooks(any())).thenReturn(new byte[]{1, 2, 3});

        byte[] result = reportService.exportReportXlsx(
                "books", "AVAILABLE", null, null, null, null, null, null, null
        );

        assertNotNull(result);
        assertEquals(3, result.length);
        verify(excelExportService).exportBooks(any());
    }
}
