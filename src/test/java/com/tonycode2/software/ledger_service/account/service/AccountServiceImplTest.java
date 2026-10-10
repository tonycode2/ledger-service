package com.tonycode2.software.ledger_service.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.tonycode2.software.ledger_service.account.dto.AccountResponse;
import com.tonycode2.software.ledger_service.account.dto.CreateAccountRequest;
import com.tonycode2.software.ledger_service.account.model.Account;
import com.tonycode2.software.ledger_service.account.model.enums.AccountType;
import com.tonycode2.software.ledger_service.account.repository.AccountRepository;
import com.tonycode2.software.ledger_service.common.exceptions.AccountNotFoundException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidAccountException;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {
    @Mock
    private AccountRepository repository;
    @InjectMocks
    private AccountServiceImpl service;

    private CreateAccountRequest correctRequest;
    private CreateAccountRequest incorrectOwnerRequest;

    @BeforeEach
    void setUp() {
        correctRequest = new CreateAccountRequest("Ana", "USD");
        incorrectOwnerRequest = new CreateAccountRequest("     ", "USD");
    }

    @Test
    void CreateAccount_CorrectUserCreated_ReturnsAccountResponseWithForcedCustomerTypeAndNoNegativeBalance() {
        when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createAccount(correctRequest);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        verify(repository).save(captor.capture());
        Account saved = captor.getValue();

        assertThat(saved.getType()).isEqualTo(AccountType.CUSTOMER);
        assertThat(saved.isAllowNegative()).isFalse();
        assertThat(saved.getBalance()).isZero();
    }

    @Test
    void CreateAccount_CorrectUserCreated_ReturnsAccountWithAccountData() {
        when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = service.createAccount(correctRequest);
        assertThat(response.owner()).isEqualTo("Ana");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.balance()).isZero();

        assertThat(response.id()).isNull();
    }

    @Test
    void CreateAccount_OwnerNotCorrect_ThrowsAndDoesNotSave() {
        assertThatThrownBy(() -> service.createAccount(incorrectOwnerRequest))
                .isInstanceOf(InvalidAccountException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void GetAccountById_WhenExists_ReturnRespose() {
        UUID id = UUID.randomUUID();
        Account account = new Account("Ana", "USD", AccountType.CUSTOMER, false);
        ReflectionTestUtils.setField(account, "id", id);
        when(repository.findById(id)).thenReturn(Optional.of(account));

        AccountResponse response = service.getAccountById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.owner()).isEqualTo("Ana");
    }

    @Test
    void GetAccountById_WhenNotExists_ThrowsAccountNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAccountById(id)).isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
