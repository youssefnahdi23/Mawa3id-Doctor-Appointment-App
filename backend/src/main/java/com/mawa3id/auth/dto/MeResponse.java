package com.mawa3id.auth.dto;

import com.mawa3id.user.Role;
import com.mawa3id.user.User;

public record MeResponse(Long userId, String username, String email, Role role, boolean active,
                         boolean emailVerified, boolean phoneVerified) {

    public static MeResponse of(User user, boolean emailVerified, boolean phoneVerified) {
        return new MeResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.isActive(), emailVerified, phoneVerified);
    }
}
