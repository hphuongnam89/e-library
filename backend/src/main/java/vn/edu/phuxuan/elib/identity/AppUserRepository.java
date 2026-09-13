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

    boolean existsByStudentCode(String studentCode);

    boolean existsByDepartmentId(Long departmentId);

    long countByRole(UserRole role);

    long countByStatus(UserStatus status);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT u FROM AppUser u
            LEFT JOIN FETCH u.department d
            WHERE (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
              AND (:departmentId IS NULL OR d.id = :departmentId)
              AND (:from IS NULL OR u.createdAt >= :from)
              AND (:to IS NULL OR u.createdAt <= :to)
            ORDER BY u.id DESC
            """,
            countQuery = """
            SELECT COUNT(u) FROM AppUser u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
              AND (:departmentId IS NULL OR u.department.id = :departmentId)
              AND (:from IS NULL OR u.createdAt >= :from)
              AND (:to IS NULL OR u.createdAt <= :to)
            """)
    org.springframework.data.domain.Page<AppUser> findReportUsers(
            @org.springframework.data.repository.query.Param("role") UserRole role,
            @org.springframework.data.repository.query.Param("status") UserStatus status,
            @org.springframework.data.repository.query.Param("departmentId") Long departmentId,
            @org.springframework.data.repository.query.Param("from") java.time.Instant from,
            @org.springframework.data.repository.query.Param("to") java.time.Instant to,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT u FROM AppUser u
            LEFT JOIN FETCH u.department d
            WHERE (:search IS NULL OR lower(u.email) LIKE lower(concat('%', :search, '%'))
                                   OR lower(u.fullName) LIKE lower(concat('%', :search, '%'))
                                   OR lower(u.studentCode) LIKE lower(concat('%', :search, '%')))
              AND (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
              AND (:departmentId IS NULL OR d.id = :departmentId)
            ORDER BY u.id DESC
            """,
            countQuery = """
            SELECT COUNT(u) FROM AppUser u
            WHERE (:search IS NULL OR lower(u.email) LIKE lower(concat('%', :search, '%'))
                                   OR lower(u.fullName) LIKE lower(concat('%', :search, '%'))
                                   OR lower(u.studentCode) LIKE lower(concat('%', :search, '%')))
              AND (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
              AND (:departmentId IS NULL OR u.department.id = :departmentId)
            """)
    org.springframework.data.domain.Page<AppUser> searchAdminUsers(
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("role") UserRole role,
            @org.springframework.data.repository.query.Param("status") UserStatus status,
            @org.springframework.data.repository.query.Param("departmentId") Long departmentId,
            org.springframework.data.domain.Pageable pageable
    );
}
