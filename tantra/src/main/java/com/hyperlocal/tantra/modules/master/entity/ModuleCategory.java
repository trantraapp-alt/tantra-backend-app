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

    @Column(name = "category_key", nullable = false, length = 50)
    private String categoryKey;

    @Column(name = "category_name_en", nullable = false, length = 100)
    private String categoryNameEn;

    @Column(name = "category_name_hi", nullable = false, length = 100)
    private String categoryNameHi;

    @Column(name = "icon_url", length = 255)
    private String iconUrl;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}