package com.tonycode2.software.ledger_service.ledger.model;

import java.time.Instant;
import java.util.UUID;

import com.tonycode2.software.ledger_service.common.exceptions.InvalidLedgerTransactionException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class LedgerTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "idempotency_key", updatable = false)
    private String idempotencyKey;
    @Column(name = "request_hash", updatable = false, length = 64)
    private String requestHash;
    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private TransactionType type;
    @Column(name = "reverses_id", updatable = false)
    private UUID reversesId;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected LedgerTransaction() {
    }

    public LedgerTransaction(String idempotencyKey, String requestHash, TransactionType type, UUID reversesId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidLedgerTransactionException("The Idempotency Key must not be null or blank");
        }
        if (requestHash == null || requestHash.isBlank()) {
            throw new InvalidLedgerTransactionException("The Request Hash must not be null or blank");
        }
        if (type == null) {
            throw new InvalidLedgerTransactionException("The Transaction Type must not be null");
        }
        if (type == TransactionType.REVERSAL && reversesId == null) {
            throw new InvalidLedgerTransactionException(
                    "If the Transaction Type is REVERSAL, the Reverses ID can not be null. Received Transaction Type: %s. Received Reverses Id: %s"
                            .formatted(type, reversesId));
        }
        if (type != TransactionType.REVERSAL && reversesId != null) {
            throw new InvalidLedgerTransactionException(
                    "If the Transaction Type is DEPOSIT or TRANSFER, the Reverses ID must be null. Received Transaction Type: %s. Received Reverses Id: %s"
                            .formatted(type, reversesId));
        }
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.type = type;
        this.reversesId = reversesId;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public TransactionType getType() {
        return type;
    }

    public UUID getReversesId() {
        return reversesId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
