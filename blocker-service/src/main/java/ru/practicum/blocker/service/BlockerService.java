package ru.practicum.blocker.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Service;
import ru.practicum.blocker.web.dto.BlockCheckRequest;
import ru.practicum.blocker.web.dto.BlockCheckResponse;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BlockerService {

    private final AtomicInteger counter;
    private final MeterRegistry meterRegistry;

    public BlockerService(MeterRegistry meterRegistry) {
        this(new AtomicInteger(0), meterRegistry);
    }

    // Package-private for tests
    BlockerService(AtomicInteger counter) {
        this(counter, Metrics.globalRegistry);
    }

    BlockerService(AtomicInteger counter, MeterRegistry meterRegistry) {
        this.counter = counter;
        this.meterRegistry = meterRegistry;
    }

    public BlockCheckResponse check(BlockCheckRequest request) {
        int current = counter.incrementAndGet();
        if (current % 3 == 0) {
            recordBlocked(request);
            return BlockCheckResponse.block("Ваш перевод заблокирован");
        }
        return BlockCheckResponse.allow();
    }

    private void recordBlocked(BlockCheckRequest request) {
        Tags tags = Tags.of(
                "from_login", request.fromLogin(),
                "to_login", request.toLogin(),
                "currency", request.currency()
        );
        meterRegistry.counter("blocker_blocked_total", tags).increment();
    }
}
