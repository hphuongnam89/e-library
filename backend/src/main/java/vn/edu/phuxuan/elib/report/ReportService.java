package vn.edu.phuxuan.elib.report;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phuxuan.elib.catalog.BookCopy;
import vn.edu.phuxuan.elib.catalog.BookCopyRepository;
import vn.edu.phuxuan.elib.catalog.BookCopyStatus;
import vn.edu.phuxuan.elib.circulation.Borrow;
import vn.edu.phuxuan.elib.circulation.BorrowRepository;
import vn.edu.phuxuan.elib.circulation.BorrowStatus;
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

@Service
@Transactional(readOnly = true)
public class ReportService {

    public static final Set<String> ALLOWED_REPORTS = Set.of("books", "borrows", "reading", "users");

    private final BookCopyRepository bookCopyRepository;
    private final BorrowRepository borrowRepository;
    private final DigitalReadingSummaryRepository digitalReadingSummaryRepository;
    private final AppUserRepository appUserRepository;
    private final ExcelExportService excelExportService;

    public ReportService(
            BookCopyRepository bookCopyRepository,
            BorrowRepository borrowRepository,
            DigitalReadingSummaryRepository digitalReadingSummaryRepository,
            AppUserRepository appUserRepository,
            ExcelExportService excelExportService
    ) {
        this.bookCopyRepository = bookCopyRepository;
        this.borrowRepository = borrowRepository;
        this.digitalReadingSummaryRepository = digitalReadingSummaryRepository;
        this.appUserRepository = appUserRepository;
        this.excelExportService = excelExportService;
    }

    public void validateReportType(String reportType) {
        if (reportType == null || !ALLOWED_REPORTS.contains(reportType.toLowerCase())) {
            throw new IllegalArgumentException("Báo cáo không hợp lệ: '" + reportType
                    + "'. Chỉ chấp nhận: " + String.join(", ", ALLOWED_REPORTS));
        }
    }

    public Page<BookReportItemDto> getBooksReport(
            BookCopyStatus status,
            Long categoryId,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        Page<BookCopy> copies = bookCopyRepository.findReportCopies(status, categoryId, from, to, pageable);
        return copies.map(this::toBookReportItem);
    }

    public Page<BorrowReportItemDto> getBorrowsReport(
            BorrowStatus status,
            Boolean overdue,
            Long departmentId,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        Instant now = Instant.now();
        Page<Borrow> borrows = borrowRepository.findReportBorrows(status, overdue, now, departmentId, from, to, pageable);
        return borrows.map(this::toBorrowReportItem);
    }

    public Page<ReadingReportItemDto> getReadingReport(
            Long documentId,
            Long departmentId,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        Page<DigitalReadingSummary> summaries = digitalReadingSummaryRepository.findReportSummaries(
                documentId, departmentId, from, to, pageable
        );
        return summaries.map(this::toReadingReportItem);
    }

    public Page<UserReportItemDto> getUsersReport(
            UserRole role,
            UserStatus status,
            Long departmentId,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        Page<AppUser> users = appUserRepository.findReportUsers(role, status, departmentId, from, to, pageable);
        return users.map(this::toUserReportItem);
    }

    public byte[] exportReportXlsx(
            String reportType,
            String statusStr,
            Long categoryId,
            Long departmentId,
            Long documentId,
            String roleStr,
            Boolean overdue,
            Instant from,
            Instant to
    ) throws IOException {
        validateReportType(reportType);
        Pageable maxPage = PageRequest.of(0, ExcelExportService.MAX_EXPORT_ROWS);

        return switch (reportType.toLowerCase()) {
            case "books" -> {
                BookCopyStatus status = parseEnum(BookCopyStatus.class, statusStr);
                List<BookReportItemDto> data = getBooksReport(status, categoryId, from, to, maxPage).getContent();
                yield excelExportService.exportBooks(data);
            }
            case "borrows" -> {
                BorrowStatus status = parseEnum(BorrowStatus.class, statusStr);
                List<BorrowReportItemDto> data = getBorrowsReport(status, overdue, departmentId, from, to, maxPage).getContent();
                yield excelExportService.exportBorrows(data);
            }
            case "reading" -> {
                List<ReadingReportItemDto> data = getReadingReport(documentId, departmentId, from, to, maxPage).getContent();
                yield excelExportService.exportReading(data);
            }
            case "users" -> {
                UserRole role = parseEnum(UserRole.class, roleStr);
                UserStatus status = parseEnum(UserStatus.class, statusStr);
                List<UserReportItemDto> data = getUsersReport(role, status, departmentId, from, to, maxPage).getContent();
                yield excelExportService.exportUsers(data);
            }
            default -> throw new IllegalArgumentException("Loại báo cáo không hỗ trợ: " + reportType);
        };
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private BookReportItemDto toBookReportItem(BookCopy bc) {
        return new BookReportItemDto(
                bc.getId(),
                bc.getBookTitle() != null ? bc.getBookTitle().getId() : null,
                bc.getBarcode(),
                bc.getBookTitle() != null ? bc.getBookTitle().getTitle() : null,
                bc.getBookTitle() != null ? bc.getBookTitle().getAuthor() : null,
                bc.getBookTitle() != null ? bc.getBookTitle().getIsbn() : null,
                bc.getBookTitle() != null && bc.getBookTitle().getCategory() != null
                        ? bc.getBookTitle().getCategory().getName() : null,
                bc.getStatus() != null ? bc.getStatus().name() : null,
                bc.getLibrary() != null ? bc.getLibrary().getName() : null,
                bc.getCreatedAt()
        );
    }

    private BorrowReportItemDto toBorrowReportItem(Borrow b) {
        return new BorrowReportItemDto(
                b.getId(),
                b.getUser() != null ? b.getUser().getStudentCode() : null,
                b.getUser() != null ? b.getUser().getFullName() : null,
                b.getUser() != null && b.getUser().getDepartment() != null
                        ? b.getUser().getDepartment().getName() : null,
                b.getBookCopy() != null && b.getBookCopy().getBookTitle() != null
                        ? b.getBookCopy().getBookTitle().getTitle() : null,
                b.getBookCopy() != null ? b.getBookCopy().getBarcode() : null,
                b.getBorrowedAt(),
                b.getDueAt(),
                b.getReturnedAt(),
                b.getStatus() != null ? b.getStatus().name() : null,
                b.getFineAmount(),
                b.getFinePaidAt()
        );
    }

    private ReadingReportItemDto toReadingReportItem(DigitalReadingSummary s) {
        return new ReadingReportItemDto(
                s.getDocument() != null ? s.getDocument().getId() : null,
                s.getDocument() != null ? s.getDocument().getTitle() : null,
                s.getUser() != null ? s.getUser().getId() : null,
                s.getUser() != null ? s.getUser().getStudentCode() : null,
                s.getUser() != null ? s.getUser().getFullName() : null,
                s.getUser() != null && s.getUser().getDepartment() != null
                        ? s.getUser().getDepartment().getName() : null,
                s.getActiveSeconds(),
                s.getSessionCount(),
                s.getLastActiveAt()
        );
    }

    private UserReportItemDto toUserReportItem(AppUser u) {
        long activeBorrows = borrowRepository.countByUserIdAndStatus(u.getId(), BorrowStatus.BORROWED);
        return new UserReportItemDto(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getStudentCode(),
                u.getRole() != null ? u.getRole().name() : null,
                u.getStatus() != null ? u.getStatus().name() : null,
                u.getDepartment() != null ? u.getDepartment().getName() : null,
                activeBorrows,
                0L,
                u.getCreatedAt()
        );
    }
}
