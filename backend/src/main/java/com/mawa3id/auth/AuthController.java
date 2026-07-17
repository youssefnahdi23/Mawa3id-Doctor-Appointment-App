package com.mawa3id.auth;

import com.mawa3id.auth.dto.AuthResponse;
import com.mawa3id.auth.dto.ForgotPasswordRequest;
import com.mawa3id.auth.dto.LoginRequest;
import com.mawa3id.auth.dto.LogoutRequest;
import com.mawa3id.auth.dto.MeResponse;
import com.mawa3id.auth.dto.MessageResponse;
import com.mawa3id.auth.dto.PhoneRequest;
import com.mawa3id.auth.dto.RefreshRequest;
import com.mawa3id.auth.dto.RegisterDoctorRequest;
import com.mawa3id.auth.dto.RegisterPatientRequest;
import com.mawa3id.auth.dto.ResetPasswordRequest;
import com.mawa3id.auth.dto.VerifyEmailConfirmRequest;
import com.mawa3id.auth.dto.VerifyPhoneConfirmRequest;
import com.mawa3id.auth.contact.ContactService;
import com.mawa3id.security.AppUserDetails;
import com.mawa3id.user.User;
import jakarta.servlet.http.HttpServletRequest;
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
    private final VerificationService verificationService;
    private final PasswordResetService passwordResetService;
    private final ContactService contactService;

    public AuthController(AuthService authService, VerificationService verificationService,
                          PasswordResetService passwordResetService, ContactService contactService) {
        this.authService = authService;
        this.verificationService = verificationService;
        this.passwordResetService = passwordResetService;
        this.contactService = contactService;
    }

    @PostMapping("/register/patient")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerPatient(@Valid @RequestBody RegisterPatientRequest request,
                                        HttpServletRequest http) {
        return authService.registerPatient(request, userAgent(http), ip(http));
    }

    @PostMapping("/register/doctor")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerDoctor(@Valid @RequestBody RegisterDoctorRequest request,
                                       HttpServletRequest http) {
        return authService.registerDoctor(request, userAgent(http), ip(http));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, userAgent(http), ip(http));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        return authService.refresh(request.refreshToken(), userAgent(http), ip(http));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest http) {
        authService.logout(request.refreshToken(), ip(http));
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal AppUserDetails principal, HttpServletRequest http) {
        authService.logoutAll(principal.getUser(), ip(http));
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        User user = principal.getUser();
        return MeResponse.of(user, contactService.hasVerifiedEmail(user),
                contactService.hasVerifiedPhone(user));
    }

    @PostMapping("/verify/email/request")
    public MessageResponse requestEmailVerification(@AuthenticationPrincipal AppUserDetails principal,
                                                    HttpServletRequest http) {
        verificationService.requestEmailVerification(principal.getUser(), ip(http));
        return new MessageResponse("Verification code sent");
    }

    @PostMapping("/verify/email/confirm")
    public MessageResponse confirmEmail(@AuthenticationPrincipal AppUserDetails principal,
                                        @Valid @RequestBody VerifyEmailConfirmRequest request,
                                        HttpServletRequest http) {
        verificationService.confirmEmail(principal.getUser(), request.code(), ip(http));
        return new MessageResponse("Email verified");
    }

    @PostMapping("/verify/phone/request")
    public MessageResponse requestPhoneVerification(@AuthenticationPrincipal AppUserDetails principal,
                                                    @Valid @RequestBody PhoneRequest request,
                                                    HttpServletRequest http) {
        verificationService.requestPhoneVerification(principal.getUser(), request.phone(), ip(http));
        return new MessageResponse("Verification code sent");
    }

    @PostMapping("/verify/phone/confirm")
    public MessageResponse confirmPhone(@AuthenticationPrincipal AppUserDetails principal,
                                        @Valid @RequestBody VerifyPhoneConfirmRequest request,
                                        HttpServletRequest http) {
        verificationService.confirmPhone(principal.getUser(), request.phone(), request.code(), ip(http));
        return new MessageResponse("Phone verified");
    }

    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                          HttpServletRequest http) {
        passwordResetService.forgot(request.identifier(), ip(http));
        // Deliberately uniform regardless of whether the identifier maps to an account.
        return new MessageResponse("If an account exists, a reset code has been sent");
    }

    @PostMapping("/password/reset")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                         HttpServletRequest http) {
        passwordResetService.reset(request.token(), request.newPassword(), ip(http));
        return new MessageResponse("Password updated");
    }

    private static String ip(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private static String userAgent(HttpServletRequest request) {
        String value = request.getHeader("User-Agent");
        if (value == null) {
            return null;
        }
        return value.length() > 255 ? value.substring(0, 255) : value;
    }
}
