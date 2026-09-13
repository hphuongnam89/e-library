package vn.edu.phuxuan.elib.catalog;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    Optional<BookCopy> findByBarcode(String barcode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM BookCopy c WHERE c.barcode = :barcode")
    Optional<BookCopy> findByBarcodeForUpdate(@Param("barcode") String barcode);

    boolean existsByBarcode(String barcode);

    boolean existsByBookTitleId(Long bookTitleId);

    boolean existsByLibraryId(Long libraryId);

    Page<BookCopy> findByBookTitleId(Long bookTitleId, Pageable pageable);

    Page<BookCopy> findByLibraryId(Long libraryId, Pageable pageable);

    Page<BookCopy> findByLibraryIdAndStatus(Long libraryId, BookCopyStatus status, Pageable pageable);

    @Query(value = "SELECT nextval('elib.book_barcode_sequence')", nativeQuery = true)
    Long getNextBarcodeSequence();
}
