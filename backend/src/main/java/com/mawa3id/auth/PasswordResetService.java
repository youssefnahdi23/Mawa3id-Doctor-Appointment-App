package com.mawa3id.auth;

import com.mawa3id.auth.audit.AuthAuditService;
import com.mawa3id.auth.audit.AuthEventType;
import com.mawa3id.auth.challenge.AuthChallenge;
import com.mawa3id.auth.challenge.ChallengeChannel;
import com.mawa3id.auth.challenge.ChallengePurpose;
import com.mawa3id.auth.challenge.ChallengeService;
import com.mawa3id.auth.challenge.ChallengeSender;
import com.mawa3id.auth.contact.ContactService;
import com.mawa3id.auth.contact.UserContact;
import com.mawa3id.auth.session.RefreshTokenService;
import com.mawa3id.auth.support.Identifiers;
import com.mawa3id.common.ApiException;
import com.mawa3id.user.User;
import com.mawa3id.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Email-based password reset. "Forgot" issues a high-entropy token to the account's
 * email and always returns the same acknowledgement so it cannot be used to probe which
 * identifiers exist. "Reset" consumes the token, sets the new password and revokes all
 * existing sessions so a leaked password cannot outlive the reset.
 */
@Service
public class PasswordResetService {

    private final ContactService contactService;
    private final ChallengeService challengeService;
    private final ChallengeSender challengeSender;
    private final RefreshTokenService refreshTokenService;
    private final AuthAuditService auditService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public PasswordResetService(ContactService contactService, ChallengeService challengeService,
                                ChallengeSender challengeSender, RefreshTokenService refreshTokenService,
                                AuthAuditService auditService, UserRepository userRepository,
                                PasswordEncoder passwordEncoder, Clock clock) {
        this.contactService = contactService;
        this.challengeService = challengeService;
        this.challengeSender = challengeSender;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Begin a reset. Only ever issues a token when the identifier maps to a user that
     * has an email on file; otherwise it silently no-ops. The caller responds
     * identically in all cases.
     */
    @Transactional
    public void forgot(String identifier, String ip) {
        Optional<User> user = contactService.resolveUser(identifier);
        if (user.isEmpty()) {
            return;
        }
        Optional<UserContact> email = contactService.primaryEmail(user.get());
        if (email.isEmpty()) {
            return;
        }
        String destination = email.get().getNormalizedValue();
        String token = challengeService.issueToken(user.get(), ChallengePurpose.RESET_EMAIL,
                ChallengeChannel.EMAIL, destination);
        challengeSender.send(ChallengeChannel.EMAIL, destination, ChallengePurpose.RESET_EMAIL, token);
        auditService.record(user.get(), AuthEventType.PASSWORD_RESET_REQUESTED,
                Identifiers.mask(destination), ip);
    }

    @Transactional
    public void reset(String token, String newPassword, String ip) {
        AuthChallenge challenge = challengeService.verifyToken(ChallengePurpose.RESET_EMAIL, token);
        User user = challenge.getUser();
        if (user == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(clock.instant());
        userRepository.save(user);
        // A reset implies the old password may be compromised: drop every live session.
        refreshTokenService.revokeAllForUser(user.getId());
        auditService.record(user, AuthEventType.PASSWORD_RESET_COMPLETED, null, ip);
    }
}
