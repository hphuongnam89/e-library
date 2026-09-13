package vn.edu.phuxuan.elib.reading;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import vn.edu.phuxuan.elib.digital.DigitalDocument;
import vn.edu.phuxuan.elib.identity.AppUser;

@Entity
@Table(name = "digital_reading_summary")
public class DigitalReadingSummary {

    @EmbeddedId
    private DigitalReadingSummaryId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("documentId")
    @JoinColumn(name = "document_id", nullable = false)
    private DigitalDocument document;

    @Column(name = "active_seconds", nullable = false)
    private Long activeSeconds = 0L;

    @Column(name = "session_count", nullable = false)
    private Long sessionCount = 0L;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    protected DigitalReadingSummary() {}

    public DigitalReadingSummary(AppUser user, DigitalDocument document) {
        this.user = user;
        this.document = document;
        this.id = new DigitalReadingSummaryId(user.getId(), document.getId());
        this.activeSeconds = 0L;
        this.sessionCount = 0L;
        this.lastActiveAt = Instant.now();
    }

    public DigitalReadingSummaryId getId() {
        return id;
    }

    public void setId(DigitalReadingSummaryId id) {
        this.id = id;
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

    public Long getActiveSeconds() {
        return activeSeconds;
    }

    public void setActiveSeconds(Long activeSeconds) {
        this.activeSeconds = activeSeconds;
    }

    public Long getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(Long sessionCount) {
        this.sessionCount = sessionCount;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DigitalReadingSummary that = (DigitalReadingSummary) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
