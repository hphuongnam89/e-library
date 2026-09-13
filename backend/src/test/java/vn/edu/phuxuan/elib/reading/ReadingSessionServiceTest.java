package vn.edu.phuxuan.elib.reading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.digital.DigitalDocument;
import vn.edu.phuxuan.elib.digital.DigitalDocumentPermission;
import vn.edu.phuxuan.elib.digital.DigitalDocumentRepository;
import vn.edu.phuxuan.elib.digital.DigitalDocumentService;
import vn.edu.phuxuan.elib.identity.AppUser;
import vn.edu.phuxuan.elib.identity.UserRole;
import vn.edu.phuxuan.elib.identity.UserStatus;
import vn.edu.phuxuan.elib.organization.Campus;
import vn.edu.phuxuan.elib.organization.Institution;
import vn.edu.phuxuan.elib.organization.Library;
import vn.edu.phuxuan.elib.reading.dto.EndReadingSessionResponse;
import vn.edu.phuxuan.elib.reading.dto.HeartbeatRequest;
import vn.edu.phuxuan.elib.reading.dto.HeartbeatResponse;
import vn.edu.phuxuan.elib.reading.dto.ReadingHistoryItemDto;
import vn.edu.phuxuan.elib.reading.dto.StartReadingSessionResponse;

@ExtendWith(MockitoExtension.class)
class ReadingSessionServiceTest {

    @Mock
    private DigitalReadingSessionRepository sessionRepository;
    @Mock
    private ReadingHeartbeatRepository heartbeatRepository;
    @Mock
    private DigitalReadingSummaryRepository summaryRepository;
    @Mock
    private DigitalDocumentRepository documentRepository;
    @Mock
    private DigitalDocumentService digitalDocumentService;

    @InjectMocks
    private ReadingSessionService service;

    private AppUser user;
    private AppUser otherUser;
    private DigitalDocument document;
    private DigitalReadingSession session;

    @BeforeEach
    void setUp() {
        Institution institution = new Institution("Đại học Phú Xuân");
        Campus campus = new Campus(institution, "Cơ sở 1");
        Library library = new Library(campus, "Thư viện 1", "Huế");

        user = new AppUser("sub-123", "student@pxu.edu.vn", "Nguyễn Văn A", UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", 100L);

        otherUser = new AppUser("sub-456", "other@pxu.edu.vn", "Trần Thị B", UserRole.STUDENT);
        otherUser.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(otherUser, "id", 200L);

        document = new DigitalDocument(
                library, "Lập trình Java Căn bản", "Tài liệu học tập", "NXB KHKT", null,
                "key-java.pdf", "application/pdf", 1024L, DigitalDocumentPermission.AUTHENTICATED
        );
        ReflectionTestUtils.setField(document, "id", 1L);

        session = new DigitalReadingSession(user, document);
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        session.setStartedAt(Instant.now().minus(15, ChronoUnit.SECONDS));
        session.setLastSeenAt(Instant.now().minus(15, ChronoUnit.SECONDS));
    }

    @Test
    void startSessionSuccessCreatesSessionAndUpdatesSummary() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(sessionRepository.save(any(DigitalReadingSession.class))).thenAnswer(inv -> {
            DigitalReadingSession s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", UUID.randomUUID());
            return s;
        });
        when(summaryRepository.findByUserIdAndDocumentId(100L, 1L)).thenReturn(Optional.empty());

        StartReadingSessionResponse res = service.startSession(1L, user);

        assertThat(res).isNotNull();
        assertThat(res.sessionId()).isNotNull();
        assertThat(res.heartbeatIntervalSeconds()).isEqualTo(15);
        assertThat(res.idleTimeoutSeconds()).isEqualTo(60);

        verify(digitalDocumentService).checkAccess(document, user);
        verify(sessionRepository).save(any(DigitalReadingSession.class));
        verify(summaryRepository).save(any(DigitalReadingSummary.class));
    }

    @Test
    void startSessionFailsWhenDocumentNotFound() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startSession(999L, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không tìm thấy tài liệu số");
    }

    @Test
    void processHeartbeatNormalActiveAndVisibleIncrementsTime() {
        UUID sId = session.getId();
        when(sessionRepository.findById(sId)).thenReturn(Optional.of(session));
        when(heartbeatRepository.existsByIdSessionIdAndIdSequenceNumber(sId, 1L)).thenReturn(false);
        when(summaryRepository.findByUserIdAndDocumentId(100L, 1L)).thenReturn(Optional.empty());

        HeartbeatRequest req = new HeartbeatRequest(1L, true, true);
        HeartbeatResponse res = service.processHeartbeat(sId, req, user);

        assertThat(res.accepted()).isTrue();
        assertThat(res.sessionEnded()).isFalse();
        assertThat(res.activeSeconds()).isGreaterThan(0L);

        verify(heartbeatRepository).save(any(ReadingHeartbeat.class));
        verify(summaryRepository).save(any(DigitalReadingSummary.class));
    }

