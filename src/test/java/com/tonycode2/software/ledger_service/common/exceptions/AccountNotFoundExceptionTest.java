package com.tonycode2.software.ledger_service.common.exceptions;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class AccountNotFoundExceptionTest {
    @Test
    void CreateException_WithId_ReturnsExceptionWithId() {
        AccountNotFoundException exception = new AccountNotFoundException(UUID.randomUUID());
        assertNotNull(exception.getId());
        assertTrue(exception.getId() instanceof UUID);
        assertTrue(exception instanceof LedgerException);
        assertTrue(exception instanceof AccountNotFoundException);
    }

    @Test
    void CreateException_WithMessage_ReturnsExceptionWithMessage() {
        AccountNotFoundException exception = new AccountNotFoundException("This is a test message");
        assertNotNull(exception.getMessage());
        assertNull(exception.getId());
        assertTrue(exception.getMessage().equals("This is a test message"));
    }
}
