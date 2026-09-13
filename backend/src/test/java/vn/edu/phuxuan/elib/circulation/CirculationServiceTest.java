package vn.edu.phuxuan.elib.circulation;

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
import vn.edu.phuxuan.elib.catalog.BookCopy;
import vn.edu.phuxuan.elib.catalog.BookCopyRepository;
import vn.edu.phuxuan.elib.catalog.BookCopyStatus;
import vn.edu.phuxuan.elib.catalog.BookTitle;
import vn.edu.phuxuan.elib.circulation.dto.BatchCheckoutResponse;
import vn.edu.phuxuan.elib.circulation.dto.CheckoutRequest;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.organization.Campus;
import vn.edu.phuxuan.elib.organization.Institution;
import vn.edu.phuxuan.elib.organization.Library;
import vn.edu.phuxuan.elib.organization.LibraryRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CirculationServiceTest {

    @Mock
    private BorrowRepository borrowRepository;

    @Mock
    private BorrowingPolicyRepository borrowingPolicyRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private LibraryRepository libraryRepository;

    private CirculationService circulationService;

    @BeforeEach
    void setUp() {
        circulationService = new CirculationService(
                borrowRepository,
                borrowingPolicyRepository,
                bookCopyRepository,
                appUserRepository,
                libraryRepository
        );
    }

    @Test
    @DisplayName("checkout batch memoizes borrowing policy per library to avoid duplicate queries")
    void checkout_batch_memoizesPolicyPerLibrary() {
        String studentCode = "SV12345";
        AppUser user = new AppUser("sub-123", "student@pxu.edu.vn", "Nguyễn Văn Sinh", UserRole.STUDENT);
        user.setId(1L);
        user.setStatus(UserStatus.ACTIVE);

        when(appUserRepository.findByStudentCode(studentCode)).thenReturn(Optional.of(user));
        when(borrowRepository.existsByUserIdAndFineAmountGreaterThanAndFinePaidAtIsNull(eq(1L), any())).thenReturn(false);
        when(borrowRepository.countByUserIdAndStatus(1L, BorrowStatus.BORROWED)).thenReturn(0L);

        Institution institution = new Institution("Đại học Phú Xuân");
        Campus campus = new Campus(institution, "Cơ sở 1");
        Library library = new Library(campus, "Thư viện Trung tâm", "TV1");
        library.setId(10L);

        BookTitle title1 = new BookTitle("Sách 1", "Tác giả 1", "NXB 1", "111-222", (short) 2026, null);
        title1.setId(101L);
        BookCopy copy1 = new BookCopy(title1, library, "BC001", "Kệ 1", BookCopyStatus.AVAILABLE);
        copy1.setId(1001L);

        BookTitle title2 = new BookTitle("Sách 2", "Tác giả 2", "NXB 2", "111-333", (short) 2026, null);
        title2.setId(102L);
        BookCopy copy2 = new BookCopy(title2, library, "BC002", "Kệ 2", BookCopyStatus.AVAILABLE);
        copy2.setId(1002L);

        when(bookCopyRepository.findByBarcodeForUpdate("BC001")).thenReturn(Optional.of(copy1));
        when(bookCopyRepository.findByBarcodeForUpdate("BC002")).thenReturn(Optional.of(copy2));

        BorrowingPolicy policy = new BorrowingPolicy(library, 14, new BigDecimal("5000"), 5, Instant.now().minusSeconds(86400));
        policy.setId(20L);

        when(borrowingPolicyRepository.findFirstByLibraryIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(eq(10L), any()))
                .thenReturn(Optional.of(policy));

        when(borrowRepository.save(any(Borrow.class))).thenAnswer(invocation -> {
            Borrow b = invocation.getArgument(0);
            b.setId(System.currentTimeMillis());
            return b;
        });

        CheckoutRequest req = new CheckoutRequest(studentCode, List.of("BC001", "BC002"));
        BatchCheckoutResponse response = circulationService.checkout(req);

        assertEquals(2, response.items().size());
        assertEquals("BC001", response.items().get(0).barcode());
        assertEquals("BC002", response.items().get(1).barcode());

        // Crucial check: only 1 query for borrowing policy despite 2 barcodes from the same library
        verify(borrowingPolicyRepository, times(1))
                .findFirstByLibraryIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(eq(10L), any());
    }
}
