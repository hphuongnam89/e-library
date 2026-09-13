package vn.edu.phuxuan.elib.circulation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BorrowRepository extends JpaRepository<Borrow, Long> {

    long countByUserIdAndStatus(Long userId, BorrowStatus status);

    boolean existsByUserIdAndFineAmountGreaterThanAndFinePaidAtIsNull(Long userId, BigDecimal zero);

    Optional<Borrow> findByBookCopyIdAndStatus(Long bookCopyId, BorrowStatus status);

    Page<Borrow> findByUserId(Long userId, Pageable pageable);

    boolean existsByBookCopyId(Long bookCopyId);

    boolean existsByPolicyId(Long policyId);

    @Query("""
            SELECT b FROM Borrow b
            WHERE (:userId IS NULL OR b.user.id = :userId)
              AND (:status IS NULL OR b.status = :status)
              AND (:overdue IS NULL OR (:overdue = true AND b.status = vn.edu.phuxuan.elib.circulation.BorrowStatus.BORROWED AND b.dueAt < :now) OR (:overdue = false AND (b.status = vn.edu.phuxuan.elib.circulation.BorrowStatus.RETURNED OR b.dueAt >= :now)))
              AND (:from IS NULL OR b.borrowedAt >= :from)
              AND (:to IS NULL OR b.borrowedAt <= :to)
            ORDER BY b.borrowedAt DESC
            """)
    Page<Borrow> findBorrowsWithFilters(
            @Param("userId") Long userId,
            @Param("status") BorrowStatus status,
            @Param("overdue") Boolean overdue,
            @Param("now") Instant now,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
