package com.tonycode2.software.ledger_service.common.exceptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SameAccountTransferExceptionTest {
    void CreateException_WithMessage_ReturnsExceptionWithMessage() {
        SameAccountTransferException exception = new SameAccountTransferException("Account is the same");
        assertTrue(exception.getMessage().equals("Account is the same"));
        assertTrue(exception instanceof LedgerException);
    }
}
