package com.tonycode2.software.ledger_service.account.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tonycode2.software.ledger_service.account.model.enums.AccountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String owner;
    @Column(length = 3)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;
    @Enumerated(EnumType.STRING)
    private AccountType type;
    @Column(name = "allow_negative")
    private boolean allowNegative = false;
    private long balance = 0L;
    @Version
    private long version = 0L;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    protected Account() {
    }

    public Account(String owner, String currency, AccountType type, boolean allowNegative) {
        this.owner = owner;
        this.currency = currency;
        this.type = type;
        this.allowNegative = allowNegative;
        this.balance = 0l;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountType getType() {
        return type;
    }

    public boolean isAllowNegative() {
        return allowNegative;
    }

    public long getBalance() {
        return balance;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
