package com.tonycode2.software.ledger_service.transfer.dto;

import java.util.UUID;

public record TransferHash(
        String operationType,
        UUID fromAccount,
        UUID toAccount,
        long amount,
        String currency) {

}
