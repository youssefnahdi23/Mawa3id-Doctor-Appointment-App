package com.mawa3id.auth.audit;

/** Security-relevant events recorded to {@code auth_audit_events}. */
public enum AuthEventType {
    REGISTERED,
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    TOKEN_REFRESHED,
    REFRESH_REUSE_DETECTED,
    LOGGED_OUT,
    LOGGED_OUT_ALL,
    CONTACT_VERIFICATION_REQUESTED,
    CONTACT_VERIFIED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED
}
