package com.tonycode2.software.ledger_service.common.exceptions;

public class IdempotencyKeyReusedException extends LedgerException {

    public IdempotencyKeyReusedException(String message) {
        super(message);
    }

}
