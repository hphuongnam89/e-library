package vn.edu.phuxuan.elib.circulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
import vn.edu.phuxuan.elib.circulation.dto.BatchCheckoutResponse;
import vn.edu.phuxuan.elib.circulation.dto.BorrowDto;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.web.GlobalExceptionHandler;
import vn.edu.phuxuan.elib.web.ProblemResponses;
import vn.edu.phuxuan.elib.web.RequestIdFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CirculationContractTest {

    private MockMvc mvc;

    @Mock
    private CirculationService circulationService;

    @Mock
    private AppUserRepository appUserRepository;

    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        CirculationController controller = new CirculationController(circulationService, appUserRepository);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new org.springframework.data.web.PageableHandlerMethodArgumentResolver(),
                        new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver()
                )
                .setControllerAdvice(new GlobalExceptionHandler(new ProblemResponses(mapper)))
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    @DisplayName("Contract POST /api/v1/borrows (checkout sách)")
    void checkoutContract() throws Exception {
        BorrowDto b1 = new BorrowDto(
                10L, 1L, "sv@pxu.edu.vn", "SV001", "Nguyễn Văn A",
                50L, "PXU-0001", 1L, "Lập trình Java", 2L, "Thư viện 1",
                Instant.now(), Instant.now().plusSeconds(86400 * 14), null,
                BigDecimal.ZERO, BigDecimal.ZERO, null,
                BorrowStatus.BORROWED, false
        );
        BatchCheckoutResponse resp = new BatchCheckoutResponse(List.of(b1));
        when(circulationService.checkout(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/borrows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentCode\":\"SV001\",\"barcodes\":[\"PXU-0001\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].id").value(10))
                .andExpect(jsonPath("$.items[0].barcode").value("PXU-0001"))
                .andExpect(jsonPath("$.items[0].bookTitle").value("Lập trình Java"));
    }

    @Test
    @DisplayName("Contract POST /api/v1/borrows/{id}/return (trả sách)")
    void returnContract() throws Exception {
        BorrowDto returned = new BorrowDto(
                10L, 1L, "sv@pxu.edu.vn", "SV001", "Nguyễn Văn A",
                50L, "PXU-0001", 1L, "Lập trình Java", 2L, "Thư viện 1",
                Instant.now().minusSeconds(86400 * 10), Instant.now().plusSeconds(86400 * 4), Instant.now(),
                BigDecimal.ZERO, BigDecimal.ZERO, null,
                BorrowStatus.RETURNED, false
        );
        when(circulationService.returnBorrow(eq(10L))).thenReturn(returned);

        mvc.perform(post("/api/v1/borrows/10/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.fineAmount").value(0));
    }

    @Test
    @DisplayName("Contract POST /api/v1/borrows/{id}/fine-payment (thanh toán tiền phạt)")
    void payFineContract() throws Exception {
        BorrowDto paid = new BorrowDto(
                10L, 1L, "sv@pxu.edu.vn", "SV001", "Nguyễn Văn A",
                50L, "PXU-0001", 1L, "Lập trình Java", 2L, "Thư viện 1",
                Instant.now().minusSeconds(86400 * 20), Instant.now().minusSeconds(86400 * 6), Instant.now(),
                BigDecimal.ZERO, new BigDecimal("30000"), Instant.now(),
                BorrowStatus.RETURNED, false
        );
        when(circulationService.payFine(eq(10L))).thenReturn(paid);

        mvc.perform(post("/api/v1/borrows/10/fine-payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.fineAmount").value(30000))
                .andExpect(jsonPath("$.finePaidAt").isNotEmpty());
    }

    @Test
    @DisplayName("Contract GET /api/v1/me/borrows (lịch sử sách của độc giả)")
    void getMyBorrowsContract() throws Exception {
        org.springframework.security.authentication.TestingAuthenticationToken studentAuth =
                new org.springframework.security.authentication.TestingAuthenticationToken("student@pxu.edu.vn", "pwd", "ROLE_STUDENT");
        studentAuth.setAuthenticated(true);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(studentAuth);

        AppUser student = new AppUser("sub-sv", "student@pxu.edu.vn", "Học Sinh", UserRole.STUDENT);
        student.setId(5L);
        when(appUserRepository.findByEmailIgnoreCase(eq("student@pxu.edu.vn"))).thenReturn(Optional.of(student));

        BorrowDto b1 = new BorrowDto(
                12L, 5L, "student@pxu.edu.vn", "SV005", "Học Sinh",
                52L, "PXU-0002", 2L, "Cơ sở dữ liệu", 2L, "Thư viện 1",
                Instant.now(), Instant.now().plusSeconds(86400 * 14), null,
                BigDecimal.ZERO, BigDecimal.ZERO, null,
                BorrowStatus.BORROWED, false
        );
        when(circulationService.getMyBorrows(eq(5L), any())).thenReturn(new PageImpl<>(List.of(b1), PageRequest.of(0, 10), 1));

        mvc.perform(get("/api/v1/me/borrows")
                        .principal(studentAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(12))
                .andExpect(jsonPath("$.content[0].barcode").value("PXU-0002"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
