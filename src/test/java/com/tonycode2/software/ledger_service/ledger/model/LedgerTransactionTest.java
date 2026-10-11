package com.tonycode2.software.ledger_service.ledger.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.tonycode2.software.ledger_service.common.exceptions.InvalidLedgerTransactionException;

public class LedgerTransactionTest {
    @Test
    void CreateLedgerTransaction_IdempotencyKeyNull_ThrowsInvalidLedgerTransactionException() {
        assertThatThrownBy(() -> new LedgerTransaction(null, "key", TransactionType.DEPOSIT, null))
                .isInstanceOf(InvalidLedgerTransactionException.class)
                .hasMessageContaining("The Idempotency Key must not be null or blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "", " ", "  ", "   " })
    void CreateLedgerTransaction_IdempotencyKeyBlank_ThrowsInvalidLedgerTransactionException(String idempotencyKey) {
        assertThatThrownBy(() -> new LedgerTransaction(idempotencyKey, null, TransactionType.DEPOSIT, null))
                .isInstanceOf(InvalidLedgerTransactionException.class)
                .hasMessageContaining("The Idempotency Key must not be null or blank");
    }

    @Test
    void CreateLedgerTransaction_RequestHashNull_ThrowsInvalidLedgerTransactionException() {
        assertThatThrownBy(() -> new LedgerTransaction("Id", null, TransactionType.DEPOSIT, null))
                .isInstanceOf(InvalidLedgerTransactionException.class)
                .hasMessageContaining("The Request Hash must not be null or blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "", " ", "  ", "   " })
    void CreateLedgerTransaction_RequestHashBlank_ThrowsInvalidLedgerTransactionException(String requestHash) {
        assertThatThrownBy(() -> new LedgerTransaction("Id", requestHash, TransactionType.DEPOSIT, null))
                .isInstanceOf(InvalidLedgerTransactionException.class)
                .hasMessageContaining("The Request Hash must not be null or blank");
    }

    @Test
    void CreateLedgerTransaction_TransactionTypeIsNull_ThrowsInvalidLedgerTransactionException() {
        assertThatThrownBy(() -> new LedgerTransaction("Id", "key", null, null))
                .isInstanceOf(InvalidLedgerTransactionException.class)
                .hasMessageContaining("The Transaction Type must not be null");
    }

    @Test
    void CreateLedgerTransaction_TransactionTypeIsReversalButReversesIdIsNull_ThrowsInvalidLedgerTransactionException() {
        assertThatThrownBy(() -> new LedgerTransaction("Id", "key", TransactionType.REVERSAL, null))
                .isInstanceOf(InvalidLedgerTransactionException.class)
                .hasMessageContaining("f the Transaction Type is REVERSAL, the Reverses ID can not be null");
    }

    @Test
    void CreateLedgerTransaction_TransactionTypeIsNotReversalAndReversesIdIsNotNull_ThrowsInvalidLedgerTransactionException() {
        assertThatThrownBy(() -> new LedgerTransaction("Id", "key", TransactionType.DEPOSIT, UUID.randomUUID()))
                .isInstanceOf(InvalidLedgerTransactionException.class)
                .hasMessageContaining("If the Transaction Type is DEPOSIT or TRANSFER, the Reverses ID must be null");
    }

    void CreateLedgerTransaction_HappyCase_ReturnsCorrectlyCreatedLedgerTransaction() {
        LedgerTransaction transaction = new LedgerTransaction("Id", "key", TransactionType.DEPOSIT, null);
        assertNull(transaction.getId());
        assertEquals(transaction.getIdempotencyKey(), "Id");
        assertEquals(transaction.getRequestHash(), "key");
        assertNotNull(transaction.getType());
        assertEquals(transaction.getType(), TransactionType.DEPOSIT);
        assertNull(transaction.getReversesId());
        assertNotNull(transaction.getCreatedAt());

    }
}
