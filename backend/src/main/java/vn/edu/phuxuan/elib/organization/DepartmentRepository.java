package vn.edu.phuxuan.elib.organization;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByLibraryId(Long libraryId);

    Page<Department> findByLibraryId(Long libraryId, Pageable pageable);

    boolean existsByLibraryId(Long libraryId);
}
