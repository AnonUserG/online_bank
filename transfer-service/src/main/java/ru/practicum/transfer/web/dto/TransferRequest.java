package ru.practicum.transfer.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @JsonProperty("from_login")
        @JsonAlias("fromLogin")
        @NotBlank(message = "from_login is required")
        String fromLogin,
        @JsonProperty("to_login")
        @JsonAlias("toLogin")
        @NotBlank(message = "to_login is required")
        String toLogin,
        @NotNull(message = "value is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "value must be positive")
        BigDecimal value
) {
}
