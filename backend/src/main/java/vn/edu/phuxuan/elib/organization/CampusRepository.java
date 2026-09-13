package vn.edu.phuxuan.elib.organization;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampusRepository extends JpaRepository<Campus, Long> {

    List<Campus> findByInstitutionId(Long institutionId);

    Page<Campus> findByInstitutionId(Long institutionId, Pageable pageable);

    boolean existsByInstitutionId(Long institutionId);
}
