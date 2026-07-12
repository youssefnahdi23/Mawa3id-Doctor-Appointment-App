package com.mawa3id.auth;

import com.mawa3id.auth.dto.AuthResponse;
import com.mawa3id.auth.dto.LoginRequest;
import com.mawa3id.auth.dto.RegisterDoctorRequest;
import com.mawa3id.auth.dto.RegisterPatientRequest;
import com.mawa3id.common.ApiException;
import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.doctor.DoctorRepository;
import com.mawa3id.doctor.DoctorService;
import com.mawa3id.patient.PatientRepository;
import com.mawa3id.patient.PatientService;
import com.mawa3id.security.JwtService;
import com.mawa3id.user.Role;
import com.mawa3id.user.User;
import com.mawa3id.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService} in isolation. These target branches the
 * MockMvc integration tests cannot easily reach: the duplicate-email conflict,
 * email normalization before persistence, the login fallback-401 when the
 * authenticated user is missing, and delegation of specialty resolution.
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
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    @Captor private ArgumentCaptor<User> userCaptor;

    private RegisterPatientRequest patientRequest(String email) {
        return new RegisterPatientRequest(email, "password123", "Alice Doe",
                LocalDate.of(1990, 5, 20));
    }

    @Test
    void registerPatientWithExistingEmailThrowsConflict() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerPatient(patientRequest("dup@example.com")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void registerPatientNormalizesEmailBeforePersistingAndReturnsToken() {
        User saved = mock(User.class);
        when(saved.getId()).thenReturn(1L);
        when(saved.getEmail()).thenReturn("alice@example.com");
        when(saved.getRole()).thenReturn(Role.PATIENT);

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(patientService.generateUniquePatientCode()).thenReturn("MW-ABC234");
        when(jwtService.generateToken(anyString(), any())).thenReturn("token-xyz");
        when(jwtService.getExpirationMs()).thenReturn(3_600_000L);

        // Mixed case + surrounding whitespace must be trimmed and lowercased.
        AuthResponse response = authService.registerPatient(patientRequest("  Alice@Example.COM  "));

        assertThat(response.token()).isEqualTo("token-xyz");
        assertThat(response.role()).isEqualTo(Role.PATIENT);
        verify(userRepository).existsByEmail("alice@example.com");

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void loginThrowsUnauthorizedWhenUserMissingAfterAuthentication() {
        // authenticate() succeeds (returns a stub) but the user row is gone.
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "password123")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void loginReturnsTokenForValidUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn("bob@example.com");
        when(user.getRole()).thenReturn(Role.DOCTOR);
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(anyString(), any())).thenReturn("tok");
        when(jwtService.getExpirationMs()).thenReturn(3_600_000L);

        AuthResponse response = authService.login(new LoginRequest("bob@example.com", "password123"));

        assertThat(response.token()).isEqualTo("tok");
        assertThat(response.role()).isEqualTo(Role.DOCTOR);
    }

    @Test
    void registerDoctorDelegatesSpecialtyResolutionAndPropagatesNotFound() {
        when(doctorService.resolveSpecialty(999L))
                .thenThrow(new ResourceNotFoundException("Specialty not found: 999"));

        RegisterDoctorRequest request = new RegisterDoctorRequest(
                "doc@example.com", "password123", "Dr Ghost", 999L, "Clinic", "9-17", "+212");

        assertThatThrownBy(() -> authService.registerDoctor(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(doctorService).resolveSpecialty(999L);
    }
}
