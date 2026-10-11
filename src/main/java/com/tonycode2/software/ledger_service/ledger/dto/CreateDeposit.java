package com.tonycode2.software.ledger_service.ledger.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateDeposit(
        @NotBlank UUID accountId,
        @Min(0) long amount,
        @NotBlank String currency) {

}
