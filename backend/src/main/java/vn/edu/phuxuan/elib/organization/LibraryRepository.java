package vn.edu.phuxuan.elib.organization;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LibraryRepository extends JpaRepository<Library, Long> {

    List<Library> findByCampusId(Long campusId);

    Page<Library> findByCampusId(Long campusId, Pageable pageable);

    boolean existsByCampusId(Long campusId);
}
