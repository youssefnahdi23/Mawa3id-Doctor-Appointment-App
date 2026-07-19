package com.mawa3id.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Production {@link PushSender} backed by Firebase Cloud Messaging (FCM), which also fronts
 * APNs for iOS.
 *
 * <p>Excluded from the test profile (and the JaCoCo coverage gate) because it makes real
 * network calls to Firebase that cannot run in CI without live credentials. The dispatch
 * logic that <em>uses</em> this sender is fully covered via the test-profile
 * {@code StubPushSender}. When credentials are absent it degrades to a no-op so the app runs
 * without a Firebase project.
 */
@Component
@Profile("!test")
public class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);
    private static final String APP_NAME = "mawa3id-fcm";
    /** FCM caps a multicast at 500 tokens per request. */
    private static final int BATCH_SIZE = 500;

    private final PushProperties properties;
    private volatile FirebaseMessaging messaging;

    public FcmPushSender(PushProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        if (!properties.isConfigured()) {
            log.info("Push notifications disabled: no FCM credentials configured (mawa3id.push).");
            return;
        }
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(properties.getCredentials().getBytes(StandardCharsets.UTF_8)));
            FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(credentials);
            if (!properties.getProjectId().isBlank()) {
                options.setProjectId(properties.getProjectId());
            }
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(a -> APP_NAME.equals(a.getName()))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(options.build(), APP_NAME));
            this.messaging = FirebaseMessaging.getInstance(app);
            log.info("Push notifications enabled (FCM).");
        } catch (Exception e) {
            log.error("Failed to initialise FCM; push notifications disabled: {}", e.getMessage());
        }
    }

    @Override
    public List<String> send(Collection<String> tokens, PushMessage message) {
        if (messaging == null || tokens.isEmpty()) {
            return List.of();
        }
        List<String> all = List.copyOf(tokens);
        List<String> unregistered = new ArrayList<>();
        for (int start = 0; start < all.size(); start += BATCH_SIZE) {
            List<String> chunk = all.subList(start, Math.min(start + BATCH_SIZE, all.size()));
            unregistered.addAll(sendChunk(chunk, message));
        }
        return unregistered;
    }

    private List<String> sendChunk(List<String> tokens, PushMessage message) {
        MulticastMessage multicast = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(message.data())
                .build();
        try {
            BatchResponse response = messaging.sendEachForMulticast(multicast);
            List<String> unregistered = new ArrayList<>();
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse sr = responses.get(i);
                if (!sr.isSuccessful() && isUnregistered(sr)) {
                    unregistered.add(tokens.get(i));
                }
            }
            return unregistered;
        } catch (Exception e) {
            // Delivery must never break the caller. Log and move on.
            log.warn("FCM push delivery failed for {} token(s): {}", tokens.size(), e.getMessage());
            return List.of();
        }
    }

    private static boolean isUnregistered(SendResponse sr) {
        if (sr.getException() == null) {
            return false;
        }
        MessagingErrorCode code = sr.getException().getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT;
    }
}
