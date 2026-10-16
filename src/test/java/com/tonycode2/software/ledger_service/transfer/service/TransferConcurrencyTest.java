package com.tonycode2.software.ledger_service.transfer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.tonycode2.software.ledger_service.account.dto.CreateAccountRequest;
import com.tonycode2.software.ledger_service.account.service.AccountService;
import com.tonycode2.software.ledger_service.common.exceptions.InsufficientFundsException;
import com.tonycode2.software.ledger_service.deposit.dto.CreateDeposit;
import com.tonycode2.software.ledger_service.deposit.service.DepositServiceImpl;
import com.tonycode2.software.ledger_service.transfer.dto.CreateTransfer;

/**
 * Pruebas de concurrencia sobre transferencias, contra PostgreSQL real.
 *
 * IMPORTANTE: esta clase NO lleva @Transactional. Cada hilo necesita su propia
 * transaccion; si el test abriera una, todos los hilos quedarian dentro de
 * ella.
 *
 * Los montos estan en centavos: "100" es el saldo inicial de Ana y cada
 * transferencia mueve "1".
 *
 * Como usarlo:
 * 1) Corre los tres tests con el codigo SIN bloqueo (solo @Version).
 * - invariants_holdUnderConcurrency -> debe pasar (saldos correctos, muchos
 * conflictos)
 * - withLocking_... -> debe FALLAR (rojo): se aceptan menos de 100
 * - crossTransfers_... -> debe FALLAR (rojo): conflictos de version
 * 2) Aplica el bloqueo pesimista con orden por id. Los tres deben pasar.
 * 3) Experimento: quita @Version de Account y bloquea desactivado ->
 * invariants_ falla.
 *
 * Imports de Testcontainers: segun la version, PostgreSQLContainer puede estar
 * en
 * org.testcontainers.postgresql (sin genericos) o en
 * org.testcontainers.containers
 * (PostgreSQLContainer<?>). Si ya tienes TestcontainersConfiguration, borra
 * ContainerConfig y usa @Import(TestcontainersConfiguration.class).
 */
