package com.tonycode2.software.ledger_service.account.dto;

import java.time.Instant;
import java.util.UUID;

import com.tonycode2.software.ledger_service.account.model.Account;

public record AccountResponse(
        UUID id,
        String owner,
        String currency,
        long balance,
        Instant createdAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getOwner(), account.getCurrency(), account.getBalance(),
                account.getCreatedAt());
    }
}
