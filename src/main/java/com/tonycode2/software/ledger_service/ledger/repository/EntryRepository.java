package com.tonycode2.software.ledger_service.ledger.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tonycode2.software.ledger_service.ledger.model.Entry;

public interface EntryRepository extends JpaRepository<Entry, Long> {
    Page<Entry> findByAccount_Id(UUID id, Pageable pageable);
}
