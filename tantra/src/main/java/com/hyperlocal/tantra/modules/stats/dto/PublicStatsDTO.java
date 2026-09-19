package com.hyperlocal.tantra.modules.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Aggregated trust stats shown on the home feed trust bar. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublicStatsDTO {

    /** Total distinct sellers who have ever posted a listing. */
    private long sellerCount;

    /** Distinct districts covered by at least one listing. */
    private long districtCount;

    /**
     * Sum of (offered_price * quantity) for all active listings.
     * Represents approximate total trade value facilitated by the platform.
     */
    private BigDecimal totalTradeValue;

    /** Fixed satisfaction percentage — updated manually or via survey integration. */
    private int satisfiedPct = 98;
}
