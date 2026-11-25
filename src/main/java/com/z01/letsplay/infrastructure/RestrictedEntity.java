package com.z01.letsplay.infrastructure;

import com.z01.letsplay.annotation.EntityAccess;

public interface RestrictedEntity<U> {
    void ensureAccess(U user, EntityAccess.Mode mode);

}
