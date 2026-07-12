package com.mawa3id.auth;

import com.mawa3id.auth.dto.AuthResponse;
import com.mawa3id.auth.dto.LoginRequest;
import com.mawa3id.auth.dto.MeResponse;
import com.mawa3id.auth.dto.RegisterDoctorRequest;
import com.mawa3id.auth.dto.RegisterPatientRequest;
import com.mawa3id.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/patient")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerPatient(@Valid @RequestBody RegisterPatientRequest request) {
        return authService.registerPatient(request);
    }

    @PostMapping("/register/doctor")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerDoctor(@Valid @RequestBody RegisterDoctorRequest request) {
        return authService.registerDoctor(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        return MeResponse.from(principal.getUser());
    }
}
