package com.mawa3id.auth.dto;

import com.mawa3id.user.Role;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        Long userId,
        String email,
        Role role
) {
    public static AuthResponse of(String token, long expiresInMs, Long userId, String email, Role role) {
        return new AuthResponse(token, "Bearer", expiresInMs, userId, email, role);
    }
}
