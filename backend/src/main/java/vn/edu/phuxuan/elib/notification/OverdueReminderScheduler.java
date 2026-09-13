package vn.edu.phuxuan.elib.notification;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phuxuan.elib.circulation.Borrow;
import vn.edu.phuxuan.elib.circulation.BorrowRepository;
import vn.edu.phuxuan.elib.notification.dto.TriggerRemindersResponse;

@Component
public class OverdueReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueReminderScheduler.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BorrowRepository borrowRepository;
    private final NotificationService notificationService;

    public OverdueReminderScheduler(BorrowRepository borrowRepository, NotificationService notificationService) {
        this.borrowRepository = borrowRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${elib.notifications.cron:0 0 7 * * *}")
    public void runDailyReminders() {
        log.info("Running scheduled daily overdue and due reminder scan...");
        TriggerRemindersResponse response = runScanNow();
        log.info("Reminder scan completed: {} due reminders created, {} overdue warnings created, {} dispatched.",
                response.dueRemindersCreated(), response.overdueCreated(), response.dispatched());
    }

    @Transactional
    public TriggerRemindersResponse runScanNow() {
        Instant now = Instant.now();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.ofInstant(now, zone);

        // 1. Scan due soon (within next 48 hours)
        Instant endOfWindow = now.plus(Duration.ofDays(2));
        List<Borrow> dueSoonBorrows = borrowRepository.findDueSoonBorrows(now, endOfWindow);
        int dueCreated = 0;

        for (Borrow b : dueSoonBorrows) {
            LocalDate dueDate = LocalDate.ofInstant(b.getDueAt(), zone);
            String dedupKey = "DUE_REMINDER:" + b.getId() + ":" + dueDate;
            String bookTitle = b.getBookCopy().getBookTitle().getTitle();
            String dueDateStr = dueDate.format(DATE_FORMATTER);

            String title = "Nhắc hẹn trả sách: " + bookTitle;
            String message = String.format(
                    "<p>Xin chào <strong>%s</strong>,</p>" +
                    "<p>Khoản mượn cuốn sách <strong>%s</strong> (Mã vạch: <code>%s</code>) của bạn tại Thư viện sắp đến hạn trả vào ngày <strong>%s</strong>.</p>" +
                    "<p>Vui lòng sắp xếp đến quầy lưu thông để hoàn trả hoặc gia hạn đúng hạn để tránh phát sinh phí phạt trễ hạn.</p>" +
                    "<p>Trân trọng,<br/>Thư viện Đại học Phú Xuân</p>",
                    b.getUser().getFullName(),
                    bookTitle,
                    b.getBookCopy().getBarcode(),
                    dueDateStr
            );

            var created = notificationService.createNotification(
                    b.getUser(), b, NotificationType.DUE_REMINDER, NotificationChannel.EMAIL, title, message, dedupKey
            );
            if (created.isPresent()) {
                dueCreated++;
            }
        }

        // 2. Scan overdue borrows
        List<Borrow> overdueBorrows = borrowRepository.findOverdueBorrows(now);
        int overdueCreated = 0;

        for (Borrow b : overdueBorrows) {
            String dedupKey = "OVERDUE:" + b.getId() + ":" + today;
            String bookTitle = b.getBookCopy().getBookTitle().getTitle();
            long overdueDays = Math.max(1, Duration.between(b.getDueAt(), now).toDays());
            BigDecimal fineAmount = b.getDailyFine().multiply(BigDecimal.valueOf(overdueDays));

            String title = "Cảnh báo quá hạn sách: " + bookTitle;
            String message = String.format(
                    "<p>Kính gửi <strong>%s</strong>,</p>" +
                    "<p>Khoản mượn cuốn sách <strong>%s</strong> (Mã vạch: <code>%s</code>) của bạn đã quá hạn <strong>%d ngày</strong>.</p>" +
                    "<p>Phí phạt tạm tính hiện tại: <strong style='color: red;'>%,.0f VNĐ</strong> (Mức phạt: %,.0f VNĐ/ngày).</p>" +
                    "<p>Vui lòng đến ngay Quầy lưu thông để hoàn trả tài liệu và thanh toán tiền phạt. Tài khoản có nợ phạt quá hạn sẽ bị tạm khóa quyền mượn sách mới.</p>" +
                    "<p>Trân trọng,<br/>Thư viện Đại học Phú Xuân</p>",
                    b.getUser().getFullName(),
                    bookTitle,
                    b.getBookCopy().getBarcode(),
                    overdueDays,
                    fineAmount,
                    b.getDailyFine()
            );

            var created = notificationService.createNotification(
                    b.getUser(), b, NotificationType.OVERDUE, NotificationChannel.EMAIL, title, message, dedupKey
            );
            if (created.isPresent()) {
                overdueCreated++;
            }
        }

        // 3. Dispatch pending notifications
        int dispatched = notificationService.dispatchPendingNotifications();

        return new TriggerRemindersResponse(dueCreated, overdueCreated, dispatched);
    }
}
