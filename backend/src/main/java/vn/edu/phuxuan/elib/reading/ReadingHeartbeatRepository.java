package vn.edu.phuxuan.elib.reading;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadingHeartbeatRepository extends JpaRepository<ReadingHeartbeat, ReadingHeartbeatId> {

    boolean existsByIdSessionIdAndIdSequenceNumber(UUID sessionId, Long sequenceNumber);

    Optional<ReadingHeartbeat> findFirstByIdSessionIdOrderByIdSequenceNumberDesc(UUID sessionId);
}
