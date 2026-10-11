package com.tonycode2.software.ledger_service.ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tonycode2.software.ledger_service.ledger.model.Entry;

public interface EntryRepository extends JpaRepository<Entry, Long> {

}
