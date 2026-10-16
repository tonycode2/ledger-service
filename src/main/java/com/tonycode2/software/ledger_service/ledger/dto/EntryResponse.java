package com.tonycode2.software.ledger_service.ledger.dto;

import java.util.UUID;

import com.tonycode2.software.ledger_service.ledger.model.Entry;

public record EntryResponse(
        String direction,
        long amount,
        UUID transactionId) {

    public static EntryResponse from(Entry entry) {
        return new EntryResponse(entry.getEntryDirection().toString(), entry.getAmount(),
                entry.getTransaction().getId());
    }
}
