package com.mk.contractservice.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeycloakJwtConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";

    @Override
    public Collection<GrantedAuthority> convert(final Jwt jwt) {
        final Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(extractRealmRoles(jwt));
        authorities.addAll(extractResourceRoles(jwt));
        return authorities;
    }

    private Collection<GrantedAuthority> extractRealmRoles(final Jwt jwt) {
        final Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
        if (realmAccess == null || !realmAccess.containsKey(ROLES_CLAIM)) {
            return new ArrayList<>();
        }
        @SuppressWarnings("unchecked") final List<String> roles = (List<String>) realmAccess.get(ROLES_CLAIM);
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    private Collection<GrantedAuthority> extractResourceRoles(final Jwt jwt) {
        final Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess == null) {
            return new ArrayList<>();
        }

        final Collection<GrantedAuthority> authorities = new ArrayList<>();

        for (final Object clientEntry : resourceAccess.values()) {
            @SuppressWarnings("unchecked") final Map<String, Object> client = (Map<String, Object>) clientEntry;
            if (!client.containsKey(ROLES_CLAIM)) {
                continue;
            }

            @SuppressWarnings("unchecked") final List<String> roles = (List<String>) client.get(ROLES_CLAIM);
            roles.forEach(role ->
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            );
        }

        return authorities;
    }
}

