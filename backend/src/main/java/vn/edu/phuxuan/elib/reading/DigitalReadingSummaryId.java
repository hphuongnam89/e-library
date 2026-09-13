package vn.edu.phuxuan.elib.reading;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DigitalReadingSummaryId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    public DigitalReadingSummaryId() {}

    public DigitalReadingSummaryId(Long userId, Long documentId) {
        this.userId = userId;
        this.documentId = documentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DigitalReadingSummaryId that = (DigitalReadingSummaryId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(documentId, that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, documentId);
    }
}
