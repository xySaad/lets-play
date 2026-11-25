package com.z01.letsplay.resolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.WebUtils;

import com.z01.letsplay.annotation.Auth;
import com.z01.letsplay.exception.AppError;
import com.z01.letsplay.services.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class AuthResolver implements HandlerMethodArgumentResolver {
    @Autowired
    AuthService authService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Auth.User.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null)
            throw AppError.INTERNAL_ERROR.asException();
        Cookie cookie = WebUtils.getCookie(request, "jwt");
        if (cookie == null)
            throw AppError.MISSING_JWT_TOKEN.asException();

        return this.authService.getUserId(cookie.getValue());
    }

}
