package com.tonycode2.software.ledger_service.common.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class InsufficientFundsExceptionTest {
    @Test
    void CreateException_WithCorrectInformation_ReturnsCorrectException() {
        InsufficientFundsException exception = new InsufficientFundsException(UUID.randomUUID(), 100L, 50L);
        assertNotNull(exception.getAccountId());
        assertNotNull(exception.getBalance());
        assertNotNull(exception.getRequested());
        assertNotNull(exception.getMessage());
        assertTrue(exception.getAccountId() instanceof UUID);
        assertEquals(exception.getBalance(), 100L);
        assertEquals(exception.getRequested(), 50L);
    }
}
