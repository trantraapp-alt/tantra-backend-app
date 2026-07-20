package com.hyperlocal.tantra.modules.forms.entity;

import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * A reusable dropdown definition (e.g. "crop_name", "unit_of_measure", "state").
 * Managed by admins via the dropdown-maintenance API and shared across many form fields/modules.
 */
@Entity
@Table(name = "option_sets")
@Data
public class OptionSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "set_key", unique = true, nullable = false, length = 60)
    private String setKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "name", columnDefinition = "jsonb")
    private LocalizedText name;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
