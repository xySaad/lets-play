package com.z01.letsplay.api.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.z01.letsplay.model.Role;
import com.z01.letsplay.model.User;
import com.z01.letsplay.model.DTO.RegisterRequest;
import com.z01.letsplay.model.DTO.UserResponse;
import com.z01.letsplay.services.AuthService;
import com.z01.letsplay.services.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
public class Register {
    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;

    @PostMapping("/api/v1/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@RequestBody @Valid RegisterRequest body, HttpServletResponse response) {
        User user = userService.create(body.name, body.email, body.password, Role.DEFAULT);

        response.addHeader("Set-Cookie", authService.startSession(user).toString());
        return UserResponse.of(user);
    }
}
