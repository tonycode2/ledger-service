package com.tonycode2.software.ledger_service.common.exceptions;

public abstract class LedgerException extends RuntimeException {

    protected LedgerException(String message) {
        super(message);
    }
}
