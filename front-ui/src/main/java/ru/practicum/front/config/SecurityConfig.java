package ru.practicum.front.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ClientRegistrationRepository clientRegistrationRepository,
                                            OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService) throws Exception {
        http.authorizeHttpRequests(registry -> registry
                .requestMatchers("/signup", "/css/**", "/js/**", "/images/**",
                        "/actuator/health", "/actuator/info", "/actuator/refresh").permitAll()
                .anyRequest().authenticated()
        );

        http.oauth2Login(oauth -> oauth.userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService)));
        http.csrf(csrf -> csrf.disable());

        var logoutHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        logoutHandler.setPostLogoutRedirectUri("/login");
        http.logout(logout -> logout.logoutSuccessHandler(logoutHandler));

        return http.build();
    }
}
