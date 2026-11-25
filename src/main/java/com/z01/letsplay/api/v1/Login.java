package com.z01.letsplay.api.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.z01.letsplay.model.User;
import com.z01.letsplay.model.DTO.LoginRequest;
import com.z01.letsplay.model.DTO.UserResponse;
import com.z01.letsplay.services.AuthService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
public class Login {
    @Autowired
    private AuthService authService;

    @PostMapping("/api/v1/login")
    public UserResponse login(@RequestBody @Valid LoginRequest body, HttpServletResponse response) {
        User user = authService.login(body.email, body.password);

        response.addHeader("Set-Cookie", authService.startSession(user).toString());
        return UserResponse.of(user);
    }
}
