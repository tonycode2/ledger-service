package com.tonycode2.software.ledger_service.ledger.model;

import java.time.Instant;
import java.util.UUID;

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
    @Column(name = "idempotency_key")
    private String idempotencyKey;
    @Column(name = "request_hash")
    private String requestHash;
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    @Column(name = "reverses_id")
    private UUID reversesId;
    @Column(name = "created_at")
    private Instant createdAt;

    protected LedgerTransaction() {
    }

    public LedgerTransaction(String idempotencyKey, String requestHash, TransactionType type) {

    }
}
