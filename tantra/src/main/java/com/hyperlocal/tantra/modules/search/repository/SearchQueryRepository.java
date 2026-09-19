package com.hyperlocal.tantra.modules.search.repository;

import com.hyperlocal.tantra.modules.search.entity.SearchQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchQueryRepository extends JpaRepository<SearchQuery, Long> {

    /**
     * Top N trending query terms ranked by frequency in the given time window.
     * Excludes very short terms (< 2 chars) to filter noise.
     */
    @Query(value =
            "SELECT sq.query_term FROM search_queries sq" +
            " WHERE sq.searched_at >= :since AND LENGTH(sq.query_term) >= 2" +
            " GROUP BY sq.query_term ORDER BY COUNT(*) DESC LIMIT :limit",
            nativeQuery = true)
    List<String> findTrendingTerms(@Param("since") LocalDateTime since,
                                    @Param("limit") int limit);
}
