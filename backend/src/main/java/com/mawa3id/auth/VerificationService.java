package com.mawa3id.auth;

import com.mawa3id.auth.audit.AuthAuditService;
import com.mawa3id.auth.audit.AuthEventType;
import com.mawa3id.auth.challenge.ChallengeChannel;
import com.mawa3id.auth.challenge.ChallengePurpose;
import com.mawa3id.auth.challenge.ChallengeService;
import com.mawa3id.auth.challenge.ChallengeSender;
import com.mawa3id.auth.contact.ContactService;
import com.mawa3id.auth.contact.UserContact;
import com.mawa3id.auth.support.Identifiers;
import com.mawa3id.common.ApiException;
import com.mawa3id.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Email and phone ownership verification. A short numeric code is issued to the
 * contact and delivered via the pluggable {@link ChallengeSender}; confirming the code
 * stamps {@code verified_at} on the contact.
 */
@Service
public class VerificationService {

    private final ContactService contactService;
    private final ChallengeService challengeService;
    private final ChallengeSender challengeSender;
    private final AuthAuditService auditService;
    private final Clock clock;

    public VerificationService(ContactService contactService, ChallengeService challengeService,
                               ChallengeSender challengeSender, AuthAuditService auditService, Clock clock) {
        this.contactService = contactService;
        this.challengeService = challengeService;
        this.challengeSender = challengeSender;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public void requestEmailVerification(User user, String ip) {
        UserContact email = contactService.primaryEmail(user)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No email on file to verify"));
        if (email.isVerified()) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already verified");
        }
        String code = challengeService.issueCode(user, ChallengePurpose.VERIFY_EMAIL,
                ChallengeChannel.EMAIL, email.getNormalizedValue());
        challengeSender.send(ChallengeChannel.EMAIL, email.getNormalizedValue(),
                ChallengePurpose.VERIFY_EMAIL, code);
        auditService.record(user, AuthEventType.CONTACT_VERIFICATION_REQUESTED,
                "email " + Identifiers.mask(email.getNormalizedValue()), ip);
    }

    @Transactional
    public void confirmEmail(User user, String code, String ip) {
        UserContact email = contactService.primaryEmail(user)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No email on file to verify"));
        challengeService.verifyCode(ChallengePurpose.VERIFY_EMAIL, email.getNormalizedValue(), code);
        contactService.markVerified(email, clock.instant());
        auditService.record(user, AuthEventType.CONTACT_VERIFIED,
                "email " + Identifiers.mask(email.getNormalizedValue()), ip);
    }

    @Transactional
    public void requestPhoneVerification(User user, String rawPhone, String ip) {
        UserContact phone = contactService.addPhone(user, rawPhone);
        if (phone.isVerified()) {
            throw new ApiException(HttpStatus.CONFLICT, "Phone is already verified");
        }
        String code = challengeService.issueCode(user, ChallengePurpose.VERIFY_PHONE,
                ChallengeChannel.PHONE, phone.getNormalizedValue());
        challengeSender.send(ChallengeChannel.PHONE, phone.getNormalizedValue(),
                ChallengePurpose.VERIFY_PHONE, code);
        auditService.record(user, AuthEventType.CONTACT_VERIFICATION_REQUESTED,
                "phone " + Identifiers.mask(phone.getNormalizedValue()), ip);
    }

    @Transactional
    public void confirmPhone(User user, String rawPhone, String code, String ip) {
        String normalized = normalizePhoneOr400(rawPhone);
        UserContact phone = contactService.findPhone(user, normalized)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No such phone to verify"));
        challengeService.verifyCode(ChallengePurpose.VERIFY_PHONE, normalized, code);
        contactService.markVerified(phone, clock.instant());
        auditService.record(user, AuthEventType.CONTACT_VERIFIED,
                "phone " + Identifiers.mask(normalized), ip);
    }

    private String normalizePhoneOr400(String rawPhone) {
        try {
            return Identifiers.normalizePhone(rawPhone);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not a valid phone number");
        }
    }
}
