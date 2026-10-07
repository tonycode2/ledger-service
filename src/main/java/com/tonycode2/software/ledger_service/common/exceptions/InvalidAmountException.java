package com.tonycode2.software.ledger_service.common.exceptions;

public class InvalidAmountException extends LedgerException {

    private final long amount;

    public InvalidAmountException(Long amount) {
        super("The amount is not positive, got %d".formatted(amount));
        this.amount = amount;
    }

    public long getAmount() {
        return amount;
    }

}
