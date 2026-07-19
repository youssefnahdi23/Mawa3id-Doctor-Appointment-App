package com.mawa3id.notification;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Capturing {@link PushSender} used under the {@code test} profile so the push dispatch flow
 * runs in CI with no Firebase credentials or network. Records every send for assertions and
 * lets a test mark specific tokens as unregistered to exercise token cleanup.
 */
@Component
@Profile("test")
public class StubPushSender implements PushSender {

    /** One recorded delivery. */
    public record Sent(List<String> tokens, PushMessage message) {
    }

    private final List<Sent> sent = new CopyOnWriteArrayList<>();
    private final Set<String> unregistered = ConcurrentHashMap.newKeySet();

    @Override
    public List<String> send(Collection<String> tokens, PushMessage message) {
        sent.add(new Sent(List.copyOf(tokens), message));
        return tokens.stream().filter(unregistered::contains).toList();
    }

    public List<Sent> sent() {
        return sent;
    }

    /** Report {@code token} as unregistered on the next send so the dispatcher prunes it. */
    public void markUnregistered(String token) {
        unregistered.add(token);
    }

    public void reset() {
        sent.clear();
        unregistered.clear();
    }
}
