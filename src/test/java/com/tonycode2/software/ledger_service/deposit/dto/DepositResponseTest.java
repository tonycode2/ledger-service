package com.tonycode2.software.ledger_service.deposit.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.tonycode2.software.ledger_service.ledger.model.enums.TransactionType;

public class DepositResponseTest {
    @Test
    void MapDto_WithCorrectInfor_ReturnDto() {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        DepositResponse response = DepositResponse.from(transactionId, TransactionType.DEPOSIT, accountId, 100);
        assertNotNull(response);
        assertEquals(response.accountId(), accountId);
        assertEquals(response.transactionId(), transactionId);
        assertEquals(response.type(), TransactionType.DEPOSIT);
        assertEquals(response.amount(), 100);
    }
}
