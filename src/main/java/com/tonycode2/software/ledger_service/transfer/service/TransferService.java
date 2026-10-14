package com.tonycode2.software.ledger_service.transfer.service;

import com.tonycode2.software.ledger_service.transfer.dto.CreateTransfer;
import com.tonycode2.software.ledger_service.transfer.dto.TransferResponse;

public interface TransferService {
    public TransferResponse transfer(CreateTransfer transfer, String idempotencyId);
}
