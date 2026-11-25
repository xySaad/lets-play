package com.z01.letsplay.infrastructure;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.z01.letsplay.annotation.RequiresPermission;
import com.z01.letsplay.exception.AppError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionProvider validator;

    public PermissionInterceptor(PermissionProvider validator) {
        this.validator = validator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method))
            return false;

        RequiresPermission requiresPermission = method.getMethodAnnotation(RequiresPermission.class);
        if (requiresPermission != null && !validator.hasPermission(requiresPermission.scope()))
            throw AppError.PERMISSION_DENIED.asException();

        return true;
    }
}
