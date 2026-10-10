package com.tonycode2.software.ledger_service.account.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.tonycode2.software.ledger_service.account.model.enums.AccountType;
import com.tonycode2.software.ledger_service.common.exceptions.InsufficientFundsException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidAccountException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidAmountException;

public class AccountTest {

    // Constructor tests
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "US", "USDD", "U$D", "    " })
    void CreateAccount_WithIncorrectCurrency_ThrowsInvalidAccountException(String currency) {
        assertThatThrownBy(() -> new Account("Ana", currency, AccountType.CUSTOMER, false))
                .isInstanceOf(InvalidAccountException.class).hasMessageContaining("Currency must be 3 letters long");
    }

    @Test
    void CreateAccount_WithNullOwner_ThrowsInvalidAccountException() {
        assertThatThrownBy(() -> new Account(null, "USD", AccountType.CUSTOMER, false))
                .isInstanceOf(InvalidAccountException.class).hasMessageContaining("Owner is required");
    }

    @Test
    void CreateAccount_WithBlankOwner_ThrowsInvalidAccountException() {
        assertThatThrownBy(() -> new Account("", "USD", AccountType.CUSTOMER, false))
                .isInstanceOf(InvalidAccountException.class).hasMessageContaining("Owner is required");
    }

    @Test
    void CreateAccount_BeingCustomerWithAllowNegativeTrue_ThrowsInvalidAccountException() {
        assertThatThrownBy(() -> new Account("Ana", "USD", AccountType.CUSTOMER, true))
                .isInstanceOf(InvalidAccountException.class)
                .hasMessageContaining("Only system accounts allow negative balance");
    }

    @Test
    void CreateAccount_TypeNull_ThrowsInvalidAccountException() {
        assertThatThrownBy(() -> new Account("Ana", "USD", null, false))
                .isInstanceOf(InvalidAccountException.class).hasMessageContaining("Type is required");
    }

    @ParameterizedTest
    @ValueSource(strings = { "usd", "uSD", "usD", "Usd" })
    void CreateAccount_CurrencyShouldTransform_ReturnsCurrencyToUpperCase(String currency) {
        Account account = new Account("Ana", currency, AccountType.CUSTOMER, false);
        assertEquals(account.getCurrency(), "USD");
    }

    // Happy case
    @Test
    void CreateAccount_HappyCase_ReturnsCorrectlyCreatedClass() {
        Account account = new Account("Ana", "USD", AccountType.CUSTOMER, false);
        assertEquals(account.getBalance(), 0);
        assertNotNull(account.getCreatedAt());
        assertNull(account.getId());
    }

    @Test
    void CreateSystemAccount_HappyCase_ReturnsCorrectlyCreatedClass() {
        Account account = new Account("Ana", "USD", AccountType.SYSTEM, true);
        assertTrue(account.isAllowNegative());
        assertEquals(account.getType(), AccountType.SYSTEM);
    }

    // Function tests
    @ParameterizedTest
    @ValueSource(longs = { 0, -2L, -10L, -1000L })
    void Credit_AmountIsLessThan0_ThrowsInvalidAmountException(long amount) {
        assertThatThrownBy(() -> {
            Account account = new Account("Ana", "USD", AccountType.CUSTOMER, false);
            account.credit(amount);
        }).isInstanceOf(InvalidAmountException.class).hasMessageContaining("The amount is not positive");
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, -2L, -10L, -1000L })
    void Debit_AmountIsLessThan0_ThrowsInvalidAmountException(long amount) {
        assertThatThrownBy(() -> {
            Account account = new Account("Ana", "USD", AccountType.CUSTOMER, false);
            account.debit(amount);
        }).isInstanceOf(InvalidAmountException.class).hasMessageContaining("The amount is not positive");
    }

    @ParameterizedTest
    @ValueSource(longs = { 101, 200L, 400L, 1000L })
    void Debit_AllowNegativeIsFalseAndAmountIsMoreThanBalance_ThrowsInsuficientFundsException(long amount) {
        assertThatThrownBy(() -> {
            Account account = new Account("Ana", "USD", AccountType.CUSTOMER, false);
            account.credit(100L);
            account.debit(amount);
        }).isInstanceOf(InsufficientFundsException.class).hasMessageContaining("requested");
    }

    @Test
    void HasSameCurrency_IfCurrencyIsNotTheSame_ReturnFalse() {
        Account account1 = new Account("Ana", "USD", AccountType.CUSTOMER, false);
        Account account2 = new Account("Ana", "CRC", AccountType.CUSTOMER, false);
        assertFalse(account1.hasSameCurrency(account2));
    }

}