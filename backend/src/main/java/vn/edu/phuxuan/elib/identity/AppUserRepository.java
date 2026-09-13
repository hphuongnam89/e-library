package vn.edu.phuxuan.elib.identity;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByGoogleSubject(String googleSubject);

    Optional<AppUser> findByEmailIgnoreCase(String email);

    Optional<AppUser> findByStudentCode(String studentCode);

    boolean existsByGoogleSubject(String googleSubject);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDepartmentId(Long departmentId);
}
