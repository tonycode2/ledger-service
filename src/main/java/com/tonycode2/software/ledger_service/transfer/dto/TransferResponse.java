package com.tonycode2.software.ledger_service.transfer.dto;

import java.util.UUID;

public record TransferResponse(
        UUID transactionId,
        UUID fromAccount,
        UUID toAccount,
        String type,
        long amount) {

}
