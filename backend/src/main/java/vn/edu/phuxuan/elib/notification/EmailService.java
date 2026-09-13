package vn.edu.phuxuan.elib.notification;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@pxu.edu.vn}")
    private String fromEmail;

    @Value("${elib.notifications.mock-email:false}")
    private boolean mockEmail;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String toEmail, String subject, String contentHtml) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Địa chỉ người nhận không được để trống");
        }

        if (mailSender == null || mockEmail) {
            log.info("[MOCK EMAIL] To: {} | Subject: {} | Content snippet: {}",
                    toEmail, subject, contentHtml.length() > 80 ? contentHtml.substring(0, 80) + "..." : contentHtml);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(contentHtml, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Lỗi gửi email: " + e.getMessage(), e);
        }
    }
}
