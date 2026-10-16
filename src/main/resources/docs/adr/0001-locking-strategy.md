# ADR 0001: Locking strategy for concurrent balance updates

- **Status:** Accepted
- **Date:** 2026-10-15
- **Scope:** `TransferServiceImpl`, `AccountRepository`

## Context

A transfer does three things: it reads the balance of the source account, checks that it
has enough funds, and writes the new balance. If two transfers run at the same time on the
same account, both can read the same balance before either one writes.

Example with an account holding 100 and two concurrent transfers of 80:

| Step | Transfer A | Transfer B |
|---|---|---|
| 1 | Reads balance: 100 | |
| 2 | | Reads balance: 100 |
| 3 | 100 >= 80, approved | 100 >= 80, approved |
| 4 | Writes 20 | |
| 5 | | Writes 20 |

The account paid 160 with 100 of funds. The funds check is correct at the moment it runs,
but the decision is applied later, on data that may already be outdated.

PostgreSQL's default isolation level (`READ COMMITTED`) does not prevent this. Each
statement sees only committed data, but nothing stops two transactions from reading the
same row before either one writes.

### Requirements

- A customer account never ends with a negative balance.
- Under contention, the transfers that the funds can cover are accepted, and only those.
- The balance of an account always equals the sum of its entries.
- Transfers in opposite directions (A to B and B to A) must not deadlock.
- The solution must be simple enough to reason about and to test.

## Options considered

### 1. No protection

The code only checks `balance >= amount` in Java.

- Lost updates: two transfers write absolute values computed from the same stale read.
- The cached balance can stop matching the sum of entries.
- Rejected: it breaks the main guarantee of the project.

### 2. Optimistic locking only (`@Version`)

Hibernate adds `WHERE version = ?` to every `UPDATE`. If another transaction changed the
row first, the update affects zero rows and the whole transaction is rolled back.

- Balances stay correct: no money is lost or duplicated.
- Under contention many valid transfers fail even though the funds were enough, because
  they lose the version race.
- It needs a retry mechanism: every retry is a new transaction, outside the failed one.
- Measured result: **12 of 200 transfers accepted** (the rest failing as `OPTIMISTIC_CONFLICT` due to version races).

### 3. Pessimistic locking with ordered acquisition (chosen)

`SELECT ... FOR UPDATE` on both accounts of the transfer, always in the same order.

- The second transaction waits for the first one to finish and then reads the committed
  balance.
- The outcome is deterministic: exactly the transfers the funds cover are accepted.
- Needs a fixed lock order to avoid deadlocks (see Decision).

### 4. `SERIALIZABLE` isolation

PostgreSQL aborts any transaction that could produce a result impossible to get by running
transactions one after another.

- Correct, and needs no explicit locks.
- It aborts transactions with a serialization error, so retry logic would be needed around
  every operation, not only transfers.
- It can abort transactions that do not really conflict, and it is the heaviest option.
- Rejected for now: more complexity everywhere for a problem that is local to account rows.

## Decision

Use **pessimistic locking with ordered acquisition** for transfers.

1. The accounts involved are locked one row at a time with `SELECT ... FOR UPDATE`
   (`AccountRepository.findByIdForUpdate`), **always the smaller id first**, no matter
   which one is the source and which one is the destination.
2. Accounts are loaded only after taking the lock, never before. This guarantees that the
   balance read is the latest committed one. If an account were loaded earlier in the same
   transaction, the stale copy could be reused.
3. Business rules (currency, account type, funds) are evaluated after the locks are
   held, so the balance they read cannot change underneath them.
4. The idempotency check runs **before** locking. A retry of a request that already
   succeeded returns the stored result without locking anything.
5. `@Version` stays on `Account` as a safety net, not as the main mechanism.
6. Lock and version conflicts that still happen are returned as `409 Conflict`, which tells
   the client that retrying is reasonable.

### Why the order matters

Without a fixed order, two opposite transfers can deadlock:

| Step | Transfer 1 (A to B) | Transfer 2 (B to A) |
|---|---|---|
| 1 | Locks A | Locks B |
| 2 | Waits for B | Waits for A |

Each one waits for the lock the other holds. PostgreSQL detects the cycle and aborts one
of them, which becomes an error for the client. If every transaction locks the smaller id
first, both ask for A and then for B, so the second one simply waits for the first one. A
cycle becomes impossible. Transfers that all go in the same direction cannot form a cycle
in any case.

### Why pessimistic and not optimistic

Contention is expected on specific accounts, for example a merchant receiving many
payments. Under contention, optimistic locking turns waiting into failures and retries.
Pessimistic locking turns it into queueing, which is slower in the worst case but gives
a predictable result and a simple behavior for the client.

## Consequences

### Positive

- Under contention, exactly the transfers the funds can cover are accepted (see results).
- No deadlocks between opposite transfers (verified by a test).
- Balances and entries stay consistent. `@Version` remains as a second line of defense.

### Negative and open points

- **Transfers on the same account are serialized.** Throughput on a hot account is
  limited by how fast one transaction at a time can finish.
- **Waiting transactions hold a database connection.** The connection pool size matters,
  and long transactions hurt everyone waiting behind them.
- **No lock timeout is configured yet.** A stuck transaction could make others wait for a
  long time. PostgreSQL's `lock_timeout` is a candidate for a follow-up.
- **Deposits are not protected yet.** Every deposit of a currency updates the same
  `EXTERNAL_FUNDS` row, so concurrent deposits can conflict. This is the next place to
  apply the same pattern. It is a "hot row" by design, and may need a different solution
  (for example, several system accounts per currency).
- **Concurrent retries with the same idempotency key** are still only guarded by the
  `UNIQUE` constraint, and the client may receive a generic error. This belongs to the
  idempotency work and is not addressed here.

## Verification

Tests in `TransferConcurrencyTest`, run against a real PostgreSQL with Testcontainers:

| Test | What it shows |
|---|---|
| `invariants_holdUnderConcurrency` | The sum of all entries is zero, and each cached balance equals the sum of its entries, with or without locking |
| `withLocking_exactlyTheAffordableTransfersSucceed` | 200 concurrent transfers of 1 from a balance of 100: exactly 100 are accepted and 100 are rejected for insufficient funds |
| `crossTransfers_doNotDeadlock` | 100 transfers A to B and 100 transfers B to A at the same time: all 200 succeed, no lock failures |

### Measured results

| Scenario | Accepted | Insufficient funds | Conflicts | Lock failures |
|---|---|---|---|---|
| `@Version` only (before) | 12 | 88 | 100 | 0 |
| Pessimistic + ordered locks (after) | 100 | 100 | 0 | 0 |
| Cross transfers, locks **not** ordered | 84 | n/a | 16 | 16 |
| Cross transfers, locks ordered | 200 | n/a | 0 | 0 |

*(Environment: Java 21.0.12.1, PostgreSQL 18.6 with Testcontainers, HikariCP default connection pool).*

### Experiments done

- Removing `@Version` and the lock: `invariants_holdUnderConcurrency` fails because the
  cached balance stops matching the sum of entries. [Confirmed]
- Locking source before destination without sorting: `crossTransfers_doNotDeadlock`
  reports lock failures from deadlocks. [Confirmed]

## Follow-ups

- Apply the same locking pattern to deposits, and decide what to do about the
  `EXTERNAL_FUNDS` hot row.
- Evaluate a lock timeout.
- Measure throughput on a hot account with a load test (k6) and record the numbers here.
- Handle concurrent requests with the same idempotency key explicitly.