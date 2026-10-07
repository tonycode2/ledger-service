package com.tonycode2.software.ledger_service.account.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tonycode2.software.ledger_service.account.model.enums.AccountType;
import com.tonycode2.software.ledger_service.common.exceptions.InsufficientFundsException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidAccountException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidAmountException;

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
    private boolean allowNegative;
    private long balance;
    @Version
    private long version;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected Account() {
    }

    public Account(String owner, String currency, AccountType type, boolean allowNegative) {
        if (owner == null || owner.isBlank())
            throw new InvalidAccountException("Owner is required");
        if (currency == null || currency.isBlank() || !currency.matches("[A-Za-z]{3}"))
            throw new InvalidAccountException("Currency must be 3 letters long");
        if (allowNegative && type != AccountType.SYSTEM)
            throw new InvalidAccountException("Only system accounts allow negative balance");
        if (type == null)
            throw new InvalidAccountException("Type is required");
        this.owner = owner;
        this.currency = currency.toUpperCase();
        this.type = type;
        this.allowNegative = allowNegative;
        this.balance = 0L;
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

    public void credit(long amount) {
        if (!(amount > 0))
            throw new InvalidAmountException(amount);

        this.balance = Math.addExact(this.balance, amount);
    }

    public void debit(long amount) {
        if (!(amount > 0))
            throw new InvalidAmountException(amount);

        if (!this.allowNegative && amount > this.balance)
            throw new InsufficientFundsException(this.id, this.balance, amount);

        this.balance = Math.subtractExact(this.balance, amount);
    }

    public boolean hasSameCurrency(Account other) {
        return other.getCurrency().equals(this.getCurrency());
    }
}
