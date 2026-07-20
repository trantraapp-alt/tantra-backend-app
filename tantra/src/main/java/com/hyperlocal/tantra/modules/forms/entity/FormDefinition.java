package com.hyperlocal.tantra.modules.forms.entity;

import com.hyperlocal.tantra.modules.forms.model.FormField;
import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The form for one category + listing type. The whole field list is a jsonb document so a form is
 * edited as one unit and rendered almost directly. {@code version} keeps old listings mapped to the
 * fields they were submitted against when a form is later changed.
 */
@Entity
@Table(name = "form_definitions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"category_id", "listing_type", "version"})
})
@Data
public class FormDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 10)
    private ListingType listingType = ListingType.SELL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "title", columnDefinition = "jsonb")
    private LocalizedText title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fields", columnDefinition = "jsonb")
    private List<FormField> fields = new ArrayList<>();

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
