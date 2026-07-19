package com.hyperlocal.tantra.modules.master.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_modules")
@Data
public class AppModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "module_key", unique = true, nullable = false, length = 50)
    private String moduleKey;

    @Column(name = "module_name_en", nullable = false, length = 100)
    private String moduleNameEn;

    @Column(name = "module_name_hi", nullable = false, length = 100)
    private String moduleNameHi;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}