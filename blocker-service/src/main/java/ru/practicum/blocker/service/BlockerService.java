package ru.practicum.blocker.service;

import org.springframework.stereotype.Service;
import ru.practicum.blocker.web.dto.BlockCheckRequest;
import ru.practicum.blocker.web.dto.BlockCheckResponse;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BlockerService {

    private final AtomicInteger counter;

    public BlockerService() {
        this(new AtomicInteger(0));
    }

    // Package-private for tests
    BlockerService(AtomicInteger counter) {
        this.counter = counter;
    }

    public BlockCheckResponse check(BlockCheckRequest request) {
        int current = counter.incrementAndGet();
        if (current % 3 == 0) {
            return BlockCheckResponse.block("Ваш перевод заблокирован");
        }
        return BlockCheckResponse.allow();
    }
}
