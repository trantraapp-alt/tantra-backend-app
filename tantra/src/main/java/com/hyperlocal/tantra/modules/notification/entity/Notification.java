package com.hyperlocal.tantra.modules.notification.entity;

import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * In-app notification for a user (verification result, contact request, …). Bilingual title/body.
 * FCM push + SMS delivery are layered on top later; this table is the source of truth.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_user", columnList = "user_id,is_read")
})
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(name = "type", length = 40)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "title", columnDefinition = "jsonb")
    private LocalizedText title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "body", columnDefinition = "jsonb")
    private LocalizedText body;

    /** What the notification points at (e.g. BUSINESS_PROFILE / LISTING) + its id, for deep-linking. */
    @Column(name = "ref_type", length = 40)
    private String refType;

    @Column(name = "ref_id", length = 40)
    private String refId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
