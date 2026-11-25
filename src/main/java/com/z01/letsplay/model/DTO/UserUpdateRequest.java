package com.z01.letsplay.model.DTO;

import com.z01.letsplay.model.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserUpdateRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 64, message = "Name must not exceed 64 characters")
    public String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    public String email;

    // optional: when omitted the current password is kept
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    public String password;

    @NotNull(message = "Role is required")
    @Pattern(regexp = "^(" + Role.DEFAULT + "|" + Role.ROOT + ")$", message = "Role must be root or default")
    public String role;
}
