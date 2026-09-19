package com.hyperlocal.tantra.modules.stats.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * One tile in the home screen stats ribbon.
 * Admin controls label, icon, order, and visibility.
 * Backend computes the actual value at runtime based on statKey.
 *
 * Known statKey values (backend computes these):
 *   ACTIVE_LISTINGS  — count of live listings
 *   DISTRICTS        — distinct districts with at least one listing
 *   VERIFIED_SELLERS — sellers with an APPROVED business profile
 *   TODAY_NEW        — listings created today
 *   TOTAL_SELLERS    — distinct sellers ever posted
 *   PREMIUM_SELLERS  — sellers with an active subscription
 */
@Entity
@Table(name = "stat_tiles")
@Data
public class StatTile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Machine key — backend computes value from this. */
    @Column(name = "stat_key", unique = true, nullable = false, length = 40)
    private String statKey;

    @Column(name = "label_en", nullable = false, length = 60)
    private String labelEn;

    @Column(name = "label_hi", nullable = false, length = 60)
    private String labelHi;

    /** Emoji or icon key shown on the tile. */
    @Column(name = "icon", nullable = false, length = 10)
    private String icon;

    /** Lower = shown first. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
