package com.mawa3id.user;

import com.mawa3id.security.AppUserDetails;
import com.mawa3id.user.dto.LanguageRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserAccountController {

    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    /** Records the caller's preferred language so their notifications are localized. */
    @PutMapping("/me/language")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setLanguage(@AuthenticationPrincipal AppUserDetails principal,
                            @Valid @RequestBody LanguageRequest request) {
        userAccountService.setPreferredLanguage(principal.getUserId(), request.language());
    }
}
