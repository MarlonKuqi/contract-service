package com.mk.contractservice.infrastructure.security;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ApplicationRoles {

    public static final String HAS_ADMIN = "hasRole('ROLE_ADMIN')";
    public static final String HAS_COUNSELOR = "hasRole('ROLE_COUNSELOR')";
    public static final String HAS_ADMIN_OR_COUNSELOR = "hasAnyRole('ROLE_ADMIN', 'ROLE_COUNSELOR')";
}

