package com.tonycode2.software.ledger_service.ledger.dto;

import java.time.Instant;
import java.util.UUID;

import com.tonycode2.software.ledger_service.ledger.model.LedgerTransaction;
import com.tonycode2.software.ledger_service.ledger.model.enums.TransactionType;

public record LedgerTransactionResponse(
        UUID id,
        TransactionType type,
        UUID reversesId,
        Instant createdAt) {
    public static LedgerTransactionResponse from(LedgerTransaction transaction) {
        return new LedgerTransactionResponse(transaction.getId(), transaction.getType(), transaction.getReversesId(),
                transaction.getCreatedAt());
    }
}
