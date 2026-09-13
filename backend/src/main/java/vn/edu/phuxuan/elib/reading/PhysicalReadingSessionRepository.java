package vn.edu.phuxuan.elib.reading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhysicalReadingSessionRepository extends JpaRepository<PhysicalReadingSession, Long> {

    Page<PhysicalReadingSession> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);
}
