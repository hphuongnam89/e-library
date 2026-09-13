package vn.edu.phuxuan.elib.report;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.phuxuan.elib.report.dto.BookReportItemDto;
import vn.edu.phuxuan.elib.report.dto.BorrowReportItemDto;
import vn.edu.phuxuan.elib.report.dto.ReadingReportItemDto;
import vn.edu.phuxuan.elib.report.dto.UserReportItemDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelExportServiceTest {

    private ExcelExportService service;

    @BeforeEach
    void setUp() {
        service = new ExcelExportService();
    }

    @Test
    @DisplayName("Chống Formula Injection: Tiền tố ký tự ' khi bắt đầu bằng =, +, -, @, tab")
    void testFormulaInjectionSanitizer() {
        assertEquals("", service.sanitize(null));
        assertEquals("Sách Lập trình Java", service.sanitize("Sách Lập trình Java"));
        assertEquals("'=1+1", service.sanitize("=1+1"));
        assertEquals("'=SUM(A1:A10)", service.sanitize("=SUM(A1:A10)"));
        assertEquals("'+84912345678", service.sanitize("+84912345678"));
        assertEquals("'-5000", service.sanitize("-5000"));
        assertEquals("'@admin", service.sanitize("@admin"));
        assertEquals("'\tcmd", service.sanitize("\tcmd"));
    }

    @Test
    @DisplayName("Xuất báo cáo Sách thành file XLSX hợp lệ")
    void testExportBooks() throws IOException {
        BookReportItemDto item = new BookReportItemDto(
                10L, 1L, "=BC001", "Clean Architecture", "Robert C. Martin",
                "9780134494166", "Công nghệ thông tin", "AVAILABLE", "Thư viện Cơ sở 1", Instant.now()
        );

        byte[] bytes = service.exportBooks(List.of(item));
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheet("Danh mục Sách");
            assertNotNull(sheet);
            assertEquals(1, sheet.getLastRowNum()); // 0: header, 1: row 1

            XSSFRow row1 = sheet.getRow(1);
            assertEquals("1", row1.getCell(0).getStringCellValue());
            assertEquals("10", row1.getCell(1).getStringCellValue());
            // Check formula injection sanitization on barcode
            assertEquals("'=BC001", row1.getCell(2).getStringCellValue());
            assertEquals("Clean Architecture", row1.getCell(3).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Xuất báo cáo Mượn - Trả thành file XLSX hợp lệ")
    void testExportBorrows() throws IOException {
        BorrowReportItemDto item = new BorrowReportItemDto(
                100L, "SV001", "Nguyễn Văn A", "Khoa CNTT",
                "Lập trình Java", "BC12345", Instant.now(), Instant.now().plusSeconds(86400 * 14),
                null, "BORROWED", BigDecimal.valueOf(15000), null
        );

        byte[] bytes = service.exportBorrows(List.of(item));
        assertNotNull(bytes);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheet("Lưu thông Mượn Trả");
            assertNotNull(sheet);
            assertEquals(1, sheet.getLastRowNum());
            XSSFRow row1 = sheet.getRow(1);
            assertEquals("SV001", row1.getCell(2).getStringCellValue());
            assertEquals("15000", row1.getCell(11).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Xuất báo cáo Đọc tài liệu số thành file XLSX hợp lệ")
    void testExportReading() throws IOException {
        ReadingReportItemDto item = new ReadingReportItemDto(
                50L, "Giáo trình CSDL", 1L, "SV001", "Nguyễn Văn A", "Khoa CNTT",
                3600L, 5L, Instant.now()
        );

        byte[] bytes = service.exportReading(List.of(item));
        assertNotNull(bytes);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheet("Đọc Tài liệu số");
            assertNotNull(sheet);
            assertEquals(1, sheet.getLastRowNum());
            XSSFRow row1 = sheet.getRow(1);
            assertEquals("60", row1.getCell(7).getStringCellValue()); // 3600s = 60 mins
        }
    }

    @Test
    @DisplayName("Xuất báo cáo Độc giả thành file XLSX hợp lệ")
    void testExportUsers() throws IOException {
        UserReportItemDto item = new UserReportItemDto(
                1L, "sv@phuxuan.edu.vn", "Trần Thị B", "SV002", "STUDENT", "ACTIVE",
                "Khoa Ngoại ngữ", 2L, 7200L, Instant.now()
        );

        byte[] bytes = service.exportUsers(List.of(item));
        assertNotNull(bytes);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheet("Độc giả");
            assertNotNull(sheet);
            assertEquals(1, sheet.getLastRowNum());
            XSSFRow row1 = sheet.getRow(1);
            assertEquals("sv@phuxuan.edu.vn", row1.getCell(2).getStringCellValue());
            assertEquals("2.0", row1.getCell(9).getStringCellValue()); // 7200s = 2.0 hours
        }
    }
}
