package vn.edu.phuxuan.elib.admin;

import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phuxuan.elib.admin.dto.AuditLogDto;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.AppUserRepository;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AppUserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, AppUserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AppUser user, String action, String resourceType, String resourceId, String details) {
        String requestId = MDC.get("requestId");
        AuditLog auditLog = new AuditLog(user, action, resourceType, resourceId, requestId, details);
        auditLogRepository.save(auditLog);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String action, String resourceType, String resourceId, String details) {
        AppUser user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        log(user, action, resourceType, resourceId, details);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> searchAuditLogs(String action, String resourceType, Long userId, Instant from, Instant to, Pageable pageable) {
        return auditLogRepository.searchAuditLogs(action, resourceType, userId, from, to, pageable)
                .map(this::toDto);
    }

    private AuditLogDto toDto(AuditLog log) {
        Long userId = log.getUser() != null ? log.getUser().getId() : null;
        String userEmail = log.getUser() != null ? log.getUser().getEmail() : null;
        String userFullName = log.getUser() != null ? log.getUser().getFullName() : null;
        return new AuditLogDto(
                log.getId(),
                userId,
                userEmail,
                userFullName,
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getRequestId(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}
