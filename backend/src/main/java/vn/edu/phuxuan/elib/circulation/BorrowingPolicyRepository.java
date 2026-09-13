package vn.edu.phuxuan.elib.circulation;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BorrowingPolicyRepository extends JpaRepository<BorrowingPolicy, Long> {

    Optional<BorrowingPolicy> findFirstByLibraryIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(Long libraryId, Instant effectiveFrom);

    Page<BorrowingPolicy> findByLibraryIdOrderByEffectiveFromDesc(Long libraryId, Pageable pageable);

    boolean existsByLibraryIdAndEffectiveFrom(Long libraryId, Instant effectiveFrom);
}
