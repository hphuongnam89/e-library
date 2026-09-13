package vn.edu.phuxuan.elib.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.phuxuan.elib.catalog.BookCopy;
import vn.edu.phuxuan.elib.catalog.BookTitle;
import vn.edu.phuxuan.elib.circulation.Borrow;
import vn.edu.phuxuan.elib.circulation.BorrowRepository;
import vn.edu.phuxuan.elib.circulation.BorrowStatus;
import vn.edu.phuxuan.elib.circulation.BorrowingPolicy;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.notification.dto.TriggerRemindersResponse;
import vn.edu.phuxuan.elib.organization.Campus;
import vn.edu.phuxuan.elib.organization.Institution;
import vn.edu.phuxuan.elib.organization.Library;

@ExtendWith(MockitoExtension.class)
class OverdueReminderSchedulerTest {

    @Mock
    private BorrowRepository borrowRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OverdueReminderScheduler scheduler;

    private AppUser user;
    private Borrow dueSoonBorrow;
    private Borrow overdueBorrow;

    @BeforeEach
    void setUp() {
        Institution inst = new Institution("Đại học Phú Xuân");
        Campus campus = new Campus(inst, "Cơ sở 1");
        Library lib = new Library(campus, "Thư viện Khoa học", "Huế");

        user = new AppUser("sub-1", "student@pxu.edu.vn", "Sinh Viên A", UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", 10L);

        BookTitle bookTitle = new BookTitle("Giải tích 1", "Toán học", "NXB GD", "ISBN-1", (short) 2024, null);
        BookCopy copy1 = new BookCopy(bookTitle, lib, "BC-1001", "Kệ A1", vn.edu.phuxuan.elib.catalog.BookCopyStatus.BORROWED);
        BookCopy copy2 = new BookCopy(bookTitle, lib, "BC-1002", "Kệ A2", vn.edu.phuxuan.elib.catalog.BookCopyStatus.BORROWED);

        BorrowingPolicy policy = new BorrowingPolicy(lib, 14, new BigDecimal("5000"), 5, Instant.now());

        // Due soon borrow (due in 24 hours)
        Instant borrowedAt = Instant.now().minus(13, ChronoUnit.DAYS);
        dueSoonBorrow = new Borrow(user, copy1, policy, borrowedAt, Instant.now().plus(1, ChronoUnit.DAYS), new BigDecimal("5000"));
        ReflectionTestUtils.setField(dueSoonBorrow, "id", 100L);

        // Overdue borrow (due 3 days ago)
        Instant overdueBorrowedAt = Instant.now().minus(17, ChronoUnit.DAYS);
        overdueBorrow = new Borrow(user, copy2, policy, overdueBorrowedAt, Instant.now().minus(3, ChronoUnit.DAYS), new BigDecimal("5000"));
        ReflectionTestUtils.setField(overdueBorrow, "id", 200L);
    }

    @Test
    void runScanNowGeneratesDueRemindersAndOverdueAlerts() {
        when(borrowRepository.findDueSoonBorrows(any(), any())).thenReturn(List.of(dueSoonBorrow));
        when(borrowRepository.findOverdueBorrows(any())).thenReturn(List.of(overdueBorrow));

        Notification mockNotification = mock(Notification.class);
        when(notificationService.createNotification(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(mockNotification));
        when(notificationService.dispatchPendingNotifications()).thenReturn(2);

        TriggerRemindersResponse response = scheduler.runScanNow();

        assertThat(response.dueRemindersCreated()).isEqualTo(1);
        assertThat(response.overdueCreated()).isEqualTo(1);
        assertThat(response.dispatched()).isEqualTo(2);

        // Verify due reminder created with deterministic key
        verify(notificationService).createNotification(
                eq(user),
                eq(dueSoonBorrow),
                eq(NotificationType.DUE_REMINDER),
                eq(NotificationChannel.EMAIL),
                contains("Nhắc hẹn trả sách:"),
                contains("Giải tích 1"),
                contains("DUE_REMINDER:100:")
        );

        // Verify overdue alert created with deterministic key
        verify(notificationService).createNotification(
                eq(user),
                eq(overdueBorrow),
                eq(NotificationType.OVERDUE),
                eq(NotificationChannel.EMAIL),
                contains("Cảnh báo quá hạn sách:"),
                contains("Giải tích 1"),
                contains("OVERDUE:200:")
        );

        verify(notificationService).dispatchPendingNotifications();
    }
}
