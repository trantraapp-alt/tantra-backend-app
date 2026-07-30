package com.hyperlocal.tantra.modules.audit.repository;

import com.hyperlocal.tantra.modules.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);
    List<AuditLog> findByActorUserIdOrderByCreatedAtDesc(String actorUserId);
}
