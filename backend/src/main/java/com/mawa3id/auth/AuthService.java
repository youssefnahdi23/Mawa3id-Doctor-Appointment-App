package com.mawa3id.auth;

import com.mawa3id.auth.audit.AuthAuditService;
import com.mawa3id.auth.audit.AuthEventType;
import com.mawa3id.auth.contact.ContactService;
import com.mawa3id.auth.dto.AuthResponse;
import com.mawa3id.auth.dto.LoginRequest;
import com.mawa3id.auth.dto.RegisterDoctorRequest;
import com.mawa3id.auth.dto.RegisterPatientRequest;
import com.mawa3id.auth.session.RefreshTokenService;
import com.mawa3id.auth.support.Identifiers;
import com.mawa3id.common.ApiException;
import com.mawa3id.doctor.Doctor;
import com.mawa3id.doctor.DoctorRepository;
import com.mawa3id.doctor.DoctorService;
import com.mawa3id.patient.Patient;
import com.mawa3id.patient.PatientRepository;
import com.mawa3id.patient.PatientService;
import com.mawa3id.security.JwtService;
import com.mawa3id.specialty.Specialty;
import com.mawa3id.user.Role;
import com.mawa3id.user.User;
import com.mawa3id.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;

/**
 * Registration and session issuance. Accounts are keyed by a unique username (the JWT
 * subject); email/username/phone all resolve to it for login. Every login issues a
 * short-lived access JWT plus a rotating refresh token, and security-relevant events
 * are written to the audit trail.
 */
@Service
public class AuthService {

    // A well-formed bcrypt hash that matches nothing, compared against when the
    // identifier is unknown so login latency does not reveal whether an account exists.
    private static final String DUMMY_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOa8T3pQ0d3H3l2m4n5o6p7q8r9s0t1u2";

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ContactService contactService;
    private final RefreshTokenService refreshTokenService;
    private final AuthAuditService auditService;
    private final Clock clock;

    public AuthService(UserRepository userRepository, PatientRepository patientRepository,
                       DoctorRepository doctorRepository, PatientService patientService,
                       DoctorService doctorService, PasswordEncoder passwordEncoder,
                       JwtService jwtService, ContactService contactService,
                       RefreshTokenService refreshTokenService, AuthAuditService auditService,
                       Clock clock) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.contactService = contactService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse registerPatient(RegisterPatientRequest request, String userAgent, String ip) {
        User user = createUser(request.email(), request.password(), Role.PATIENT);
        Patient patient = new Patient(user, request.fullName(), request.dateOfBirth(),
                patientService.generateUniquePatientCode());
        patientRepository.save(patient);
        auditService.record(user, AuthEventType.REGISTERED, "patient", ip);
        return issueTokens(user, null, userAgent, ip);
    }

    @Transactional
    public AuthResponse registerDoctor(RegisterDoctorRequest request, String userAgent, String ip) {
        Specialty specialty = doctorService.resolveSpecialty(request.specialtyId());
        User user = createUser(request.email(), request.password(), Role.DOCTOR);
        Doctor doctor = new Doctor(user, request.name(), specialty, request.cabinetAddress(),
                request.workingHours(), request.phone());
        doctorRepository.save(doctor);
        auditService.record(user, AuthEventType.REGISTERED, "doctor", ip);
        return issueTokens(user, null, userAgent, ip);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent, String ip) {
        User user = contactService.resolveUser(request.identifier()).orElse(null);
        String hash = user != null ? user.getPasswordHash() : null;

        // Always run a bcrypt comparison (against a dummy hash when the account is
        // unknown or password-less) so timing does not distinguish the cases.
        boolean passwordOk = passwordEncoder.matches(request.password(), hash != null ? hash : DUMMY_HASH);

        if (user == null || hash == null || !passwordOk || !user.isActive()) {
            auditService.record(user, AuthEventType.LOGIN_FAILED,
                    Identifiers.mask(request.identifier()), ip);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        user.setLastLoginAt(clock.instant());
        userRepository.save(user);
        auditService.record(user, AuthEventType.LOGIN_SUCCEEDED, null, ip);
        return issueTokens(user, request.deviceName(), userAgent, ip);
    }

    /**
     * Rotate a refresh token into a fresh access + refresh pair. Not wrapped in a
     * single transaction: {@link RefreshTokenService#rotate} commits its own work
     * (notably family revocation on reuse) before this method may throw a 401.
     */
    public AuthResponse refresh(String refreshToken, String userAgent, String ip) {
        RefreshTokenService.RotationResult result =
                refreshTokenService.rotate(refreshToken, userAgent, ip);
        return switch (result.status()) {
            case ROTATED -> {
                User user = result.user();
                auditService.record(user, AuthEventType.TOKEN_REFRESHED, null, ip);
                String access = accessToken(user);
                yield AuthResponse.of(access, jwtService.getExpirationMs(), result.token(),
                        user.getId(), user.getUsername(), user.getEmail(), user.getRole());
            }
            case REUSE -> {
                auditService.record(result.user(), AuthEventType.REFRESH_REUSE_DETECTED,
                        "refresh token replay; family revoked", ip);
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is no longer valid");
            }
            case INVALID -> throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Refresh token is no longer valid");
        };
    }

    public void logout(String refreshToken, String ip) {
        refreshTokenService.revoke(refreshToken)
                .ifPresent(user -> auditService.record(user, AuthEventType.LOGGED_OUT, null, ip));
    }

    @Transactional
    public void logoutAll(User user, String ip) {
        refreshTokenService.revokeAllForUser(user.getId());
        auditService.record(user, AuthEventType.LOGGED_OUT_ALL, null, ip);
    }

    private User createUser(String email, String rawPassword, Role role) {
        String normalizedEmail = Identifiers.normalizeEmail(email);
        if (contactService.isEmailTaken(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User(generateUsername(normalizedEmail), normalizedEmail,
                passwordEncoder.encode(rawPassword), role);
        user.setPasswordChangedAt(clock.instant());
        userRepository.save(user);
        contactService.createPrimaryEmail(user, email, normalizedEmail);
        return user;
    }

    /** Derive a unique username from the email local part, suffixing on collision. */
    private String generateUsername(String normalizedEmail) {
        String local = normalizedEmail.substring(0, normalizedEmail.indexOf('@'));
        String base = local.replaceAll("[^a-z0-9._-]", "");
        if (base.length() < 3) {
            base = "user";
        } else if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "." + (++suffix);
        }
        return candidate;
    }

    private AuthResponse issueTokens(User user, String deviceName, String userAgent, String ip) {
        String access = accessToken(user);
        RefreshTokenService.IssuedRefresh refresh =
                refreshTokenService.issue(user, deviceName, userAgent, ip);
        return AuthResponse.of(access, jwtService.getExpirationMs(), refresh.token(),
                user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    private String accessToken(User user) {
        return jwtService.generateToken(user.getUsername(),
                Map.of("uid", user.getId(), "role", user.getRole().name()));
    }
}
