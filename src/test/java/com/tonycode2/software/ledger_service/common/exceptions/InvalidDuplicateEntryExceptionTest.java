package com.tonycode2.software.ledger_service.common.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class InvalidDuplicateEntryExceptionTest {
    @Test
    void CreateException_WithMessage_ReturnsExceptionWithMessage() {
        InvalidDuplicateEntryException exception = new InvalidDuplicateEntryException("Duplicate Entry Exception");
        assertNotNull(exception);
        assertTrue(exception instanceof LedgerException);
        assertEquals(exception.getMessage(), "Duplicate Entry Exception");
    }
}
