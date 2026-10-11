package com.tonycode2.software.ledger_service.ledger.dto;

import java.util.UUID;

import com.tonycode2.software.ledger_service.ledger.model.enums.TransactionType;

public record DepositResponse(
        UUID transactionId,
        TransactionType type,
        UUID accountId,
        long amount,
        long newBalance) {
    public DepositResponse from(UUID transactionId, TransactionType type, UUID accountId, long amount,
            long newBalance) {
        return new DepositResponse(transactionId, type, accountId, amount, newBalance);
    }
}
