package com.mk.contractservice.acceptance.config;

import com.mk.contractservice.acceptance.helper.TestTokens;
import com.mk.contractservice.infrastructure.security.AccessTokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@TestConfiguration(proxyBeanMethods = false)
public class TestJwtConfiguration {

    @Bean
    @Primary
    JwtDecoder testJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") final String issuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.audiences}") final String audience
    ) {
        final NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(TestTokens.publicKey()).build();
        decoder.setJwtValidator(AccessTokenValidator.of(issuer, audience));
        return decoder;
    }
}
