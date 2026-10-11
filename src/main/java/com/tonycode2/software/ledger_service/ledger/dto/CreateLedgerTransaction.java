package com.tonycode2.software.ledger_service.ledger.dto;

import java.util.UUID;

import com.tonycode2.software.ledger_service.ledger.model.enums.TransactionType;

import jakarta.validation.constraints.NotBlank;

public record CreateLedgerTransaction(
        @NotBlank String idempotencyKey,
        @NotBlank String requestHash,
        @NotBlank TransactionType transactionType,
        UUID reversesId) {

}
