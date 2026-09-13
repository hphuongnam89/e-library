package vn.edu.phuxuan.elib.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import vn.edu.phuxuan.elib.report.dto.BookReportItemDto;
import vn.edu.phuxuan.elib.report.dto.BorrowReportItemDto;
import vn.edu.phuxuan.elib.report.dto.ReadingReportItemDto;
import vn.edu.phuxuan.elib.report.dto.UserReportItemDto;

@Service
public class ExcelExportService {

    public static final int MAX_EXPORT_ROWS = 10000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    /**
     * Sanitizes string to prevent CSV/Excel Formula Injection attacks.
     * Any string starting with '=', '+', '-', '@', '\t', '\r' is prepended with a single quote.
     */
    public String sanitize(String value) {
        if (value == null) {
            return "";
        }
        if (!value.isEmpty()) {
            char rawFirst = value.charAt(0);
            if (rawFirst == '\t' || rawFirst == '\r' || rawFirst == '\n') {
                return "'" + value;
            }
        }
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) {
            char firstChar = trimmed.charAt(0);
            if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@') {
                return "'" + value;
            }
        }
        return value;
    }

    private String formatDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DATE_FORMATTER.format(instant);
    }

    public byte[] exportBooks(List<BookReportItemDto> items) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Danh mục Sách");
            XSSFCellStyle headerStyle = createHeaderStyle(workbook);
            XSSFCellStyle dataStyle = createDataStyle(workbook);

            String[] headers = {
                    "STT", "Mã bản sao", "Barcode", "Tên sách", "Tác giả",
                    "ISBN", "Thể loại", "Trạng thái", "Thư viện", "Ngày tạo"
            };

            createHeaderRow(sheet, headers, headerStyle);

            int rowIdx = 1;
            int limit = Math.min(items.size(), MAX_EXPORT_ROWS);
            for (int i = 0; i < limit; i++) {
                BookReportItemDto item = items.get(i);
                Row row = sheet.createRow(rowIdx++);
                createCell(row, 0, String.valueOf(i + 1), dataStyle);
                createCell(row, 1, String.valueOf(item.copyId()), dataStyle);
                createCell(row, 2, sanitize(item.barcode()), dataStyle);
                createCell(row, 3, sanitize(item.title()), dataStyle);
                createCell(row, 4, sanitize(item.author()), dataStyle);
                createCell(row, 5, sanitize(item.isbn()), dataStyle);
                createCell(row, 6, sanitize(item.categoryName()), dataStyle);
                createCell(row, 7, sanitize(item.status()), dataStyle);
                createCell(row, 8, sanitize(item.libraryName()), dataStyle);
                createCell(row, 9, formatDate(item.createdAt()), dataStyle);
            }

            autoSizeColumns(sheet, headers.length);
            return toByteArray(workbook);
        }
    }

    public byte[] exportBorrows(List<BorrowReportItemDto> items) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Lưu thông Mượn Trả");
            XSSFCellStyle headerStyle = createHeaderStyle(workbook);
            XSSFCellStyle dataStyle = createDataStyle(workbook);

            String[] headers = {
                    "STT", "Mã mượn", "Mã SV/CB", "Họ và tên", "Khoa / Đơn vị",
                    "Tên sách", "Mã vạch", "Ngày mượn", "Hạn trả", "Ngày trả",
                    "Trạng thái", "Tiền phạt (VND)", "Ngày đóng phạt"
            };

            createHeaderRow(sheet, headers, headerStyle);

            int rowIdx = 1;
            int limit = Math.min(items.size(), MAX_EXPORT_ROWS);
            for (int i = 0; i < limit; i++) {
                BorrowReportItemDto item = items.get(i);
                Row row = sheet.createRow(rowIdx++);
                createCell(row, 0, String.valueOf(i + 1), dataStyle);
                createCell(row, 1, String.valueOf(item.borrowId()), dataStyle);
                createCell(row, 2, sanitize(item.studentCode()), dataStyle);
                createCell(row, 3, sanitize(item.userName()), dataStyle);
                createCell(row, 4, sanitize(item.departmentName()), dataStyle);
                createCell(row, 5, sanitize(item.bookTitle()), dataStyle);
                createCell(row, 6, sanitize(item.barcode()), dataStyle);
                createCell(row, 7, formatDate(item.borrowedAt()), dataStyle);
                createCell(row, 8, formatDate(item.dueAt()), dataStyle);
                createCell(row, 9, formatDate(item.returnedAt()), dataStyle);
                createCell(row, 10, sanitize(item.status()), dataStyle);
                createCell(row, 11, item.fineAmount() != null ? item.fineAmount().toPlainString() : "0", dataStyle);
                createCell(row, 12, formatDate(item.finePaidAt()), dataStyle);
            }

            autoSizeColumns(sheet, headers.length);
            return toByteArray(workbook);
        }
    }

    public byte[] exportReading(List<ReadingReportItemDto> items) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Đọc Tài liệu số");
            XSSFCellStyle headerStyle = createHeaderStyle(workbook);
            XSSFCellStyle dataStyle = createDataStyle(workbook);

            String[] headers = {
                    "STT", "Mã tài liệu", "Tên tài liệu", "Mã độc giả", "Mã SV/CB",
                    "Họ và tên", "Khoa / Đơn vị", "Thời gian đọc (phút)", "Số phiên", "Lần đọc cuối"
            };

            createHeaderRow(sheet, headers, headerStyle);

            int rowIdx = 1;
            int limit = Math.min(items.size(), MAX_EXPORT_ROWS);
            for (int i = 0; i < limit; i++) {
                ReadingReportItemDto item = items.get(i);
                Row row = sheet.createRow(rowIdx++);
                long minutes = Math.max(1, item.totalActiveSeconds() / 60);
                createCell(row, 0, String.valueOf(i + 1), dataStyle);
                createCell(row, 1, String.valueOf(item.documentId()), dataStyle);
                createCell(row, 2, sanitize(item.documentTitle()), dataStyle);
                createCell(row, 3, String.valueOf(item.userId()), dataStyle);
                createCell(row, 4, sanitize(item.studentCode()), dataStyle);
                createCell(row, 5, sanitize(item.userName()), dataStyle);
                createCell(row, 6, sanitize(item.departmentName()), dataStyle);
                createCell(row, 7, String.valueOf(minutes), dataStyle);
                createCell(row, 8, String.valueOf(item.sessionCount()), dataStyle);
                createCell(row, 9, formatDate(item.lastActiveAt()), dataStyle);
            }

            autoSizeColumns(sheet, headers.length);
            return toByteArray(workbook);
        }
    }

    public byte[] exportUsers(List<UserReportItemDto> items) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Độc giả");
            XSSFCellStyle headerStyle = createHeaderStyle(workbook);
            XSSFCellStyle dataStyle = createDataStyle(workbook);

            String[] headers = {
                    "STT", "Mã người dùng", "Email", "Họ và tên", "Mã SV/CB",
                    "Vai trò", "Trạng thái", "Khoa / Đơn vị", "Sách đang mượn", "Tổng giờ đọc", "Ngày tham gia"
            };

            createHeaderRow(sheet, headers, headerStyle);

            int rowIdx = 1;
            int limit = Math.min(items.size(), MAX_EXPORT_ROWS);
            for (int i = 0; i < limit; i++) {
                UserReportItemDto item = items.get(i);
                Row row = sheet.createRow(rowIdx++);
                double hours = Math.round((item.totalReadingSeconds() / 3600.0) * 10.0) / 10.0;
                createCell(row, 0, String.valueOf(i + 1), dataStyle);
                createCell(row, 1, String.valueOf(item.userId()), dataStyle);
                createCell(row, 2, sanitize(item.email()), dataStyle);
                createCell(row, 3, sanitize(item.fullName()), dataStyle);
                createCell(row, 4, sanitize(item.studentCode()), dataStyle);
                createCell(row, 5, sanitize(item.role()), dataStyle);
                createCell(row, 6, sanitize(item.status()), dataStyle);
                createCell(row, 7, sanitize(item.departmentName()), dataStyle);
                createCell(row, 8, String.valueOf(item.activeBorrowsCount()), dataStyle);
                createCell(row, 9, String.valueOf(hours), dataStyle);
                createCell(row, 10, formatDate(item.createdAt()), dataStyle);
            }

            autoSizeColumns(sheet, headers.length);
            return toByteArray(workbook);
        }
    }

    private void createHeaderRow(XSSFSheet sheet, String[] headers, XSSFCellStyle headerStyle) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(24);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createCell(Row row, int colIdx, String value, XSSFCellStyle style) {
        Cell cell = row.createCell(colIdx);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, new DefaultIndexedColorMap()));
        style.setFont(font);

        // Header Background: Dark Blue #1E3A8A
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 30, (byte) 58, (byte) 138}, new DefaultIndexedColorMap()));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private XSSFCellStyle createDataStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private void setBorders(XSSFCellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void autoSizeColumns(XSSFSheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            // Ensure minimum column width
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth < 3000) {
                sheet.setColumnWidth(i, 3500);
            }
        }
    }

    private byte[] toByteArray(XSSFWorkbook workbook) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
