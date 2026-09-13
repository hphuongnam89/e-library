package vn.edu.phuxuan.elib.reading;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DigitalReadingSummaryRepository extends JpaRepository<DigitalReadingSummary, DigitalReadingSummaryId> {

    @Query("SELECT s FROM DigitalReadingSummary s WHERE s.user.id = :userId AND s.document.id = :documentId")
    Optional<DigitalReadingSummary> findByUserIdAndDocumentId(@Param("userId") Long userId, @Param("documentId") Long documentId);

    @Query("SELECT s FROM DigitalReadingSummary s JOIN FETCH s.document d WHERE s.user.id = :userId ORDER BY s.lastActiveAt DESC NULLS LAST")
    Page<DigitalReadingSummary> findByUserIdWithDocument(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.activeSeconds), 0) FROM DigitalReadingSummary s")
    long sumTotalActiveSeconds();

    @Query("SELECT s.document.id, s.document.title, s.document.author, SUM(s.activeSeconds), SUM(s.sessionCount) FROM DigitalReadingSummary s GROUP BY s.document.id, s.document.title, s.document.author ORDER BY SUM(s.activeSeconds) DESC")
    java.util.List<Object[]> findTopReadDocuments(Pageable pageable);

    @Query(value = """
            SELECT s FROM DigitalReadingSummary s
            JOIN FETCH s.document d
            JOIN FETCH s.user u
            LEFT JOIN FETCH u.department dept
            WHERE (:documentId IS NULL OR d.id = :documentId)
              AND (:departmentId IS NULL OR dept.id = :departmentId)
              AND (:from IS NULL OR s.lastActiveAt >= :from)
              AND (:to IS NULL OR s.lastActiveAt <= :to)
            ORDER BY s.activeSeconds DESC
            """,
            countQuery = """
            SELECT COUNT(s) FROM DigitalReadingSummary s
            WHERE (:documentId IS NULL OR s.document.id = :documentId)
              AND (:departmentId IS NULL OR s.user.department.id = :departmentId)
              AND (:from IS NULL OR s.lastActiveAt >= :from)
              AND (:to IS NULL OR s.lastActiveAt <= :to)
            """)
    Page<DigitalReadingSummary> findReportSummaries(
            @Param("documentId") Long documentId,
            @Param("departmentId") Long departmentId,
            @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to,
            Pageable pageable
    );
}
