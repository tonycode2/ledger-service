package com.tonycode2.software.ledger_service.ledger.model;

import java.time.Instant;

import org.hibernate.annotations.Immutable;

import com.tonycode2.software.ledger_service.account.model.Account;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidEntryException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "entries")
@Immutable
public class Entry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, updatable = false)
    private LedgerTransaction transaction;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;
    @Enumerated(EnumType.STRING)
    @Column(name = "direction")
    private EntryDirection EntryDirection;
    @Column(nullable = false, updatable = false)
    private long amount;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Entry() {
    }

    public Entry(LedgerTransaction transaction, Account account, EntryDirection EntryDirection, long amount) {
        if (transaction == null) {
            throw new InvalidEntryException("Transaction can not be null");
        }
        if (account == null) {
            throw new InvalidEntryException("Account can not be null");
        }
        if (EntryDirection == null) {
            throw new InvalidEntryException("The EntryDirection must not be null");
        }
        if (amount < 0) {
            throw new InvalidEntryException("The Amount must be positive");
        }
        this.transaction = transaction;
        this.account = account;
        this.EntryDirection = EntryDirection;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public LedgerTransaction getTransaction() {
        return transaction;
    }

    public Account getAccount() {
        return account;
    }

    public EntryDirection getEntryDirection() {
        return EntryDirection;
    }

    public long getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
