package vn.edu.phuxuan.elib.reading;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.digital.DigitalDocument;
import vn.edu.phuxuan.elib.digital.DigitalDocumentRepository;
import vn.edu.phuxuan.elib.digital.DigitalDocumentService;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.reading.dto.EndReadingSessionResponse;
import vn.edu.phuxuan.elib.reading.dto.HeartbeatRequest;
import vn.edu.phuxuan.elib.reading.dto.HeartbeatResponse;
import vn.edu.phuxuan.elib.reading.dto.ReadingHistoryItemDto;
import vn.edu.phuxuan.elib.reading.dto.StartReadingSessionResponse;

@Service
@Transactional
public class ReadingSessionService {

    private static final int HEARTBEAT_INTERVAL_SECONDS = 15;
    private static final int IDLE_TIMEOUT_SECONDS = 60;
    private static final long MAX_HEARTBEAT_CAP_SECONDS = 30;

    private final DigitalReadingSessionRepository sessionRepository;
    private final ReadingHeartbeatRepository heartbeatRepository;
    private final DigitalReadingSummaryRepository summaryRepository;
    private final DigitalDocumentRepository documentRepository;
    private final DigitalDocumentService digitalDocumentService;

    // Concurrent map tracking last active timestamp per user across all browser tabs
    private final Map<Long, Instant> userLastActiveMap = new ConcurrentHashMap<>();

    public ReadingSessionService(
            DigitalReadingSessionRepository sessionRepository,
            ReadingHeartbeatRepository heartbeatRepository,
            DigitalReadingSummaryRepository summaryRepository,
            DigitalDocumentRepository documentRepository,
            DigitalDocumentService digitalDocumentService) {
        this.sessionRepository = sessionRepository;
        this.heartbeatRepository = heartbeatRepository;
        this.summaryRepository = summaryRepository;
        this.documentRepository = documentRepository;
        this.digitalDocumentService = digitalDocumentService;
    }

    public StartReadingSessionResponse startSession(Long documentId, AppUser currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        DigitalDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu số"));

        // Validate access permissions (staff has full access, students require published + authenticated/granted)
        digitalDocumentService.checkAccess(document, currentUser);

        DigitalReadingSession session = new DigitalReadingSession(currentUser, document);
        session = sessionRepository.save(session);

        // Update or create summary for this (user, document)
        DigitalReadingSummary summary = summaryRepository.findByUserIdAndDocumentId(currentUser.getId(), document.getId())
                .orElseGet(() -> new DigitalReadingSummary(currentUser, document));
        summary.setSessionCount(summary.getSessionCount() + 1);
        summary.setLastActiveAt(Instant.now());
        summaryRepository.save(summary);

        return new StartReadingSessionResponse(session.getId(), HEARTBEAT_INTERVAL_SECONDS, IDLE_TIMEOUT_SECONDS);
    }

    public HeartbeatResponse processHeartbeat(UUID sessionId, HeartbeatRequest req, AppUser currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        DigitalReadingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên đọc"));

        if (!session.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Phiên đọc không thuộc về bạn");
        }

        if (session.isEnded()) {
            return new HeartbeatResponse(false, session.getActiveSeconds(), true);
        }

        // Sequence deduplication and replay attack prevention
        if (req.sequenceNumber() == null || req.sequenceNumber() < 0) {
            return new HeartbeatResponse(false, session.getActiveSeconds(), false);
        }

        if (heartbeatRepository.existsByIdSessionIdAndIdSequenceNumber(sessionId, req.sequenceNumber())) {
            return new HeartbeatResponse(false, session.getActiveSeconds(), false);
        }

        Instant now = Instant.now();
        long sessionDelta = Duration.between(session.getLastSeenAt(), now).toSeconds();
        if (sessionDelta < 0) {
            sessionDelta = 0;
        }
        if (sessionDelta > MAX_HEARTBEAT_CAP_SECONDS) {
            sessionDelta = MAX_HEARTBEAT_CAP_SECONDS;
        }

        boolean isActiveAndVisible = Boolean.TRUE.equals(req.active()) && Boolean.TRUE.equals(req.visible());
        long effectiveDelta = 0;

        if (isActiveAndVisible) {
            Instant lastUserActive = userLastActiveMap.get(currentUser.getId());
            if (lastUserActive != null) {
                long wallClockDelta = Duration.between(lastUserActive, now).toSeconds();
                if (wallClockDelta < 0) {
                    wallClockDelta = 0;
                }
                // Cap effective delta to physical wall clock duration across all tabs
                effectiveDelta = Math.min(sessionDelta, wallClockDelta);
            } else {
                effectiveDelta = sessionDelta;
            }
            userLastActiveMap.put(currentUser.getId(), now);
        }

        // Update session
        session.setActiveSeconds(session.getActiveSeconds() + effectiveDelta);
        session.setLastSeenAt(now);
        sessionRepository.save(session);

        // Update summary if any active seconds gained
        if (effectiveDelta > 0) {
            DigitalReadingSummary summary = summaryRepository.findByUserIdAndDocumentId(currentUser.getId(), session.getDocument().getId())
                    .orElseGet(() -> new DigitalReadingSummary(currentUser, session.getDocument()));
            summary.setActiveSeconds(summary.getActiveSeconds() + effectiveDelta);
            summary.setLastActiveAt(now);
            summaryRepository.save(summary);
        }

        // Persist heartbeat log
        ReadingHeartbeat hb = new ReadingHeartbeat(session, req.sequenceNumber(), req.active(), req.visible());
        hb.setReceivedAt(now);
        heartbeatRepository.save(hb);

        return new HeartbeatResponse(true, session.getActiveSeconds(), false);
    }

    public EndReadingSessionResponse endSession(UUID sessionId, AppUser currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        DigitalReadingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên đọc"));

        if (!session.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Phiên đọc không thuộc về bạn");
        }

        if (!session.isEnded()) {
            session.setEndedAt(Instant.now());
            sessionRepository.save(session);
        }

        return new EndReadingSessionResponse(true, session.getActiveSeconds());
    }

    @Transactional(readOnly = true)
    public Page<ReadingHistoryItemDto> getMyReadingHistory(AppUser currentUser, Pageable pageable) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        Page<DigitalReadingSummary> page = summaryRepository.findByUserIdWithDocument(currentUser.getId(), pageable);
        return page.map(s -> new ReadingHistoryItemDto(
                s.getDocument().getId(),
                s.getDocument().getTitle(),
                s.getDocument().getPublisher(),
                s.getActiveSeconds(),
                s.getSessionCount(),
                s.getLastActiveAt()
        ));
    }

    // Package-private method for testing multi-tab synchronization
    void setUserLastActive(Long userId, Instant timestamp) {
        if (timestamp != null) {
            userLastActiveMap.put(userId, timestamp);
        } else {
            userLastActiveMap.remove(userId);
        }
    }
}
