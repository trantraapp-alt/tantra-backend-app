package com.hyperlocal.tantra.modules.msp.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Government Minimum Support Price (MSP) for agricultural crops.
 * Admin-maintained. Updated each season when the central government announces new MSP.
 *
 * Table: msp_prices
 */
@Entity
@Table(name = "msp_prices", indexes = {
        @Index(name = "idx_msp_season_year", columnList = "season, year"),
        @Index(name = "idx_msp_crop",        columnList = "crop_key")
})
@Data
public class MspPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique crop identifier, e.g. "wheat", "paddy", "tur_dal", "cotton". */
    @Column(name = "crop_key", nullable = false, length = 50)
    private String cropKey;

    /** Localized crop name: {"en":"Wheat","hi":"गेहूं"} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "crop_name", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> cropName;

    /** Crop emoji for display: "🌾" */
    @Column(name = "emoji", length = 10)
    private String emoji;

    /** Season: "RABI" | "KHARIF" | "COMMERCIAL" */
    @Column(name = "season", length = 20, nullable = false)
    private String season;

    /** Announcement year, e.g. 2026. */
    @Column(name = "year", nullable = false)
    private Integer year;

    /** MSP in ₹ per quintal. */
    @Column(name = "price_per_quintal", precision = 10, scale = 2, nullable = false)
    private BigDecimal pricePerQuintal;

    /** Previous year's MSP — used to compute YoY increase. */
    @Column(name = "prev_price_per_quintal", precision = 10, scale = 2)
    private BigDecimal prevPricePerQuintal;

    /** Official government notification date. */
    @Column(name = "notified_on")
    private LocalDate notifiedOn;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
