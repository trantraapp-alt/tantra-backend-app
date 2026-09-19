package com.hyperlocal.tantra.modules.search.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Tracks every search query submitted by users.
 * Used to compute trending searches shown on the home feed.
 * userId is nullable — anonymous searches are also tracked for trending.
 */
@Entity
@Table(name = "search_queries", indexes = {
        @Index(name = "idx_sq_term_time", columnList = "query_term, searched_at"),
        @Index(name = "idx_sq_user",      columnList = "user_id")
})
@Data
public class SearchQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The raw query string as typed by the user (lowercased before save). */
    @Column(name = "query_term", nullable = false, length = 200)
    private String queryTerm;

    /** Null for anonymous / unauthenticated searches. */
    @Column(name = "user_id", length = 20)
    private String userId;

    /** Result count returned for this search — useful for zero-result detection. */
    @Column(name = "result_count")
    private Integer resultCount;

    @Column(name = "searched_at", nullable = false, updatable = false)
    private LocalDateTime searchedAt = LocalDateTime.now();
}
