package com.z01.letsplay.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import com.z01.letsplay.exception.AppError;
import com.z01.letsplay.infrastructure.PrincipalProvider;
import com.z01.letsplay.model.User;
import com.z01.letsplay.services.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

// The principal is the whole User (not just its id): ownership rules need the role,
@Component
public class JwtPrincipalProvider implements PrincipalProvider<User> {
    @Autowired
    HttpServletRequest request;
    @Autowired
    AuthService authService;

    @Override
    public User getCurrentPrincipal() {
        Cookie cookie = WebUtils.getCookie(request, "jwt");
        if (cookie == null)
            throw AppError.MISSING_JWT_TOKEN.asException();

        return authService.getUser(cookie.getValue());
    }
}
