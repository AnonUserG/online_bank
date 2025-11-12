package ru.practicum.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.test.StepVerifier;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void jwtAuthenticationConverterExtractsRolesWithPrefix() {
        var converter = config.jwtAuthenticationConverter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user")
                .issuedAt(Instant.now())
                .claim("roles", List.of("ADMIN", "USER"))
                .build();

        StepVerifier.create(converter.convert(jwt))
                .assertNext(auth -> assertThat(auth.getAuthorities())
                        .extracting("authority")
                        .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER"))
                .verifyComplete();
    }
}
