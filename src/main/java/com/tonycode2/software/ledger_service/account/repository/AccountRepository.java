package com.tonycode2.software.ledger_service.account.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tonycode2.software.ledger_service.account.model.Account;

public interface AccountRepository extends JpaRepository<Account, UUID> {

}
