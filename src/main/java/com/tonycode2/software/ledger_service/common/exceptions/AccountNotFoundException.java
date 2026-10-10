package com.tonycode2.software.ledger_service.common.exceptions;

import java.util.UUID;

public class AccountNotFoundException extends LedgerException {
    private UUID id;

    public AccountNotFoundException(UUID id) {
        super("Account %s not found".formatted(id));
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

}
