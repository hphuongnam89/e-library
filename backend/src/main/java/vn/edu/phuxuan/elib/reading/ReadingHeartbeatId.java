package vn.edu.phuxuan.elib.reading;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ReadingHeartbeatId implements Serializable {

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    public ReadingHeartbeatId() {}

    public ReadingHeartbeatId(UUID sessionId, Long sequenceNumber) {
        this.sessionId = sessionId;
        this.sequenceNumber = sequenceNumber;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReadingHeartbeatId that = (ReadingHeartbeatId) o;
        return Objects.equals(sessionId, that.sessionId) && Objects.equals(sequenceNumber, that.sequenceNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, sequenceNumber);
    }
}
