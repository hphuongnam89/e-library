package vn.edu.phuxuan.elib.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.phuxuan.elib.report.dto.BookReportItemDto;
import vn.edu.phuxuan.elib.report.dto.DashboardSummaryDto;
import vn.edu.phuxuan.elib.web.GlobalExceptionHandler;
import vn.edu.phuxuan.elib.web.ProblemResponses;
import vn.edu.phuxuan.elib.web.RequestIdFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportsContractTest {

    private MockMvc mvc;

    @Mock
    private ReportService reportService;

    @Mock
    private DashboardService dashboardService;

    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        ReportController reportController = new ReportController(reportService);
        DashboardController dashboardController = new DashboardController(dashboardService);

        mvc = MockMvcBuilders.standaloneSetup(reportController, dashboardController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler(new ProblemResponses(mapper)))
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    @DisplayName("Contract GET /api/v1/dashboard/summary trả về đầy đủ KPI thư viện")
    void summaryContract() throws Exception {
        DashboardSummaryDto.BookStats books = new DashboardSummaryDto.BookStats(100, 250, 200, 40, 10);
        DashboardSummaryDto.DigitalStats digital = new DashboardSummaryDto.DigitalStats(30, 150, 2.0);
        DashboardSummaryDto.CirculationStats borrows = new DashboardSummaryDto.CirculationStats(40, 5, 500, BigDecimal.ZERO);
        DashboardSummaryDto.UserStats users = new DashboardSummaryDto.UserStats(1000, 950, 900, 50);

        DashboardSummaryDto summary = new DashboardSummaryDto(
                books, digital, borrows, users, List.of(), List.of(), List.of()
        );
        when(dashboardService.getDashboardSummary()).thenReturn(summary);

        mvc.perform(get("/api/v1/dashboard/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books.totalTitles").value(100))
                .andExpect(jsonPath("$.books.totalCopies").value(250))
                .andExpect(jsonPath("$.circulation.activeBorrows").value(40))
                .andExpect(jsonPath("$.digital.totalDocuments").value(30))
                .andExpect(jsonPath("$.users.totalUsers").value(1000));
    }

    @Test
    @DisplayName("Contract GET /api/v1/reports/books trả về dữ liệu báo cáo phân trang")
    void reportDataContract() throws Exception {
        BookReportItemDto item = new BookReportItemDto(
                10L, 1L, "PXU-001", "Kỹ nghệ phần mềm", "Tác giả Y",
                "123-456", "CNTT", "AVAILABLE", "Thư viện 1", Instant.now()
        );
        when(reportService.getBooksReport(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mvc.perform(get("/api/v1/reports/books").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].barcode").value("PXU-001"))
                .andExpect(jsonPath("$.content[0].title").value("Kỹ nghệ phần mềm"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Contract GET /api/v1/reports/books/export xuất file Excel kèm Content-Disposition")
    void exportExcelContract() throws Exception {
        byte[] fakeExcel = new byte[]{0x50, 0x4B, 0x03, 0x04}; // standard zip/xlsx magic bytes
        when(reportService.exportReportXlsx(eq("books"), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(fakeExcel);

        mvc.perform(get("/api/v1/reports/books/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().exists("Content-Disposition"));
    }
}
