package com.tonycode2.software.ledger_service.ledger.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.tonycode2.software.ledger_service.account.model.Account;
import com.tonycode2.software.ledger_service.account.model.enums.AccountType;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidEntryException;

public class EntryTest {

    private Account account;
    private LedgerTransaction transaction;

    @BeforeEach
    void setUp() {
        this.account = new Account("Ana", "USD", AccountType.CUSTOMER, false);
        this.transaction = new LedgerTransaction("Id", "key", TransactionType.DEPOSIT, null);
    }

    @Test
    void CreateEntry_TransactionIsNull_ThrowsInvalidEntryException() {
        assertThatThrownBy(() -> new Entry(null, account, EntryDirection.CREDIT, 100))
                .isInstanceOf(InvalidEntryException.class).hasMessageContaining("Transaction can not be null");
    }

    @Test
    void CreateEntry_AccountIsNull_ThrowsInvalidEntryException() {
        assertThatThrownBy(() -> new Entry(transaction, null, EntryDirection.CREDIT, 100))
                .isInstanceOf(InvalidEntryException.class).hasMessageContaining(("Account can not be null"));
    }

    @Test
    void CreateEntry_EntryDirectionIsNull_ThrowsInvalidEntryException() {
        assertThatThrownBy(() -> new Entry(transaction, account, null, 100))
                .isInstanceOf(InvalidEntryException.class).hasMessageContaining("The EntryDirection must not be null");
    }

    @ParameterizedTest
    @ValueSource(longs = { -2L, -100L, -1000L, -10000L })
    void CreateEntry_AmountIsLessThanZero_ThrowsInvalidEntryException(long amount) {
        assertThatThrownBy(
                () -> new Entry(transaction, account, EntryDirection.DEBIT, amount))
                .isInstanceOf(InvalidEntryException.class).hasMessageContaining("The Amount must be positive");
    }

    @Test
    void CreateEntry_HappyCase_ReturnsCorrectlyCreatedEntry() {
        Entry entry = new Entry(transaction, account, EntryDirection.CREDIT, 100);
        assertNull(entry.getId());
        assertNotNull(entry.getTransaction());
        assertNotNull(entry.getAccount());
        assertEquals(entry.getEntryDirection(), EntryDirection.CREDIT);
        assertEquals(entry.getAmount(), 100);
        assertNotNull(entry.getCreatedAt());
    }
}