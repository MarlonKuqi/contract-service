package com.mk.contractservice.infrastructure.security;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ApplicationRoles {
    public static final String HAS_ADMIN = "hasAuthority('ROLE_ADMIN')";
    public static final String HAS_COUNSELOR = "hasAuthority('ROLE_COUNSELOR')";
    public static final String HAS_ADMIN_OR_COUNSELOR = "hasAnyAuthority('ROLE_ADMIN', 'ROLE_COUNSELOR')";
}

