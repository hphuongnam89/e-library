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

    @Query("SELECT b FROM Borrow b JOIN FETCH b.user u JOIN FETCH b.bookCopy bc JOIN FETCH bc.bookTitle bt WHERE b.status = vn.edu.phuxuan.elib.circulation.BorrowStatus.BORROWED AND b.dueAt >= :start AND b.dueAt <= :end")
    java.util.List<Borrow> findDueSoonBorrows(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT b FROM Borrow b JOIN FETCH b.user u JOIN FETCH b.bookCopy bc JOIN FETCH bc.bookTitle bt WHERE b.status = vn.edu.phuxuan.elib.circulation.BorrowStatus.BORROWED AND b.dueAt < :now")
    java.util.List<Borrow> findOverdueBorrows(@Param("now") Instant now);

    long countByStatus(BorrowStatus status);

    long countByStatusAndDueAtBefore(BorrowStatus status, Instant dueAt);

    @Query("SELECT COALESCE(SUM(b.fineAmount), 0) FROM Borrow b WHERE b.fineAmount > 0 AND b.finePaidAt IS NULL")
    BigDecimal sumUnpaidFines();

    @Query("SELECT bc.bookTitle.id, bc.bookTitle.title, bc.bookTitle.author, COUNT(b) FROM Borrow b JOIN b.bookCopy bc GROUP BY bc.bookTitle.id, bc.bookTitle.title, bc.bookTitle.author ORDER BY COUNT(b) DESC")
    java.util.List<Object[]> findTopBorrowedBooks(Pageable pageable);

    @Query("SELECT b FROM Borrow b JOIN FETCH b.user u JOIN FETCH b.bookCopy bc JOIN FETCH bc.bookTitle bt ORDER BY b.borrowedAt DESC")
    java.util.List<Borrow> findRecentBorrows(Pageable pageable);

    @Query(value = """
            SELECT b FROM Borrow b
            JOIN FETCH b.user u
            LEFT JOIN FETCH u.department d
            JOIN FETCH b.bookCopy bc
            JOIN FETCH bc.bookTitle bt
            WHERE (:status IS NULL OR b.status = :status)
              AND (:overdue IS NULL OR (:overdue = true AND b.status = vn.edu.phuxuan.elib.circulation.BorrowStatus.BORROWED AND b.dueAt < :now) OR (:overdue = false AND (b.status = vn.edu.phuxuan.elib.circulation.BorrowStatus.RETURNED OR b.dueAt >= :now)))
              AND (:departmentId IS NULL OR u.department.id = :departmentId)
              AND (:from IS NULL OR b.borrowedAt >= :from)
              AND (:to IS NULL OR b.borrowedAt <= :to)
            ORDER BY b.borrowedAt DESC
            """,
            countQuery = """
            SELECT COUNT(b) FROM Borrow b
            WHERE (:status IS NULL OR b.status = :status)
              AND (:overdue IS NULL OR (:overdue = true AND b.status = vn.edu.phuxuan.elib.circulation.BorrowStatus.BORROWED AND b.dueAt < :now) OR (:overdue = false AND (b.status = vn.edu.phuxuan.elib.circulation.BorrowStatus.RETURNED OR b.dueAt >= :now)))
              AND (:departmentId IS NULL OR b.user.department.id = :departmentId)
              AND (:from IS NULL OR b.borrowedAt >= :from)
              AND (:to IS NULL OR b.borrowedAt <= :to)
            """)
    Page<Borrow> findReportBorrows(
            @Param("status") BorrowStatus status,
            @Param("overdue") Boolean overdue,
            @Param("now") Instant now,
            @Param("departmentId") Long departmentId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
