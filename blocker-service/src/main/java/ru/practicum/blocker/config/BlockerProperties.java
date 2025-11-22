package ru.practicum.blocker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.blocker")
public class BlockerProperties {
    private double blockProbability = 0.3;
}
