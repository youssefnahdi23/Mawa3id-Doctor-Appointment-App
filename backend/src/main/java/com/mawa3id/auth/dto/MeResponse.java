package com.mawa3id.auth.dto;

import com.mawa3id.user.Role;
import com.mawa3id.user.User;

public record MeResponse(Long userId, String username, String email, Role role, boolean active) {

    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.isActive());
    }
}
