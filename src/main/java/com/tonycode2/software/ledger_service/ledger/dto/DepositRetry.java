package com.tonycode2.software.ledger_service.ledger.dto;

import java.util.UUID;

import com.tonycode2.software.ledger_service.ledger.model.enums.TransactionType;

public record DepositRetry(
        UUID transactionId,
        TransactionType type,
        UUID accountId,
        long amount) {
}
