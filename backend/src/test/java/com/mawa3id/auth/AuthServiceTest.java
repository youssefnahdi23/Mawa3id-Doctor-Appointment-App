package com.mawa3id.auth;

import com.mawa3id.auth.audit.AuthAuditService;
import com.mawa3id.auth.audit.AuthEventType;
import com.mawa3id.auth.contact.ContactService;
import com.mawa3id.auth.dto.AuthResponse;
import com.mawa3id.auth.dto.LoginRequest;
import com.mawa3id.auth.dto.RegisterPatientRequest;
import com.mawa3id.auth.session.RefreshTokenService;
import com.mawa3id.common.ApiException;
import com.mawa3id.doctor.DoctorRepository;
import com.mawa3id.doctor.DoctorService;
import com.mawa3id.patient.PatientRepository;
import com.mawa3id.patient.PatientService;
import com.mawa3id.security.JwtService;
import com.mawa3id.user.Role;
import com.mawa3id.user.User;
import com.mawa3id.user.UserRepository;
import com.mawa3id.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link AuthService} branch logic that the integration tests cannot
 * easily reach: duplicate-email conflict, username derivation, the uniform 401 on every
 * login failure mode (unknown / wrong password / disabled), and the refresh outcomes.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private PatientService patientService;
    @Mock private DoctorService doctorService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private ContactService contactService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuthAuditService auditService;

    private AuthService authService;

    @Captor private ArgumentCaptor<User> userCaptor;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-16T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, patientRepository, doctorRepository,
                patientService, doctorService, passwordEncoder, jwtService, contactService,
                refreshTokenService, auditService, CLOCK);
    }

    private RegisterPatientRequest patientRequest(String email) {
        return patientRequest(email, null);
    }

    private RegisterPatientRequest patientRequest(String email, String username) {
        return new RegisterPatientRequest(email, "password123", "Alice Doe",
                LocalDate.of(1990, 5, 20), username);
    }

    /** Persisted users always have an id; the token claim map rejects null values. */
    private static User withId(User user, long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void stubTokenIssuance() {
        when(jwtService.generateToken(anyString(), any())).thenReturn("access-jwt");
        when(jwtService.getExpirationMs()).thenReturn(3_600_000L);
        when(refreshTokenService.issue(any(), any(), any(), any()))
                .thenReturn(new RefreshTokenService.IssuedRefresh("refresh-tok", null));
    }

    @Test
    void registerPatientWithExistingEmailThrowsConflict() {
        when(contactService.isEmailTaken("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerPatient(patientRequest("dup@example.com"), null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void registerPatientNormalizesEmailDerivesUsernameAndIssuesTokens() {
        when(contactService.isEmailTaken("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> withId(inv.getArgument(0), 1L));
        when(patientService.generateUniquePatientCode()).thenReturn("MW-ABC234");
        stubTokenIssuance();

        AuthResponse response = authService.registerPatient(patientRequest("  Alice@Example.COM  "), "ua", "ip");

        assertThat(response.token()).isEqualTo("access-jwt");
        assertThat(response.refreshToken()).isEqualTo("refresh-tok");
        assertThat(response.username()).isEqualTo("alice");
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("alice");
        verify(contactService).createPrimaryEmail(any(User.class), eq("  Alice@Example.COM  "),
                eq("alice@example.com"));
        verify(auditService).record(any(User.class), eq(AuthEventType.REGISTERED), eq("patient"), eq("ip"));
    }

    @Test
    void usernameCollisionGetsNumericSuffix() {
        when(contactService.isEmailTaken(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        when(userRepository.existsByUsername("alice.2")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> withId(inv.getArgument(0), 1L));
        when(patientService.generateUniquePatientCode()).thenReturn("MW-ABC234");
        stubTokenIssuance();

        AuthResponse response = authService.registerPatient(patientRequest("alice@example.com"), null, null);

        assertThat(response.username()).isEqualTo("alice.2");
    }

    @Test
    void registerPatientWithChosenUsernameNormalizesAndUsesIt() {
        when(contactService.isEmailTaken(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("cool.doc")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> withId(inv.getArgument(0), 1L));
        when(patientService.generateUniquePatientCode()).thenReturn("MW-ABC234");
        stubTokenIssuance();

        AuthResponse response =
                authService.registerPatient(patientRequest("alice@example.com", "Cool.Doc"), null, null);

        assertThat(response.username()).isEqualTo("cool.doc");
        verify(userRepository, never()).existsByUsername("alice");
    }

    @Test
    void registerPatientWithTakenUsernameThrowsConflict() {
        when(contactService.isEmailTaken(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.registerPatient(patientRequest("alice@example.com", "taken"), null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerPatientWithMalformedUsernameThrowsBadRequest() {
        when(contactService.isEmailTaken(anyString())).thenReturn(false);

        assertThatThrownBy(() ->
                authService.registerPatient(patientRequest("alice@example.com", "no spaces!"), null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithUnknownIdentifierIsUnauthorizedAndStillRunsDummyCompare() {
        when(contactService.resolveUser("ghost@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(eq("password123"), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("ghost@example.com", "password123", null), null, "ip"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        // A bcrypt comparison runs even for a missing account (timing uniformity).
        verify(passwordEncoder).matches(eq("password123"), anyString());
        verify(auditService).record(isNull(), eq(AuthEventType.LOGIN_FAILED), anyString(), eq("ip"));
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() {
        User user = new User("bob", "bob@example.com", "stored-hash", Role.DOCTOR);
        when(contactService.resolveUser("bob")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("bob", "wrong", null), null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(refreshTokenService, never()).issue(any(), any(), any(), any());
    }

    @Test
    void loginToDisabledAccountIsUnauthorized() {
        User user = new User("bob", "bob@example.com", "stored-hash", Role.DOCTOR);
        user.setStatus(UserStatus.DISABLED);
        when(contactService.resolveUser("bob")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "stored-hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("bob", "password123", null), null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void loginSuccessUpdatesLastLoginAuditsAndIssuesTokens() {
        User user = new User("bob", "bob@example.com", "stored-hash", Role.DOCTOR);
        when(contactService.resolveUser("bob@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "stored-hash")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> withId(inv.getArgument(0), 1L));
        stubTokenIssuance();

        AuthResponse response = authService.login(
                new LoginRequest("bob@example.com", "password123", "Pixel"), "ua", "ip");

        assertThat(response.token()).isEqualTo("access-jwt");
        assertThat(user.getLastLoginAt()).isEqualTo(CLOCK.instant());
        verify(auditService).record(eq(user), eq(AuthEventType.LOGIN_SUCCEEDED), isNull(), eq("ip"));
        verify(refreshTokenService).issue(user, "Pixel", "ua", "ip");
    }

    @Test
    void refreshReuseIsUnauthorizedAndAudited() {
        User user = new User("bob", "bob@example.com", "hash", Role.DOCTOR);
        when(refreshTokenService.rotate("tok", "ua", "ip"))
                .thenReturn(RefreshTokenService.RotationResult.reuse(user));

        assertThatThrownBy(() -> authService.refresh("tok", "ua", "ip"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(auditService).record(eq(user), eq(AuthEventType.REFRESH_REUSE_DETECTED), anyString(), eq("ip"));
    }

    @Test
    void refreshInvalidIsUnauthorized() {
        when(refreshTokenService.rotate(anyString(), any(), any()))
                .thenReturn(RefreshTokenService.RotationResult.invalid());

        assertThatThrownBy(() -> authService.refresh("tok", null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void refreshRotatedReturnsFreshPair() {
        User user = withId(new User("bob", "bob@example.com", "hash", Role.DOCTOR), 7L);
        when(refreshTokenService.rotate("tok", "ua", "ip"))
                .thenReturn(RefreshTokenService.RotationResult.rotated("new-refresh", null, user));
        when(jwtService.generateToken(anyString(), any())).thenReturn("new-access");
        when(jwtService.getExpirationMs()).thenReturn(3_600_000L);

        AuthResponse response = authService.refresh("tok", "ua", "ip");

        assertThat(response.token()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(auditService).record(eq(user), eq(AuthEventType.TOKEN_REFRESHED), isNull(), eq("ip"));
    }

    @Test
    void logoutAuditsWhenSessionExisted() {
        User user = new User("bob", "bob@example.com", "hash", Role.DOCTOR);
        when(refreshTokenService.revoke("tok")).thenReturn(Optional.of(user));

        authService.logout("tok", "ip");

        verify(auditService).record(eq(user), eq(AuthEventType.LOGGED_OUT), isNull(), eq("ip"));
    }

    @Test
    void logoutIsSilentWhenTokenUnknown() {
        when(refreshTokenService.revoke("tok")).thenReturn(Optional.empty());

        authService.logout("tok", "ip");

        verify(auditService, never()).record(any(), any(), any(), any());
    }

    @Test
    void logoutAllRevokesAndAudits() {
        User user = new User("bob", "bob@example.com", "hash", Role.DOCTOR);
        lenient().when(refreshTokenService.revokeAllForUser(any())).thenReturn(2);

        authService.logoutAll(user, "ip");

        verify(refreshTokenService).revokeAllForUser(user.getId());
        verify(auditService).record(eq(user), eq(AuthEventType.LOGGED_OUT_ALL), isNull(), eq("ip"));
    }
}
