package com.tonycode2.software.ledger_service.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAccountDto(
        @NotBlank String owner,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") @Schema(description = "This is the amount of cents on the account") String currency) {
}
