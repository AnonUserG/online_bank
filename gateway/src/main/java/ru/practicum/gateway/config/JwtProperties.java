package ru.practicum.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки JWT для ресурс-сервера.
 */
@Component
@ConfigurationProperties(prefix = "spring.security.oauth2.resourceserver.jwt")
@Data
public class JwtProperties {
    private String jwkSetUri;
    private String expectedIssuer;
}
