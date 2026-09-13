package vn.edu.phuxuan.elib.digital;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DigitalDocumentRepository extends JpaRepository<DigitalDocument, Long> {

    boolean existsByStorageKey(String storageKey);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByLibraryId(Long libraryId);

    @Query("SELECT d FROM DigitalDocument d WHERE " +
           "(:libraryId IS NULL OR d.library.id = :libraryId) AND " +
           "(:categoryId IS NULL OR d.category.id = :categoryId) AND " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:isActive IS NULL OR d.isActive = :isActive) AND " +
           "(:query IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(d.publisher) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<DigitalDocument> findWithFilters(@Param("libraryId") Long libraryId,
                                         @Param("categoryId") Long categoryId,
                                         @Param("status") DigitalDocumentStatus status,
                                         @Param("isActive") Boolean isActive,
                                         @Param("query") String query,
                                         Pageable pageable);

    @Query("SELECT d FROM DigitalDocument d WHERE " +
           "d.isActive = true AND d.status = vn.edu.phuxuan.elib.digital.DigitalDocumentStatus.PUBLISHED AND " +
           "(:libraryId IS NULL OR d.library.id = :libraryId) AND " +
           "(:categoryId IS NULL OR d.category.id = :categoryId) AND " +
           "(:query IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(d.publisher) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(d.permission = vn.edu.phuxuan.elib.digital.DigitalDocumentPermission.AUTHENTICATED OR " +
           "EXISTS (SELECT g FROM DocumentGrant g WHERE g.document = d AND (" +
           "  g.user.id = :userId OR " +
           "  (:departmentId IS NOT NULL AND g.department.id = :departmentId) OR " +
           "  (:campusId IS NOT NULL AND g.campus.id = :campusId) OR " +
           "  (:institutionId IS NOT NULL AND g.institution.id = :institutionId)" +
           ")))")
    Page<DigitalDocument> findAccessibleForUser(@Param("userId") Long userId,
                                               @Param("departmentId") Long departmentId,
                                               @Param("campusId") Long campusId,
                                               @Param("institutionId") Long institutionId,
                                               @Param("libraryId") Long libraryId,
                                               @Param("categoryId") Long categoryId,
                                               @Param("query") String query,
                                               Pageable pageable);
}
