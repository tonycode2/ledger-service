package com.tonycode2.software.ledger_service.migration;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class MigrationTest {

        @Container
        @ServiceConnection
        static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

        @Autowired
        private JdbcTemplate jdbc;

        @Test
        void shouldThrowExceptionWhenTransactionIsUnbalanced() {
                UUID accountId = UUID.randomUUID();
                UUID txId = UUID.randomUUID();

                jdbc.update("INSERT INTO accounts (id, owner, currency, type) VALUES (?, 'Tony', 'USD', 'CUSTOMER')",
                                accountId);
                jdbc.update(
                                "INSERT INTO transactions (id, idempotency_key, request_hash, type) VALUES (?, 'key123', 'hash123', 'DEPOSIT')",
                                txId);

                jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
                Exception exception = assertThrows(Exception.class, () -> {
                        jdbc.update(
                                        "INSERT INTO entries (transaction_id, account_id, direction, amount) VALUES (?, ?, 'CREDIT', 100)",
                                        txId, accountId);
                });

                org.assertj.core.api.Assertions.assertThat(exception.getMessage())
                                .contains("is unbalanced");
        }

        @Test
        void shouldPreventUpdateOnEntries() {
                UUID accountId = UUID.randomUUID();
                UUID txId = UUID.randomUUID();

                jdbc.update("INSERT INTO accounts (id, owner, currency, type) VALUES (?, 'Tony', 'USD', 'CUSTOMER')",
                                accountId);
                jdbc.update(
                                "INSERT INTO transactions (id, idempotency_key, request_hash, type) VALUES (?, 'key_update', 'hash_update', 'DEPOSIT')",
                                txId);
                jdbc.update("INSERT INTO entries (transaction_id, account_id, direction, amount) VALUES (?, ?, 'CREDIT', 100)",
                                txId, accountId);

                Exception updateException = assertThrows(Exception.class, () -> {
                        jdbc.update("UPDATE entries SET amount = 200 WHERE transaction_id = ?", txId);
                });

                org.assertj.core.api.Assertions.assertThat(updateException.getMessage())
                                .contains("entries are immutable");
        }

        @Test
        void shouldPreventDeleteOnEntries() {
                UUID accountId = UUID.randomUUID();
                UUID txId = UUID.randomUUID();

                jdbc.update("INSERT INTO accounts (id, owner, currency, type) VALUES (?, 'Tony', 'USD', 'CUSTOMER')",
                                accountId);
                jdbc.update(
                                "INSERT INTO transactions (id, idempotency_key, request_hash, type) VALUES (?, 'key_delete', 'hash_delete', 'DEPOSIT')",
                                txId);
                jdbc.update("INSERT INTO entries (transaction_id, account_id, direction, amount) VALUES (?, ?, 'CREDIT', 100)",
                                txId, accountId);

                Exception deleteException = assertThrows(Exception.class, () -> {
                        jdbc.update("DELETE FROM entries WHERE transaction_id = ?", txId);
                });

                org.assertj.core.api.Assertions.assertThat(deleteException.getMessage())
                                .contains("entries are immutable");
        }

        @Test
        void shouldPreventZeroAmountInEntries() {
                UUID accountId = UUID.randomUUID();
                UUID txId = UUID.randomUUID();

                jdbc.update("INSERT INTO accounts (id, owner, currency, type) VALUES (?, 'Tony', 'USD', 'CUSTOMER')",
                                accountId);
                jdbc.update(
                                "INSERT INTO transactions (id, idempotency_key, request_hash, type) VALUES (?, 'key_zero', 'hash_zero', 'DEPOSIT')",
                                txId);

                Exception zeroException = assertThrows(Exception.class, () -> {
                        jdbc.update(
                                        "INSERT INTO entries (transaction_id, account_id, direction, amount) VALUES (?, ?, 'CREDIT', 0)",
                                        txId, accountId);
                });

                org.assertj.core.api.Assertions.assertThat(zeroException.getMessage())
                                .contains("violates check constraint");
        }

        @Test
        void shouldPreventNegativeAmountInEntries() {
                UUID accountId = UUID.randomUUID();
                UUID txId = UUID.randomUUID();

                jdbc.update("INSERT INTO accounts (id, owner, currency, type) VALUES (?, 'Tony', 'USD', 'CUSTOMER')",
                                accountId);
                jdbc.update(
                                "INSERT INTO transactions (id, idempotency_key, request_hash, type) VALUES (?, 'key_negative', 'hash_negative', 'DEPOSIT')",
                                txId);

                Exception negativeException = assertThrows(Exception.class, () -> {
                        jdbc.update(
                                        "INSERT INTO entries (transaction_id, account_id, direction, amount) VALUES (?, ?, 'CREDIT', -50)",
                                        txId, accountId);
                });

                org.assertj.core.api.Assertions.assertThat(negativeException.getMessage())
                                .contains("violates check constraint");
        }
}
