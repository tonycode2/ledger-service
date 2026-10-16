package com.tonycode2.software.ledger_service.account.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tonycode2.software.ledger_service.account.dto.AccountResponse;
import com.tonycode2.software.ledger_service.account.dto.CreateAccountRequest;
import com.tonycode2.software.ledger_service.account.model.Account;
import com.tonycode2.software.ledger_service.account.model.enums.AccountType;
import com.tonycode2.software.ledger_service.account.repository.AccountRepository;
import com.tonycode2.software.ledger_service.common.exceptions.AccountNotFoundException;
import com.tonycode2.software.ledger_service.common.exceptions.IllegalPageablePropertiesException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidSortedByException;
import com.tonycode2.software.ledger_service.ledger.dto.EntryResponse;
import com.tonycode2.software.ledger_service.ledger.model.Entry;
import com.tonycode2.software.ledger_service.ledger.repository.EntryRepository;

@Service
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final EntryRepository entryRepository;

    public AccountServiceImpl(AccountRepository accountRepository, EntryRepository entryRepository) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest dto) {
        Account account = new Account(dto.owner(), dto.currency(), AccountType.CUSTOMER, false);
        Account savedEntity = accountRepository.save(account);
        return AccountResponse.from(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return AccountResponse.from(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntryResponse> getEntriesByAccountId(UUID id, int pageNumber, int pageSize, String sortedBy) {
        if (!List.of("id", "createdAt", "amount").contains(sortedBy)) {
            throw new InvalidSortedByException("The property you want to sort from is not available");
        }
        if (pageNumber < 0 || pageSize < 1) {
            throw new IllegalPageablePropertiesException("sortedBy must be one of: id, createdAt, amount");
        }

        accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));

        Sort sort = Sort.by(Sort.Direction.DESC, sortedBy);
        if (!sortedBy.equals("id")) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }
        Pageable pageable = PageRequest.of(pageNumber, Math.min(pageSize, 100), sort);
        Page<Entry> entryPage = entryRepository.findByAccount_Id(id, pageable);
        return entryPage.map(EntryResponse::from);
    }

}
