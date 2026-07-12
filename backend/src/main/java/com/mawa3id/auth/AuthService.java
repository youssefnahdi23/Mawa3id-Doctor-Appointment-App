package com.mawa3id.auth;

import com.mawa3id.auth.dto.AuthResponse;
import com.mawa3id.auth.dto.LoginRequest;
import com.mawa3id.auth.dto.RegisterDoctorRequest;
import com.mawa3id.auth.dto.RegisterPatientRequest;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PatientRepository patientRepository,
                       DoctorRepository doctorRepository, PatientService patientService,
                       DoctorService doctorService, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse registerPatient(RegisterPatientRequest request) {
        User user = createUser(request.email(), request.password(), Role.PATIENT);
        Patient patient = new Patient(user, request.fullName(), request.dateOfBirth(),
                patientService.generateUniquePatientCode());
        patientRepository.save(patient);
        return tokenFor(user);
    }

    @Transactional
    public AuthResponse registerDoctor(RegisterDoctorRequest request) {
        Specialty specialty = doctorService.resolveSpecialty(request.specialtyId());
        User user = createUser(request.email(), request.password(), Role.DOCTOR);
        Doctor doctor = new Doctor(user, request.name(), specialty, request.cabinetAddress(),
                request.workingHours(), request.phone());
        doctorRepository.save(doctor);
        return tokenFor(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException (handled globally as 401) on failure.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return tokenFor(user);
    }

    private User createUser(String email, String rawPassword, Role role) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User(normalizedEmail, passwordEncoder.encode(rawPassword), role);
        return userRepository.save(user);
    }

    private AuthResponse tokenFor(User user) {
        String token = jwtService.generateToken(user.getEmail(),
                Map.of("uid", user.getId(), "role", user.getRole().name()));
        return AuthResponse.of(token, jwtService.getExpirationMs(), user.getId(), user.getEmail(), user.getRole());
    }
}
