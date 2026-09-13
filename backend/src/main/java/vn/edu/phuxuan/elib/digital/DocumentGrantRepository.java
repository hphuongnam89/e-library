package vn.edu.phuxuan.elib.digital;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentGrantRepository extends JpaRepository<DocumentGrant, Long> {

    List<DocumentGrant> findByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);

    @Query("SELECT COUNT(g) > 0 FROM DocumentGrant g WHERE g.document.id = :documentId AND (" +
           "g.user.id = :userId OR " +
           "(:departmentId IS NOT NULL AND g.department.id = :departmentId) OR " +
           "(:campusId IS NOT NULL AND g.campus.id = :campusId) OR " +
           "(:institutionId IS NOT NULL AND g.institution.id = :institutionId))")
    boolean hasGrantAccess(@Param("documentId") Long documentId,
                           @Param("userId") Long userId,
                           @Param("departmentId") Long departmentId,
                           @Param("campusId") Long campusId,
                           @Param("institutionId") Long institutionId);
}
