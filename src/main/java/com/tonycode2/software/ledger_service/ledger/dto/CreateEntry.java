package com.tonycode2.software.ledger_service.ledger.dto;

import com.tonycode2.software.ledger_service.account.model.Account;
import com.tonycode2.software.ledger_service.ledger.model.LedgerTransaction;
import com.tonycode2.software.ledger_service.ledger.model.enums.EntryDirection;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateEntry(
        @NotBlank LedgerTransaction transaction,
        @NotBlank Account account,
        @NotBlank EntryDirection direction,
        @Min(0) long amount) {

}
