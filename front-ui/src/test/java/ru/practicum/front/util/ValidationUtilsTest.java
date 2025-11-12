package ru.practicum.front.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ValidationUtilsTest {

    @Test
    void validateSignupDetectsBlankAndUnderage() {
        var errors = ValidationUtils.validateSignup(" ", "pwd", "pw", "", LocalDate.now());
        assertThat(errors).hasSize(3);
    }

    @Test
    void validatePasswordChangeChecksLengthAndMatch() {
        var errors = ValidationUtils.validatePasswordChange("123", "321");
        assertThat(errors).hasSize(2);
    }

    @Test
    void validateTransferRejectsNegativeAndNonNumeric() {
        var errors = ValidationUtils.validateTransfer(" ", "not-number");
        assertThat(errors).hasSize(2);

        var errors2 = ValidationUtils.validateTransfer("bob", "-10");
        assertThat(errors2).hasSize(1);
    }
}
