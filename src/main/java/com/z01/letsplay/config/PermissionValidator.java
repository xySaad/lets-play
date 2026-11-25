package com.z01.letsplay.config;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.z01.letsplay.infrastructure.PermissionProvider;
import com.z01.letsplay.infrastructure.PrincipalProvider;
import com.z01.letsplay.model.Role;
import com.z01.letsplay.model.User;

@Component
public class PermissionValidator implements PermissionProvider {
    private static final Map<String, Set<String>> ROLE_SCOPES = Map.of(
            Role.ROOT,
            Set.of("v1:users:list", "v1:users:read", "v1:users:create", "v1:users:update", "v1:users:delete"),
            Role.DEFAULT, Set.of());

    private final PrincipalProvider<User> principalProvider;

    public PermissionValidator(PrincipalProvider<User> principalProvider) {
        this.principalProvider = principalProvider;
    }

    @Override
    public boolean hasPermission(String scope) {
        User user = principalProvider.getCurrentPrincipal();
        return ROLE_SCOPES.getOrDefault(user.role, Set.of()).contains(scope);
    }
}
