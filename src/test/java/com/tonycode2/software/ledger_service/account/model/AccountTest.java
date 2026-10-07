package com.tonycode2.software.ledger_service.account.model;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.tonycode2.software.ledger_service.account.model.enums.AccountType;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidAccountException;

public class AccountTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "US", "USDD", "U$D", "    " })
    void CreateAccount_WithIncorrectCurrency_ThrowsInvalidAccountException(String currency) {
        assertThatThrownBy(() -> new Account("Ana", currency, AccountType.CUSTOMER, false))
                .isInstanceOf(InvalidAccountException.class);
    }

    @Test
    void CreateAccount_WithNullOwner_ThrowsInvalidAccountException() {
        assertThatThrownBy(() -> new Account(null, "USD", AccountType.CUSTOMER, false))
                .isInstanceOf(InvalidAccountException.class);
    }

    @Test
    void CreateAccount_WithBlankOwner_ThrowsInvalidAccountException() {
        assertThatThrownBy(() -> new Account("", "USD", AccountType.CUSTOMER, false))
                .isInstanceOf(InvalidAccountException.class);
    }

    @Test
    void CreateAccount_BeingCustomerWithAllowNegativeTrue_ThrowsInvalidAccountException() {
        assertThatThrownBy(() -> new Account("Ana", "USD", AccountType.CUSTOMER, true))
                .isInstanceOf(InvalidAccountException.class);
    }

    @Test
    void CreateAccount_TypeNull_ThrowsInvalidAccountException() {
        assertThatThrownBy(() -> new Account("Ana", "USD", null, false))
                .isInstanceOf(InvalidAccountException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "usd", "uSD", "usD", "Usd" })
    void CreateAccount_CurrencyShouldTransform_ReturnsCurrencyToUpperCase(String currency) {
        Account account = new Account("Ana", currency, AccountType.CUSTOMER, false);
        assertEquals(account.getCurrency(), "USD");
    }
}