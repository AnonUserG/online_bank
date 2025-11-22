package ru.practicum.transfer.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.transfer.model.TransferEntity;
import ru.practicum.transfer.model.TransferStatus;
import ru.practicum.transfer.service.dto.TransferPlan;

/**
 * Simple manual implementation to keep mapping deterministic for tests.
 */
@Component
public class TransferMapperImpl implements TransferMapper {

    @Override
    public TransferEntity toEntity(TransferPlan plan) {
        if (plan == null) {
            return null;
        }
        var entity = new TransferEntity();
        entity.setFromAccountId(plan.fromAccountId());
        entity.setToAccountId(plan.toAccountId());
        entity.setAmount(plan.amount());
        entity.setCurrency(plan.currency());
        entity.setIdempotencyKey(plan.idempotencyKey());
        entity.setStatus(TransferStatus.PENDING);
        return entity;
    }
}
