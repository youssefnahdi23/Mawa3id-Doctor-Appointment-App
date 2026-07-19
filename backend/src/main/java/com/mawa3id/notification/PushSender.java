package com.mawa3id.notification;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Abstraction over a push-notification transport (FCM in production). The production
 * implementation talks to Firebase Cloud Messaging; tests supply a capturing stub so the
 * dispatch flow can be exercised without network access or credentials.
 */
public interface PushSender {

    /**
     * Deliver a message to the given device tokens. Never throws for delivery failures —
     * transient errors are swallowed/logged; only tokens the provider reports as
     * permanently unregistered are returned so the caller can prune them.
     *
     * @return the subset of {@code tokens} that are unregistered/invalid and should be deleted.
     */
    List<String> send(Collection<String> tokens, PushMessage message);

    /** A provider-neutral push payload. {@code data} is delivered as string key/values. */
    record PushMessage(String title, String body, Map<String, String> data) {
    }
}
