package com.tonycode2.software.ledger_service.ledger.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateDeposit(
        UUID accountId,
        @Positive @NotNull long amount,
        @NotBlank String currency) {
}
