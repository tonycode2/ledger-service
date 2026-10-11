package com.tonycode2.software.ledger_service.ledger.service;

import com.tonycode2.software.ledger_service.ledger.dto.CreateDeposit;
import com.tonycode2.software.ledger_service.ledger.dto.DepositResponse;

public interface DepositService {
    public DepositResponse deposit(CreateDeposit deposit, String idempotencyId);
}
