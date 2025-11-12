package ru.practicum.accounts.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Проверяет, что дата рождения указывает на возраст не младше заданного количества лет.
 */
public class AdultValidator implements ConstraintValidator<Adult, LocalDate> {

    private final Clock clock;
    private int minimumYears;

    public AdultValidator() {
        this(Clock.systemDefaultZone());
    }

    AdultValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void initialize(Adult constraintAnnotation) {
        this.minimumYears = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDate threshold = LocalDate.now(clock).minusYears(minimumYears);
        return !value.isAfter(threshold);
    }
}
