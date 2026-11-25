package com.z01.letsplay.infrastructure;

public interface PermissionProvider {
    boolean hasPermission(String scope);
}
