package com.mawa3id.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for push notifications (FCM), bound from {@code mawa3id.push.*}.
 * Credentials are supplied via environment variables; nothing sensitive is committed.
 * When unconfigured the feature is a no-op (see {@link FcmPushSender}).
 */
@Component
@ConfigurationProperties(prefix = "mawa3id.push")
public class PushProperties {

    /** Master switch. When false, no pushes are sent regardless of credentials. */
    private boolean enabled = true;

    /**
     * Service-account credentials as inline JSON (FCM_CREDENTIALS). When blank the sender
     * falls back to Application Default Credentials (GOOGLE_APPLICATION_CREDENTIALS path).
     */
    private String credentials = "";

    /** Optional explicit Firebase project id; usually inferred from the credentials. */
    private String projectId = "";

    /** True only when push is switched on and some credentials source is available. */
    public boolean isConfigured() {
        return enabled && !credentials.isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
