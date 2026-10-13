package com.tonycode2.software.ledger_service.transfer.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTransfer(
        UUID fromAccount,
        UUID toAccount,
        @Positive @NotNull long amount,
        @NotBlank String currency) {

}
