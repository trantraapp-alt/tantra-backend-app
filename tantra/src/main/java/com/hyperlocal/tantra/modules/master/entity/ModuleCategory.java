package com.hyperlocal.tantra.modules.master.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "module_categories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"module_id", "category_key"})
})
@Data
public class ModuleCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "module_id", nullable = false)
    private Integer moduleId;

    /** Self-referencing tree: null = top-level category under the module; else a subcategory. */
    @Column(name = "parent_id")
    private Integer parentId;

    @Column(name = "category_key", nullable = false, length = 50)
    private String categoryKey;

    @Column(name = "category_name_en", nullable = false, length = 100)
    private String categoryNameEn;

    @Column(name = "category_name_hi", nullable = false, length = 100)
    private String categoryNameHi;

    @Column(name = "icon_url", length = 255)
    private String iconUrl;

    /** What tapping this category does: "LISTING" (open the form → post a listing) or "BUSINESS_PROFILE". */
    @Column(name = "action_type", length = 20)
    private String actionType = "LISTING";

    /** For BUSINESS_PROFILE categories: the business_profile_type to prefill (e.g. "vet_clinic"). */
    @Column(name = "link_key", length = 60)
    private String linkKey;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Quality assurance text shown on listing detail page — English. Set by admin per category. */
    @Column(name = "quality_desc_en", columnDefinition = "TEXT")
    private String qualityDescEn;

    /** Quality assurance text shown on listing detail page — Hindi. Set by admin per category. */
    @Column(name = "quality_desc_hi", columnDefinition = "TEXT")
    private String qualityDescHi;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}