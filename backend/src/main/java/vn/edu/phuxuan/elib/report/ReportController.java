package vn.edu.phuxuan.elib.report;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.phuxuan.elib.catalog.BookCopyStatus;
import vn.edu.phuxuan.elib.circulation.BorrowStatus;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{report}")
    public ResponseEntity<?> getReportData(
            @PathVariable("report") String report,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "documentId", required = false) Long documentId,
            @RequestParam(name = "overdue", required = false) Boolean overdue,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        reportService.validateReportType(report);

        return switch (report.toLowerCase()) {
            case "books" -> {
                BookCopyStatus copyStatus = parseEnum(BookCopyStatus.class, status);
                yield ResponseEntity.ok(reportService.getBooksReport(copyStatus, categoryId, from, to, pageable));
            }
            case "borrows" -> {
                BorrowStatus borrowStatus = parseEnum(BorrowStatus.class, status);
                yield ResponseEntity.ok(reportService.getBorrowsReport(borrowStatus, overdue, departmentId, from, to, pageable));
            }
            case "reading" -> ResponseEntity.ok(reportService.getReadingReport(documentId, departmentId, from, to, pageable));
            case "users" -> {
                UserRole userRole = parseEnum(UserRole.class, role);
                UserStatus userStatus = parseEnum(UserStatus.class, status);
                yield ResponseEntity.ok(reportService.getUsersReport(userRole, userStatus, departmentId, from, to, pageable));
            }
            default -> throw new IllegalArgumentException("Báo cáo không hợp lệ: " + report);
        };
    }

    @GetMapping("/{report}/export")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable("report") String report,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "documentId", required = false) Long documentId,
            @RequestParam(name = "overdue", required = false) Boolean overdue,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) throws IOException {
        reportService.validateReportType(report);

        byte[] excelBytes = reportService.exportReportXlsx(
                report, status, categoryId, departmentId, documentId, role, overdue, from, to
        );

        String filename = "report-" + report.toLowerCase() + "-" + FILE_DATE_FORMATTER.format(Instant.now()) + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(excelBytes);
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
}
