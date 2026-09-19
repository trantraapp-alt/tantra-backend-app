package com.hyperlocal.tantra.modules.listing.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Tracks which logged-in user has viewed which listing.
 * The unique constraint (listing_id, viewer_user_id) guarantees 1 count per user per listing
 * regardless of how many times they open it. Inserts use ON CONFLICT DO NOTHING — no application-
 * level read required, so this is safe under concurrent traffic.
 */
@Entity
@Table(name = "listing_views")
@Data
public class ListingView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false, length = 20)
    private String listingId;

    /** userId of the authenticated viewer. Anonymous views are not counted. */
    @Column(name = "viewer_user_id", nullable = false, length = 20)
    private String viewerUserId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt = LocalDateTime.now();
}
