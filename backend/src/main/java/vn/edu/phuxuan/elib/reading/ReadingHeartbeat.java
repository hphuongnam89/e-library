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

@Entity
@Table(name = "reading_heartbeat")
public class ReadingHeartbeat {

    @EmbeddedId
    private ReadingHeartbeatId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("sessionId")
    @JoinColumn(name = "session_id", nullable = false)
    private DigitalReadingSession session;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "visible", nullable = false)
    private Boolean visible;

    protected ReadingHeartbeat() {}

    public ReadingHeartbeat(DigitalReadingSession session, Long sequenceNumber, Boolean active, Boolean visible) {
        this.session = session;
        this.id = new ReadingHeartbeatId(session.getId(), sequenceNumber);
        this.receivedAt = Instant.now();
        this.active = active;
        this.visible = visible;
    }

    public ReadingHeartbeatId getId() {
        return id;
    }

    public void setId(ReadingHeartbeatId id) {
        this.id = id;
    }

    public DigitalReadingSession getSession() {
        return session;
    }

    public void setSession(DigitalReadingSession session) {
        this.session = session;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReadingHeartbeat that = (ReadingHeartbeat) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
