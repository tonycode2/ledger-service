package com.tonycode2.software.ledger_service.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.tonycode2.software.ledger_service.account.dto.AccountResponse;
import com.tonycode2.software.ledger_service.account.dto.CreateAccountRequest;
import com.tonycode2.software.ledger_service.account.model.Account;
import com.tonycode2.software.ledger_service.account.model.enums.AccountType;
import com.tonycode2.software.ledger_service.account.repository.AccountRepository;
import com.tonycode2.software.ledger_service.common.exceptions.AccountNotFoundException;
import com.tonycode2.software.ledger_service.common.exceptions.IllegalPageablePropertiesException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidAccountException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidSortedByException;
import com.tonycode2.software.ledger_service.ledger.dto.EntryResponse;
import com.tonycode2.software.ledger_service.ledger.model.Entry;
import com.tonycode2.software.ledger_service.ledger.model.LedgerTransaction;
import com.tonycode2.software.ledger_service.ledger.model.enums.EntryDirection;
import com.tonycode2.software.ledger_service.ledger.model.enums.TransactionType;
import com.tonycode2.software.ledger_service.ledger.repository.EntryRepository;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {
    @Mock
    private AccountRepository repository;
    @Mock
    private EntryRepository entryRepository;
    @InjectMocks
    private AccountServiceImpl service;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

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

    @Test
    void GetEntriesByAccountId_IncorrectSortedBy_ThrowsInvalidSortedByException() {
        assertThatThrownBy(() -> service.getEntriesByAccountId(UUID.randomUUID(), 0, 0, "incorrect"))
                .isInstanceOf(InvalidSortedByException.class).hasMessageContaining("property you want to sort");
        verify(repository, never()).findByCurrencyAndType(any(), any());
    }

    @Test
    void GetEntriesByAccountId_IllegalPageableProperties_ThrowsIllegalPageablePropertiesException() {
        assertThatThrownBy(() -> service.getEntriesByAccountId(UUID.randomUUID(), -10, 0, "createdAt"))
                .isInstanceOf(IllegalPageablePropertiesException.class).hasMessageContaining("must be one of");
        verify(repository, never()).findByCurrencyAndType(any(), any());
    }

    @Test
    void GetEntriesByAccountId_AccountNotFound_ThrowsAccountNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getEntriesByAccountId(id, 0, 1, "createdAt"))
                .isInstanceOf(AccountNotFoundException.class).hasMessageContaining("not found");

        verify(repository, never()).findByCurrencyAndType(any(), any());
    }

    @Test
    void GetEntriesByAccountId_HappyCase_ReturnsPageable() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account("Ana", "USD", AccountType.CUSTOMER, false);
        ReflectionTestUtils.setField(account, "id", accountId);

        UUID transactionId = UUID.randomUUID();
        LedgerTransaction transaction = new LedgerTransaction("key-1", "hash-1", TransactionType.DEPOSIT, null);
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        Entry entry = new Entry(transaction, account, EntryDirection.CREDIT, 10_000L);

        Page<Entry> repositoryPage = new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1);

        when(repository.findById(accountId)).thenReturn(Optional.of(account));
        when(entryRepository.findByAccount_Id(eq(accountId), any(Pageable.class))).thenReturn(repositoryPage);

        Page<EntryResponse> result = service.getEntriesByAccountId(accountId, 0, 20, "createdAt");

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        EntryResponse response = result.getContent().get(0);
        assertThat(response.amount()).isEqualTo(10_000L);
        assertThat(response.transactionId()).isEqualTo(transactionId);

        verify(entryRepository).findByAccount_Id(eq(accountId), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isZero();
        assertThat(used.getPageSize()).isEqualTo(20);
        assertThat(used.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);

    }
}
