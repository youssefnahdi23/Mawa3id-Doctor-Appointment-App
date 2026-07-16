package com.mawa3id.user;

/**
 * Account lifecycle state. Only {@link #ACTIVE} accounts may authenticate; a
 * {@link #DISABLED} account is rejected at login without leaking whether the
 * credentials were otherwise valid.
 */
public enum UserStatus {
    ACTIVE,
    DISABLED
}
