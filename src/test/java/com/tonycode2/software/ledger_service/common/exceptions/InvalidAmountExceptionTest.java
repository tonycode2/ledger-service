package com.tonycode2.software.ledger_service.common.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class InvalidAmountExceptionTest {
    @Test
    void CreateException_WithCorrectInformation_ReturnsCorrectException() {
        InvalidAmountException exception = new InvalidAmountException(100L);
        assertNotNull(exception.getAmount());
        assertNotNull(exception.getMessage());
        assertEquals(exception.getAmount(), 100L);
    }
}
