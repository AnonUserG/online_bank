package ru.practicum.blocker.service;

import org.junit.jupiter.api.Test;
import ru.practicum.blocker.web.dto.BlockCheckRequest;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BlockerServiceTest {

    @Test
    void blocksWhenChanceBelowThreshold() {
        var service = new BlockerService(new AtomicInteger(1)); // 2nd call -> counter 2, 3rd -> block

        var resp = service.check(new BlockCheckRequest("from", "to", "RUB", BigDecimal.TEN));
        var resp2 = service.check(new BlockCheckRequest("from", "to", "RUB", BigDecimal.TEN)); // counter=3 -> block

        assertThat(resp.allowed()).isTrue();
        assertThat(resp2.allowed()).isFalse();
        assertThat(resp2.reason()).isEqualTo("Ваш перевод заблокирован");
    }

    @Test
    void allowsWhenChanceAboveThreshold() {
        var service = new BlockerService(new AtomicInteger(3)); // next is 4 (allowed)

        var resp = service.check(new BlockCheckRequest("from", "to", "USD", BigDecimal.ONE));

        assertThat(resp.allowed()).isTrue();
        assertThat(resp.reason()).isNull();
    }
}
