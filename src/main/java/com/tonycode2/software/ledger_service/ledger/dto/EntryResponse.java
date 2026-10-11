package com.tonycode2.software.ledger_service.ledger.dto;

import java.time.Instant;

import com.tonycode2.software.ledger_service.account.dto.AccountResponse;
import com.tonycode2.software.ledger_service.ledger.model.Entry;
import com.tonycode2.software.ledger_service.ledger.model.enums.EntryDirection;

public record EntryResponse(
        long id,
        LedgerTransactionResponse transaction,
        AccountResponse account,
        EntryDirection direction,
        long amount,
        Instant createdAt) {
    public static EntryResponse from(Entry entry) {
        return new EntryResponse(entry.getId(), LedgerTransactionResponse.from(entry.getTransaction()),
                AccountResponse.from(entry.getAccount()), entry.getEntryDirection(), entry.getAmount(),
                entry.getCreatedAt());
    }
}
