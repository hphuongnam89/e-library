package vn.edu.phuxuan.elib.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookTitleRepository extends JpaRepository<BookTitle, Long> {

    Page<BookTitle> findByCategoryId(Long categoryId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    @Query(
            value = "SELECT * FROM elib.book_title WHERE search_vector @@ plainto_tsquery('simple', :query) " +
                    "AND (CAST(:categoryId AS BIGINT) IS NULL OR category_id = CAST(:categoryId AS BIGINT))",
            countQuery = "SELECT count(*) FROM elib.book_title WHERE search_vector @@ plainto_tsquery('simple', :query) " +
                    "AND (CAST(:categoryId AS BIGINT) IS NULL OR category_id = CAST(:categoryId AS BIGINT))",
            nativeQuery = true
    )
    Page<BookTitle> searchFts(@Param("query") String query, @Param("categoryId") Long categoryId, Pageable pageable);
}
