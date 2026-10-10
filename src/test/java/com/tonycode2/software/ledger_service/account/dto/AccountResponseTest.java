package com.tonycode2.software.ledger_service.account.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.tonycode2.software.ledger_service.account.model.Account;
import com.tonycode2.software.ledger_service.account.model.enums.AccountType;

public class AccountResponseTest {
    @Test
    void From_MappingBaseModelToDto_ReturnsCorrectDto() {
        Account account = new Account("Ana", "USD", AccountType.CUSTOMER, false);
        AccountResponse response = AccountResponse.from(account);

        assertNull(response.id());
        assertNotNull(response.owner());
        assertNotNull(response.currency());
        assertEquals(response.owner(), "Ana");
        assertEquals(response.currency(), "USD");
    }
}
