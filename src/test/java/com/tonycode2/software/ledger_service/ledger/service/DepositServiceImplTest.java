package com.tonycode2.software.ledger_service.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.tonycode2.software.ledger_service.account.model.Account;
import com.tonycode2.software.ledger_service.account.model.enums.AccountType;
import com.tonycode2.software.ledger_service.account.repository.AccountRepository;
import com.tonycode2.software.ledger_service.common.exceptions.IdempotencyKeyReusedException;
import com.tonycode2.software.ledger_service.ledger.dto.CreateDeposit;
import com.tonycode2.software.ledger_service.ledger.model.Entry;
import com.tonycode2.software.ledger_service.ledger.model.LedgerTransaction;
import com.tonycode2.software.ledger_service.ledger.model.enums.EntryDirection;
import com.tonycode2.software.ledger_service.ledger.model.enums.TransactionType;
import com.tonycode2.software.ledger_service.ledger.repository.EntryRepository;
import com.tonycode2.software.ledger_service.ledger.repository.LedgerTransactionRepository;

@ExtendWith(MockitoExtension.class)
public class DepositServiceImplTest {

    @Mock
    private LedgerTransactionRepository ledgerTransactionRepository;
    @Mock
    private EntryRepository entryRepository;
    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private DepositServiceImpl service;

    @Captor
    private ArgumentCaptor<LedgerTransaction> transactionCaptor;
    @Captor
    private ArgumentCaptor<List<Entry>> entriesCaptor;

    private Account customer;
    private Account external;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customer = new Account("Ana", "USD", AccountType.CUSTOMER, false);
        ReflectionTestUtils.setField(customer, "id", customerId); // el id lo genera Hibernate; aqui lo forzamos

        external = new Account("EXTERNAL_FUNDS", "USD", AccountType.SYSTEM, true);
        ReflectionTestUtils.setField(external, "id", UUID.randomUUID());
    }

    /** Stubs comunes del camino feliz (clave nueva, cuentas existentes). */
    private void stubHappyPath(String key) {
        when(ledgerTransactionRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(accountRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(accountRepository.findByCurrencyAndType("USD", AccountType.SYSTEM)).thenReturn(List.of(external));
        // save() de un mock devuelve null; devolvemos el mismo objeto que recibe
        when(ledgerTransactionRepository.save(any(LedgerTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void deposit_newKey_createsBalancedEntriesAndMovesMoney() {
        stubHappyPath("key-1");

        service.deposit(new CreateDeposit(customerId, 10_000L, "USD"), "key-1");

        // la transaccion guardada
        verify(ledgerTransactionRepository).save(transactionCaptor.capture());
        LedgerTransaction saved = transactionCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(saved.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(saved.getRequestHash()).hasSize(64);

        // los dos entries
        verify(entryRepository).saveAll(entriesCaptor.capture());
        List<Entry> entries = entriesCaptor.getValue();
        assertThat(entries).hasSize(2);

        Entry credit = entries.stream().filter(e -> e.getEntryDirection() == EntryDirection.CREDIT).findFirst()
                .orElseThrow();
        Entry debit = entries.stream().filter(e -> e.getEntryDirection() == EntryDirection.DEBIT).findFirst()
                .orElseThrow();
        assertThat(credit.getAccount()).isSameAs(customer);
        assertThat(debit.getAccount()).isSameAs(external);
        assertThat(credit.getAmount()).isEqualTo(10_000L);
        assertThat(debit.getAmount()).isEqualTo(10_000L);

        // los saldos de verdad cambiaron
        assertThat(customer.getBalance()).isEqualTo(10_000L);
        assertThat(external.getBalance()).isEqualTo(-10_000L);
    }

    @Test
    void deposit_sameKeyAndSameContent_isARetryAndMovesNoMoney() {
        stubHappyPath("key-1");
        CreateDeposit request = new CreateDeposit(customerId, 10_000L, "USD");

        service.deposit(request, "key-1"); // primer intento: se ejecuta

        // capturamos lo que se guardo y lo devolvemos como "ya existe" en el reintento
        verify(ledgerTransactionRepository).save(transactionCaptor.capture());
        LedgerTransaction original = transactionCaptor.getValue();
        when(ledgerTransactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(original));

        service.deposit(request, "key-1"); // reintento

        // sigue habiendo UNA sola escritura de cada tipo, y el saldo no se duplico
        verify(ledgerTransactionRepository, times(1)).save(any(LedgerTransaction.class));
        verify(entryRepository, times(1)).saveAll(any());
        assertThat(customer.getBalance()).isEqualTo(10_000L);
    }

    @Test
    void deposit_sameKeyButDifferentContent_throwsAndMovesNoMoney() {
        stubHappyPath("key-1");
        service.deposit(new CreateDeposit(customerId, 10_000L, "USD"), "key-1");

        verify(ledgerTransactionRepository).save(transactionCaptor.capture());
        when(ledgerTransactionRepository.findByIdempotencyKey("key-1"))
                .thenReturn(Optional.of(transactionCaptor.getValue()));

        // misma clave, otro monto
        assertThatThrownBy(() -> service.deposit(new CreateDeposit(customerId, 50_000L, "USD"), "key-1"))
                .isInstanceOf(IdempotencyKeyReusedException.class);

        verify(ledgerTransactionRepository, times(1)).save(any(LedgerTransaction.class));
        assertThat(customer.getBalance()).isEqualTo(10_000L);
    }

    /**
     * Este test es "rojo" con el codigo actual: ver explicacion en la respuesta.
     * Escribelo, miralo fallar, y despues arregla el servicio.
     */
    @Test
    void deposit_lowercaseCurrency_isAcceptedAndNormalized() {
        stubHappyPath("key-2");

        service.deposit(new CreateDeposit(customerId, 10_000L, "usd"), "key-2");

        assertThat(customer.getBalance()).isEqualTo(10_000L);
    }

}
