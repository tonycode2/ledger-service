package com.tonycode2.software.ledger_service.common.exceptions;

import java.util.UUID;

public class InsufficientFundsException extends LedgerException {

    private final UUID accountId;
    private final long balance;
    private final long requested;

    public InsufficientFundsException(UUID accountId, long balance, long requested) {
        super("Account %s has %d, requested %d".formatted(accountId, balance, requested));
        this.accountId = accountId;
        this.balance = balance;
        this.requested = requested;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public long getBalance() {
        return balance;
    }

    public long getRequested() {
        return requested;
    }

}
