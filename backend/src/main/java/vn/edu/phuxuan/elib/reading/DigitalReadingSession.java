package vn.edu.phuxuan.elib.reading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import vn.edu.phuxuan.elib.digital.DigitalDocument;
import vn.edu.phuxuan.elib.identity.AppUser;

@Entity
@Table(name = "digital_reading_session")
public class DigitalReadingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DigitalDocument document;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "active_seconds", nullable = false)
    private Long activeSeconds = 0L;

    protected DigitalReadingSession() {}

    public DigitalReadingSession(AppUser user, DigitalDocument document) {
        this.user = user;
        this.document = document;
        this.startedAt = Instant.now();
        this.lastSeenAt = this.startedAt;
        this.activeSeconds = 0L;
    }

    public UUID getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public DigitalDocument getDocument() {
        return document;
    }

    public void setDocument(DigitalDocument document) {
        this.document = document;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public Long getActiveSeconds() {
        return activeSeconds;
    }

    public void setActiveSeconds(Long activeSeconds) {
        this.activeSeconds = activeSeconds;
    }

    public boolean isEnded() {
        return endedAt != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DigitalReadingSession that = (DigitalReadingSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
