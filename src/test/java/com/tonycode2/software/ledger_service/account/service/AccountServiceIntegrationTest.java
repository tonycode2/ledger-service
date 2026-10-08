package com.tonycode2.software.ledger_service.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.tonycode2.software.ledger_service.account.dto.AccountResponse;
import com.tonycode2.software.ledger_service.account.dto.CreateAccountRequest;
import com.tonycode2.software.ledger_service.account.model.Account;
import com.tonycode2.software.ledger_service.account.model.enums.AccountType;
import com.tonycode2.software.ledger_service.account.repository.AccountRepository;
import com.tonycode2.software.ledger_service.common.exceptions.AccountNotFoundException;

@SpringBootTest
@Testcontainers
public class AccountServiceIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private AccountService service;

    @Autowired
    private AccountRepository repository;

    @BeforeEach
    void cleanCustonerAccounts() {
        repository.deleteAll(repository.findAll().stream()
                .filter(a -> a.getType() == AccountType.CUSTOMER)
                .toList());
    }

    @Test
    void CreateThenGet_RoundTrip() {
        AccountResponse created = service.createAccount(new CreateAccountRequest("Ana", "USD"));
        AccountResponse found = service.getAccountById(created.id());

        assertThat(created.id()).isNotNull();
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.owner()).isEqualTo("Ana");
        assertThat(found.balance()).isZero();
    }

    @Test
    void CreateAccount_PersistsExpectedState() {
        AccountResponse created = service.createAccount(new CreateAccountRequest("Ana", "USD"));

        Account saved = repository.findById(created.id()).orElseThrow();

        assertThat(saved.getType()).isEqualTo(AccountType.CUSTOMER);
        assertThat(saved.isAllowNegative()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getVersion()).isZero();
    }

    @Test
    void GetAccountById_WhenMissing_ThrowsAccountNotFound() {
        assertThatThrownBy(() -> service.getAccountById(UUID.randomUUID()))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
