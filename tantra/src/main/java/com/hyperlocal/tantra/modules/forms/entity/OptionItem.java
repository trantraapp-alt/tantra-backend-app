package com.hyperlocal.tantra.modules.forms.entity;

import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * A single value inside an {@link OptionSet}, e.g. Wheat / गेहूं. Each value is its own row so the
 * admin UI can add, edit, reorder or deactivate one value at a time.
 *
 * <p>{@code parentItemId} enables cascading dropdowns (State depends on Country, City on State).
 * For cascading geo sets, keep {@code itemKey} fully-qualified (e.g. "mp_bhopal") to stay unique.</p>
 */
@Entity
@Table(name = "option_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"option_set_id", "item_key"})
})
@Data
public class OptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "option_set_id", nullable = false)
    private Integer optionSetId;

    @Column(name = "parent_item_id")
    private Integer parentItemId;

    @Column(name = "item_key", nullable = false, length = 80)
    private String itemKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "label", columnDefinition = "jsonb")
    private LocalizedText label;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
