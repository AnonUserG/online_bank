package ru.practicum.transfer.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.transfer.model.TransferEntity;
import ru.practicum.transfer.model.TransferStatus;
import ru.practicum.transfer.service.dto.TransferPlan;

class TransferMapperTest {

    private TransferMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TransferMapperImpl();
    }

    @Test
    void toEntityPopulatesDefaults() {
        TransferPlan plan = new TransferPlan(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("123.45"), "RUB", "key");

        TransferEntity entity = mapper.toEntity(plan);

        assertThat(entity.getFromAccountId()).isEqualTo(plan.fromAccountId());
        assertThat(entity.getToAccountId()).isEqualTo(plan.toAccountId());
        assertThat(entity.getAmount()).isEqualByComparingTo(plan.amount());
        assertThat(entity.getCurrency()).isEqualTo(plan.currency());
        assertThat(entity.getIdempotencyKey()).isEqualTo(plan.idempotencyKey());
        assertThat(entity.getStatus()).isEqualTo(TransferStatus.PENDING);
        assertThat(entity.getId()).isNull();
    }
}
