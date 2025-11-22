package ru.practicum.exchangegen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.practicum.exchangegen.config.ExchangeGeneratorProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ExchangeGeneratorProperties.class)
public class ExchangeGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExchangeGeneratorApplication.class, args);
    }
}
