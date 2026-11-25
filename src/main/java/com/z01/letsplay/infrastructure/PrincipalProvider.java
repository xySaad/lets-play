package com.z01.letsplay.infrastructure;

public interface PrincipalProvider<U> {
    U getCurrentPrincipal();
}
