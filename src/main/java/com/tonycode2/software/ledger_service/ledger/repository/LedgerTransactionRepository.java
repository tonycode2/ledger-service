package com.tonycode2.software.ledger_service.ledger.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tonycode2.software.ledger_service.ledger.model.LedgerTransaction;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID> {

}
