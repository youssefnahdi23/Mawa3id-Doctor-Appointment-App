package com.mawa3id.auth.audit;

import com.mawa3id.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records security events to the append-only audit trail. Callers pass already-masked
 * details; nothing sensitive (passwords, raw secrets, full identifiers) is stored.
 */
@Service
public class AuthAuditService {

    private final AuthAuditEventRepository repository;

    public AuthAuditService(AuthAuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(User user, AuthEventType type, String detail, String ipAddress) {
        repository.save(new AuthAuditEvent(user, type, truncate(detail), ipAddress));
    }

    private String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        return detail.length() <= 500 ? detail : detail.substring(0, 500);
    }
}
