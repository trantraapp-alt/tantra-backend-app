package com.hyperlocal.tantra.modules.audit.service;

import com.hyperlocal.tantra.common.web.TraceIdFilter;
import com.hyperlocal.tantra.modules.audit.entity.AuditLog;
import com.hyperlocal.tantra.modules.audit.repository.AuditLogRepository;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Records business audit entries. The current request's traceId is captured automatically so the
 * audit row can be tied back to the technical logs.
 */
@Service
public class AuditService {

    @Autowired private AuditLogRepository auditLogRepository;

    public void record(String action, String entityType, String entityId, String actorUserId) {
        record(action, entityType, entityId, actorUserId, null);
    }

    public void record(String action, String entityType, String entityId, String actorUserId,
                       Map<String, Object> details) {
        AuditLog entry = new AuditLog();
        entry.setTraceId(MDC.get(TraceIdFilter.TRACE_ID));
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setActorUserId(actorUserId);
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }
}
