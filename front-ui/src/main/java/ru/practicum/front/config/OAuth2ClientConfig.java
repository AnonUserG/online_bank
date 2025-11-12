package ru.practicum.front.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Configuration
public class OAuth2ClientConfig {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ClientConfig.class);
    private static final String KEYCLOAK_REGISTRATION_ID = "keycloak";

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(Environment environment) {
        ClientRegistration keycloak = buildKeycloakRegistration(environment);
        return new InMemoryClientRegistrationRepository(keycloak);
    }

    private ClientRegistration buildKeycloakRegistration(Environment environment) {
        String clientId = getRequiredProperty(environment, "spring.security.oauth2.client.registration.keycloak.client-id");
        String redirectUri = getRequiredProperty(environment, "spring.security.oauth2.client.registration.keycloak.redirect-uri");
        String[] scopes = splitScopes(environment.getProperty("spring.security.oauth2.client.registration.keycloak.scope"));
        String authorizationUri = getRequiredProperty(environment, "spring.security.oauth2.client.provider.keycloak.authorization-uri");
        String tokenUri = getRequiredProperty(environment, "spring.security.oauth2.client.provider.keycloak.token-uri");
        String userInfoUri = environment.getProperty("spring.security.oauth2.client.provider.keycloak.user-info-uri");
        String jwkSetUri = getRequiredProperty(environment, "spring.security.oauth2.client.provider.keycloak.jwk-set-uri");
        String issuerUri = getRequiredProperty(environment, "spring.security.oauth2.client.provider.keycloak.issuer-uri");
        String userNameAttribute = environment.getProperty(
                "spring.security.oauth2.client.provider.keycloak.user-name-attribute",
                "preferred_username"
        );

        Map<String, Object> metadata = new HashMap<>();
        String endSessionUri = environment.getProperty("spring.security.oauth2.client.provider.keycloak.end-session-uri");
        if (StringUtils.hasText(endSessionUri)) {
            metadata.put("end_session_endpoint", endSessionUri);
        }

        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(KEYCLOAK_REGISTRATION_ID)
                .clientId(clientId)
                .clientName(environment.getProperty(
                        "spring.security.oauth2.client.registration.keycloak.client-name",
                        "Keycloak"))
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope(scopes)
                .authorizationUri(authorizationUri)
                .tokenUri(tokenUri)
                .userNameAttributeName(userNameAttribute)
                .jwkSetUri(jwkSetUri)
                .issuerUri(issuerUri)
                .providerConfigurationMetadata(metadata);

        if (StringUtils.hasText(userInfoUri)) {
            builder.userInfoUri(userInfoUri);
        }

        String clientSecret = environment.getProperty("spring.security.oauth2.client.registration.keycloak.client-secret");
        if (StringUtils.hasText(clientSecret)) {
            builder.clientSecret(clientSecret);
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        } else {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        }

        return builder.build();
    }

    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();
        return userRequest -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            Map<String, Object> realmAccess = extractRealmAccess(oidcUser);
            List<String> roles = extractRoles(realmAccess);

            Set<GrantedAuthority> mappedAuthorities = new LinkedHashSet<>(oidcUser.getAuthorities());
            roles.stream()
                    .filter(StringUtils::hasText)
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase(Locale.ROOT))
                    .map(SimpleGrantedAuthority::new)
                    .forEach(mappedAuthorities::add);

            String nameAttribute = userRequest.getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUserNameAttributeName();
            if (!StringUtils.hasText(nameAttribute)) {
                nameAttribute = "sub";
            }

            log.debug("Loaded OIDC user: {}, roles: {}", oidcUser.getName(), roles);

            if (oidcUser.getUserInfo() != null) {
                return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), nameAttribute);
            }

            return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), nameAttribute);
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractRealmAccess(OidcUser oidcUser) {
        return oidcUser.getClaims().entrySet().stream()
                .filter(entry -> "realm_access".equals(entry.getKey()))
                .filter(entry -> entry.getValue() instanceof Map)
                .map(entry -> (Map<String, Object>) entry.getValue())
                .findFirst()
                .orElseGet(Map::of);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Map<String, Object> realmAccess) {
        Object roles = realmAccess.get("roles");
        if (roles instanceof List<?> roleList) {
            return (List<String>) roleList;
        }
        return List.of();
    }

    private String[] splitScopes(String scopes) {
        if (!StringUtils.hasText(scopes)) {
            return new String[0];
        }
        return Arrays.stream(scopes.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }

    private String getRequiredProperty(Environment environment, String key) {
        String value = environment.getProperty(key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing required OAuth2 property: " + key);
        }
        return value;
    }
}