@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=40",
        "spring.datasource.hikari.connection-timeout=60000"
})
@Import(TransferConcurrencyTest.ContainerConfig.class)
class TransferConcurrencyTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainerConfig {
        @Bean
        @ServiceConnection
        static PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18");
        }
    }

    @Autowired
    private TransferService transferService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private DepositServiceImpl depositService;
    @Autowired
    private JdbcTemplate jdbc;

    // ---------- Limpieza entre tests ----------

    /*
     * No se puede usar deleteAll(): un trigger prohibe DELETE sobre entries.
     * TRUNCATE no dispara triggers por fila. EXTERNAL_FUNDS conserva su saldo
     * entre tests, por eso se resetea.
     */
    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE entries, transactions, outbox RESTART IDENTITY");
        jdbc.execute("DELETE FROM accounts WHERE type = 'CUSTOMER'");
        jdbc.execute("UPDATE accounts SET balance = 0, version = 0 WHERE type = 'SYSTEM'");
    }

    // ---------- Tests ----------

    /** Invariantes que deben cumplirse SIEMPRE, con o sin bloqueo. */
    @RepeatedTest(3)
    void invariants_holdUnderConcurrency() throws Exception {
        UUID ana = createFundedAccount("Ana", 100);
        UUID beto = createFundedAccount("Beto", 0);

        Results results = runAll(Collections.nCopies(200, new CreateTransfer(ana, beto, 1L, "USD")));
        System.out.println("invariants_holdUnderConcurrency -> " + results);

        assertThat(results.total()).isEqualTo(200);
        assertThat(results.count(Outcome.OTHER))
                .as("errores inesperados: %s", results.otherErrors()).isZero();
        assertLedgerIsConsistent(ana, beto);
        assertThat(balanceOf(ana)).isEqualTo(100 - results.count(Outcome.SUCCESS));
        assertThat(balanceOf(beto)).isEqualTo(results.count(Outcome.SUCCESS));
    }

    /** Con bloqueo, se aceptan EXACTAMENTE las que alcanzan: 100 de 200. */
    @RepeatedTest(3)
    void withLocking_exactlyTheAffordableTransfersSucceed() throws Exception {
        UUID ana = createFundedAccount("Ana", 100);
        UUID beto = createFundedAccount("Beto", 0);

        Results results = runAll(Collections.nCopies(200, new CreateTransfer(ana, beto, 1L, "USD")));
        System.out.println("withLocking -> " + results);

        assertThat(results.count(Outcome.SUCCESS)).isEqualTo(100);
        assertThat(results.count(Outcome.INSUFFICIENT_FUNDS)).isEqualTo(100);
        assertThat(results.count(Outcome.OPTIMISTIC_CONFLICT)).isZero();
        assertThat(results.count(Outcome.OTHER)).as("%s", results.otherErrors()).isZero();
        assertThat(balanceOf(ana)).isZero();
        assertThat(balanceOf(beto)).isEqualTo(100);
        assertLedgerIsConsistent(ana, beto);
    }

    /**
     * A->B y B->A a la vez, con fondos de sobra: nadie debe fallar ni haber
     * deadlock.
     */
    @RepeatedTest(3)
    void crossTransfers_doNotDeadlock() throws Exception {
        UUID a = createFundedAccount("A", 1000);
        UUID b = createFundedAccount("B", 1000);

        List<CreateTransfer> requests = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            requests.add(new CreateTransfer(a, b, 1L, "USD"));
            requests.add(new CreateTransfer(b, a, 1L, "USD"));
        }

        Results results = runAll(requests);
        System.out.println("crossTransfers -> " + results);

        assertThat(results.count(Outcome.SUCCESS)).isEqualTo(200);
        assertThat(results.count(Outcome.LOCK_FAILURE)).as("deadlocks / fallos de bloqueo").isZero();
        assertThat(results.count(Outcome.OPTIMISTIC_CONFLICT)).isZero();
        assertThat(balanceOf(a)).isEqualTo(1000);
        assertThat(balanceOf(b)).isEqualTo(1000);
        assertLedgerIsConsistent(a, b);
    }

    // ---------- Runner concurrente ----------

    enum Outcome {
        SUCCESS, INSUFFICIENT_FUNDS, OPTIMISTIC_CONFLICT, LOCK_FAILURE, OTHER
    }

    record Results(Map<Outcome, Integer> counts, Set<String> otherErrors) {
        int count(Outcome outcome) {
            return counts.getOrDefault(outcome, 0);
        }

        int total() {
            return counts.values().stream().mapToInt(Integer::intValue).sum();
        }

        @Override
        public String toString() {
            return counts + (otherErrors.isEmpty() ? "" : " other=" + otherErrors);
        }
    }

    private Results runAll(List<CreateTransfer> requests) throws Exception {
        int n = requests.size();
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        Set<String> otherErrors = ConcurrentHashMap.newKeySet();
        List<Future<Outcome>> futures = new ArrayList<>();

        for (CreateTransfer request : requests) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    transferService.transfer(request, "key-" + UUID.randomUUID());
                    return Outcome.SUCCESS;
                } catch (InsufficientFundsException e) {
                    return Outcome.INSUFFICIENT_FUNDS;
                } catch (OptimisticLockingFailureException e) {
                    return Outcome.OPTIMISTIC_CONFLICT;
                } catch (PessimisticLockingFailureException e) {
                    return Outcome.LOCK_FAILURE; // incluye deadlocks
                } catch (Exception e) {
                    otherErrors.add(e.getClass().getName() + ": " + e.getMessage());
                    return Outcome.OTHER;
                }
            }));
        }

        ready.await();
        start.countDown();

        Map<Outcome, Integer> counts = new EnumMap<>(Outcome.class);
        for (Future<Outcome> future : futures) {
            counts.merge(future.get(60, TimeUnit.SECONDS), 1, Integer::sum);
        }
        pool.shutdown();
        return new Results(counts, otherErrors);
    }

    // ---------- Helpers de datos y comprobaciones ----------

    private UUID createFundedAccount(String owner, long funds) {
        UUID id = accountService.createAccount(new CreateAccountRequest(owner, "USD")).id();
        if (funds > 0) {
            depositService.deposit(new CreateDeposit(id, funds, "USD"), "seed-" + UUID.randomUUID());
        }
        return id;
    }

    private long balanceOf(UUID accountId) {
        return jdbc.queryForObject("SELECT balance FROM accounts WHERE id = ?", Long.class, accountId);
    }

    /**
     * Creditos menos debitos de una cuenta, segun sus entries (la "verdad" del
     * ledger).
     */
    private long entriesNet(UUID accountId) {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE -amount END), 0) "
                        + "FROM entries WHERE account_id = ?",
                Long.class, accountId);
    }

    private long ledgerNet() {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE -amount END), 0) FROM entries",
                Long.class);
    }

    /**
     * Lo que NUNCA debe romperse: suma global cero y saldo cacheado == suma de
     * entries.
     */
    private void assertLedgerIsConsistent(UUID... accountIds) {
        assertThat(ledgerNet()).as("la suma firmada de todos los entries").isZero();
        for (UUID id : accountIds) {
            assertThat(balanceOf(id)).as("saldo cacheado de %s", id).isEqualTo(entriesNet(id));
            assertThat(balanceOf(id)).as("saldo de %s no negativo", id).isGreaterThanOrEqualTo(0);
        }
    }
}
