package com.mawa3id.auth.dto;

import com.mawa3id.user.Role;

/**
 * Issued on register / login / refresh. {@code token} is the short-lived access
 * (bearer) JWT; {@code refreshToken} is the opaque, rotating credential used to obtain
 * a new access token without re-authenticating.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        String refreshToken,
        Long userId,
        String username,
        String email,
        Role role
) {
    public static AuthResponse of(String token, long expiresInMs, String refreshToken,
                                  Long userId, String username, String email, Role role) {
        return new AuthResponse(token, "Bearer", expiresInMs, refreshToken, userId, username, email, role);
    }
}
