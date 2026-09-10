package com.payroll.service;

import com.payroll.model.AuditLog;
import com.payroll.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String action, String module, String details) {
        String performedBy = "SYSTEM";
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().isBlank() && !"anonymousUser".equals(auth.getName())) {
                performedBy = auth.getName();
            }
        } catch (Exception ignore) {}

        AuditLog auditLog = new AuditLog(action, module, performedBy, details);
        auditLogRepository.save(auditLog);
        log.info("[AUDIT] {} | {} | by {} | {}", module, action, performedBy, details);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsByModule(String module) {
        return auditLogRepository.findByModuleOrderByTimestampDesc(module);
    }
}
