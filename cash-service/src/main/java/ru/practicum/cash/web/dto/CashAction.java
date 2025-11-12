package ru.practicum.cash.web.dto;

import ru.practicum.cash.model.OperationType;

public enum CashAction {
    PUT(OperationType.DEPOSIT),
    GET(OperationType.WITHDRAW);

    private final OperationType operationType;

    CashAction(OperationType operationType) {
        this.operationType = operationType;
    }

    public OperationType toOperationType() {
        return operationType;
    }
}
