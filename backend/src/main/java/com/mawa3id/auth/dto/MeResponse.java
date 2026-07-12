package com.mawa3id.auth.dto;

import com.mawa3id.user.Role;
import com.mawa3id.user.User;

public record MeResponse(Long userId, String email, Role role) {

    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
