package com.tonycode2.software.ledger_service.common.exceptions;

public class SameAccountTransferException extends LedgerException {
    public SameAccountTransferException(String message) {
        super(message);
    }
}
