package com.hyperlocal.tantra.modules.filter.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "filter_configs")
@Data
public class FilterConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * GLOBAL → applies to all modules/categories.
     * CATEGORY → overrides a matching filter_key when scope_key = category_key.
     */
    @Column(name = "scope", nullable = false, length = 20)
    private String scope;

    /**
     * NULL for GLOBAL scope; category_key string for CATEGORY scope.
     * Deliberately not a FK — categories are created via API with runtime IDs.
     */
    @Column(name = "scope_key", length = 50)
    private String scopeKey;

    /** Stable identifier consumed by the frontend: "listingType", "priceRange", etc. */
    @Column(name = "filter_key", nullable = false, length = 50)
    private String filterKey;

    @Column(name = "label_en", nullable = false, length = 100)
    private String labelEn;

    @Column(name = "label_hi", nullable = false, length = 100)
    private String labelHi;

    /** CHIP_SELECT or RANGE */
    @Column(name = "filter_type", nullable = false, length = 20)
    private String filterType;

    /**
     * JSONB payload — shape depends on filterType:
     *   CHIP_SELECT → { options:[{value, labelEn, labelHi}], defaultValue, multiSelect }
     *   RANGE       → { min, max, step, displayMin, displayMax }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> config;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
