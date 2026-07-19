package com.mawa3id.notification;

import com.mawa3id.user.User;
import com.mawa3id.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Manages the push-notification device tokens the mobile clients register. A token is
 * globally unique: re-registering an existing token simply re-points it at the caller (a
 * device that switched accounts) and refreshes {@code last_seen_at}.
 */
@Service
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository, UserRepository userRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userRepository = userRepository;
    }

    /** Upsert the token for the given user, re-attributing it if it currently belongs elsewhere. */
    @Transactional
    public void register(Long userId, String token, String platformHint) {
        DevicePlatform platform = parsePlatform(platformHint);
        deviceTokenRepository.findByToken(token).ifPresentOrElse(existing -> {
            if (existing.getRecipient().getId().equals(userId)) {
                existing.touch();
                if (platform != DevicePlatform.UNKNOWN) {
                    existing.reassignTo(existing.getRecipient(), platform);
                }
            } else {
                existing.reassignTo(userReference(userId), platform);
            }
        }, () -> {
            try {
                deviceTokenRepository.saveAndFlush(new DeviceToken(userReference(userId), token, platform));
            } catch (DataIntegrityViolationException race) {
                // Concurrent insert of the same token won the unique constraint; adopt it.
                deviceTokenRepository.findByToken(token)
                        .ifPresent(other -> other.reassignTo(userReference(userId), platform));
            }
        });
    }

    /** Remove the token (idempotent — a no-op if it is already gone). */
    @Transactional
    public void unregister(String token) {
        deviceTokenRepository.deleteByToken(token);
    }

    /** The raw push tokens registered for a user, for dispatching a delivery. */
    @Transactional(readOnly = true)
    public List<String> tokensFor(Long userId) {
        return deviceTokenRepository.findByRecipientId(userId).stream()
                .map(DeviceToken::getToken)
                .toList();
    }

    private User userReference(Long userId) {
        return userRepository.getReferenceById(userId);
    }

    private static DevicePlatform parsePlatform(String hint) {
        if (hint == null || hint.isBlank()) {
            return DevicePlatform.UNKNOWN;
        }
        try {
            return DevicePlatform.valueOf(hint.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DevicePlatform.UNKNOWN;
        }
    }
}