    @Test
    void processHeartbeatHiddenTabDoesNotAddActiveTime() {
        UUID sId = session.getId();
        when(sessionRepository.findById(sId)).thenReturn(Optional.of(session));
        when(heartbeatRepository.existsByIdSessionIdAndIdSequenceNumber(sId, 1L)).thenReturn(false);

        // active = true, but visible = false (tab was in background)
        HeartbeatRequest req = new HeartbeatRequest(1L, true, false);
        HeartbeatResponse res = service.processHeartbeat(sId, req, user);

        assertThat(res.accepted()).isTrue();
        assertThat(res.activeSeconds()).isEqualTo(0L);

        verify(heartbeatRepository).save(any(ReadingHeartbeat.class));
        verify(summaryRepository, never()).save(any(DigitalReadingSummary.class));
    }

    @Test
    void processHeartbeatIdleDoesNotAddActiveTime() {
        UUID sId = session.getId();
        when(sessionRepository.findById(sId)).thenReturn(Optional.of(session));
        when(heartbeatRepository.existsByIdSessionIdAndIdSequenceNumber(sId, 1L)).thenReturn(false);

        // active = false (idle > 60s), visible = true
        HeartbeatRequest req = new HeartbeatRequest(1L, false, true);
        HeartbeatResponse res = service.processHeartbeat(sId, req, user);

        assertThat(res.accepted()).isTrue();
        assertThat(res.activeSeconds()).isEqualTo(0L);

        verify(heartbeatRepository).save(any(ReadingHeartbeat.class));
        verify(summaryRepository, never()).save(any(DigitalReadingSummary.class));
    }

    @Test
    void processHeartbeatReplaySequenceIsRejected() {
        UUID sId = session.getId();
        when(sessionRepository.findById(sId)).thenReturn(Optional.of(session));
        when(heartbeatRepository.existsByIdSessionIdAndIdSequenceNumber(sId, 1L)).thenReturn(true);

        HeartbeatRequest req = new HeartbeatRequest(1L, true, true);
        HeartbeatResponse res = service.processHeartbeat(sId, req, user);

        assertThat(res.accepted()).isFalse();
        verify(heartbeatRepository, never()).save(any(ReadingHeartbeat.class));
    }

    @Test
    void processHeartbeatEndedSessionIsRejected() {
        UUID sId = session.getId();
        session.setEndedAt(Instant.now());
        when(sessionRepository.findById(sId)).thenReturn(Optional.of(session));

        HeartbeatRequest req = new HeartbeatRequest(1L, true, true);
        HeartbeatResponse res = service.processHeartbeat(sId, req, user);

        assertThat(res.accepted()).isFalse();
        assertThat(res.sessionEnded()).isTrue();
        verify(heartbeatRepository, never()).save(any(ReadingHeartbeat.class));
    }

    @Test
    void processHeartbeatForbiddenWhenSessionNotOwnedByUser() {
        UUID sId = session.getId();
        when(sessionRepository.findById(sId)).thenReturn(Optional.of(session));

        HeartbeatRequest req = new HeartbeatRequest(1L, true, true);
        assertThatThrownBy(() -> service.processHeartbeat(sId, req, otherUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Phiên đọc không thuộc về bạn");
    }

    @Test
    void processHeartbeatMultiTabCapsElapsedToWallClock() {
        UUID sId = session.getId();
        when(sessionRepository.findById(sId)).thenReturn(Optional.of(session));
        when(heartbeatRepository.existsByIdSessionIdAndIdSequenceNumber(sId, 1L)).thenReturn(false);

        // Simulate user had sent a heartbeat on another tab 2 seconds ago
        Instant twoSecondsAgo = Instant.now().minus(2, ChronoUnit.SECONDS);
        service.setUserLastActive(100L, twoSecondsAgo);

        HeartbeatRequest req = new HeartbeatRequest(1L, true, true);
        HeartbeatResponse res = service.processHeartbeat(sId, req, user);

        // Session delta would have been ~15s, but wall clock since other tab is only 2s
        assertThat(res.accepted()).isTrue();
        assertThat(res.activeSeconds()).isLessThanOrEqualTo(3L);
    }

    @Test
    void endSessionIsIdempotent() {
        UUID sId = session.getId();
        when(sessionRepository.findById(sId)).thenReturn(Optional.of(session));

        EndReadingSessionResponse res1 = service.endSession(sId, user);
        assertThat(res1.success()).isTrue();
        assertThat(session.isEnded()).isTrue();

        // Calling end again is idempotent
        EndReadingSessionResponse res2 = service.endSession(sId, user);
        assertThat(res2.success()).isTrue();
    }

    @Test
    void getMyReadingHistoryReturnsSummaryPage() {
        DigitalReadingSummary summary = new DigitalReadingSummary(user, document);
        summary.setActiveSeconds(120L);
        summary.setSessionCount(3L);
        summary.setLastActiveAt(Instant.now());

        Page<DigitalReadingSummary> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1);
        when(summaryRepository.findByUserIdWithDocument(eq(100L), any())).thenReturn(page);

        Page<ReadingHistoryItemDto> result = service.getMyReadingHistory(user, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        ReadingHistoryItemDto item = result.getContent().get(0);
        assertThat(item.title()).isEqualTo("Lập trình Java Căn bản");
        assertThat(item.activeSeconds()).isEqualTo(120L);
        assertThat(item.sessionCount()).isEqualTo(3L);
    }
}
