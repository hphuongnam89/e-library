package vn.edu.phuxuan.elib.reading;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DigitalReadingSessionRepository extends JpaRepository<DigitalReadingSession, UUID> {

    Page<DigitalReadingSession> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);
}
