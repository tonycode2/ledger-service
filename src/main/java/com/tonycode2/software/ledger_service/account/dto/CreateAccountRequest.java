package com.tonycode2.software.ledger_service.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(
                @NotBlank String owner,
                @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency) {
}
