package com.mawa3id.auth.contact;

import com.mawa3id.auth.support.Identifiers;
import com.mawa3id.common.ApiException;
import com.mawa3id.user.User;
import com.mawa3id.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * The source of truth for login/recovery identifiers. Resolves a presented identifier
 * (email / username / phone) to a user and manages the {@code user_contacts} rows that
 * back verification and recovery.
 */
@Service
public class ContactService {

    private final UserContactRepository contacts;
    private final UserRepository users;

    public ContactService(UserContactRepository contacts, UserRepository users) {
        this.contacts = contacts;
        this.users = users;
    }

    /** Resolve any identifier form to its owning user, if one exists. */
    @Transactional(readOnly = true)
    public Optional<User> resolveUser(String identifier) {
        return switch (Identifiers.classify(identifier)) {
            case EMAIL -> {
                String normalized = Identifiers.normalizeEmail(identifier);
                yield contacts.findByTypeAndNormalizedValue(ContactType.EMAIL, normalized)
                        .map(UserContact::getUser)
                        .or(() -> users.findByEmail(normalized));
            }
            case PHONE -> {
                Optional<String> normalized = tryNormalizePhone(identifier);
                yield normalized.flatMap(n ->
                        contacts.findByTypeAndNormalizedValue(ContactType.PHONE, n).map(UserContact::getUser));
            }
            case USERNAME -> users.findByUsername(Identifiers.normalizeUsername(identifier));
        };
    }

    /** True if the email is already claimed as a user email or a contact. */
    @Transactional(readOnly = true)
    public boolean isEmailTaken(String normalizedEmail) {
        return users.existsByEmail(normalizedEmail)
                || contacts.existsByTypeAndNormalizedValue(ContactType.EMAIL, normalizedEmail);
    }

    /** Create the primary EMAIL contact for a freshly registered user (unverified). */
    @Transactional
    public UserContact createPrimaryEmail(User user, String rawEmail, String normalizedEmail) {
        return contacts.save(new UserContact(user, ContactType.EMAIL, rawEmail.trim(), normalizedEmail, true));
    }

    @Transactional(readOnly = true)
    public Optional<UserContact> primaryEmail(User user) {
        return contacts.findByUserIdAndTypeAndPrimaryTrue(user.getId(), ContactType.EMAIL);
    }

    /** True if the user's primary email contact has been verified. */
    @Transactional(readOnly = true)
    public boolean hasVerifiedEmail(User user) {
        return primaryEmail(user).map(UserContact::isVerified).orElse(false);
    }

    /** True if the user has any verified phone contact. */
    @Transactional(readOnly = true)
    public boolean hasVerifiedPhone(User user) {
        return contacts.existsByUserIdAndTypeAndVerifiedAtNotNull(user.getId(), ContactType.PHONE);
    }

    /**
     * Add (or return the existing) phone contact for a user. Rejects a phone already
     * verified by a different account so numbers are not silently shared.
     */
    @Transactional
    public UserContact addPhone(User user, String rawPhone) {
        String normalized;
        try {
            normalized = Identifiers.normalizePhone(rawPhone);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not a valid phone number");
        }
        Optional<UserContact> existing = contacts.findByTypeAndNormalizedValue(ContactType.PHONE, normalized);
        if (existing.isPresent()) {
            UserContact contact = existing.get();
            if (!contact.getUser().getId().equals(user.getId())) {
                throw new ApiException(HttpStatus.CONFLICT, "Phone number already in use");
            }
            return contact;
        }
        return contacts.save(new UserContact(user, ContactType.PHONE, rawPhone.trim(), normalized, false));
    }

    /** Find a user's phone contact by its normalized value. */
    @Transactional(readOnly = true)
    public Optional<UserContact> findPhone(User user, String normalizedPhone) {
        return contacts.findByTypeAndNormalizedValue(ContactType.PHONE, normalizedPhone)
                .filter(c -> c.getUser().getId().equals(user.getId()));
    }

    @Transactional
    public void markVerified(UserContact contact, Instant now) {
        contact.markVerified(now);
        contacts.save(contact);
    }

    private Optional<String> tryNormalizePhone(String raw) {
        try {
            return Optional.of(Identifiers.normalizePhone(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
