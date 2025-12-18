package ru.practicum.front.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

/**
 * Tracks successful and failed logins (by login).
 */
@Component
@RequiredArgsConstructor
public class LoginMetricsListener implements ApplicationListener<AbstractAuthenticationEvent> {

    private final MeterRegistry meterRegistry;

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        var authentication = event.getAuthentication();
        String login = authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "unknown";
        String outcome = (event instanceof AbstractAuthenticationFailureEvent) ? "failure" : "success";
        meterRegistry.counter("auth_login_total", Tags.of("login", login, "outcome", outcome)).increment();
    }
}
