package com.mk.contractservice.acceptance.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestTokens {

    public static final String ISSUER = "https://keycloak.test/realms/contract-service";
    public static final String AUDIENCE = "contract-service";
    public static final String COUNSELOR_ROLE = "counselor";

    private static final KeyPair KEY_PAIR = generateKeyPair();
    private static final NimbusJwtEncoder ENCODER = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(
            new RSAKey.Builder(publicKey()).privateKey((RSAPrivateKey) KEY_PAIR.getPrivate()).keyID("test-key").build())));

    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEY_PAIR.getPublic();
    }

    public static String counselor() {
        return builder().realmRoles(COUNSELOR_ROLE).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private static KeyPair generateKeyPair() {
        try {
            final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static final class Builder {

        private String issuer = ISSUER;
        private String audience = AUDIENCE;
        private Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
        private List<String> realmRoles = List.of();
        private Map<String, Object> resourceAccess = Map.of();

        public Builder issuer(final String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder audience(final String audience) {
            this.audience = audience;
            return this;
        }

        public Builder expiresAt(final Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder realmRoles(final String... roles) {
            this.realmRoles = List.of(roles);
            return this;
        }

        public Builder clientRoles(final String clientId, final String... roles) {
            this.resourceAccess = Map.of(clientId, Map.of("roles", List.of(roles)));
            return this;
        }

        public String build() {
            final JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(issuer)
                    .audience(List.of(audience))
                    .subject(UUID.randomUUID().toString())
                    .issuedAt(expiresAt.minus(Duration.ofMinutes(10)))
                    .expiresAt(expiresAt)
                    .claim("realm_access", Map.of("roles", realmRoles))
                    .claim("resource_access", resourceAccess)
                    .build();
            final JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId("test-key").build();
            return ENCODER.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        }
    }
}
