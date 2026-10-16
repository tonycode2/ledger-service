package com.tonycode2.software.ledger_service.common.exceptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class InvalidTransferTypeExceptionTest {
    @Test
    void CreateException_WithMessage_ReturnsExceptionWithMessage() {
        InvalidTransferTypeException exception = new InvalidTransferTypeException("The transfer type is not valid");
        assertTrue(exception.getMessage().equals("The transfer type is not valid"));
        assertTrue(exception instanceof LedgerException);
    }
}
