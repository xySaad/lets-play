package com.z01.letsplay.model.DTO;

import com.z01.letsplay.model.User;

// deliberately has no password field: sensitive data must never reach a response
public record UserResponse(String id, String name, String email, String role) {
    public static UserResponse of(User user) {
        return new UserResponse(user.id, user.name, user.email, user.role);
    }
}
