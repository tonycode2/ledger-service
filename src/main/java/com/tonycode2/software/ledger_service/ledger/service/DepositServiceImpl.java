package com.tonycode2.software.ledger_service.ledger.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.hash.Hashing;
import com.tonycode2.software.ledger_service.account.model.Account;
import com.tonycode2.software.ledger_service.account.model.enums.AccountType;
import com.tonycode2.software.ledger_service.account.repository.AccountRepository;
import com.tonycode2.software.ledger_service.common.exceptions.AccountNotFoundException;
import com.tonycode2.software.ledger_service.ledger.dto.CreateDeposit;
import com.tonycode2.software.ledger_service.ledger.dto.DepositHash;
import com.tonycode2.software.ledger_service.ledger.dto.DepositResponse;
import com.tonycode2.software.ledger_service.ledger.model.LedgerTransaction;
import com.tonycode2.software.ledger_service.ledger.model.enums.TransactionType;
import com.tonycode2.software.ledger_service.ledger.repository.EntryRepository;
import com.tonycode2.software.ledger_service.ledger.repository.LedgerTransactionRepository;

import jakarta.transaction.Transactional;

@Service
public class DepositServiceImpl implements DepositService {

    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final EntryRepository entryRepository;
    private final AccountRepository accountRepository;

    public DepositServiceImpl(LedgerTransactionRepository ledgerTransactionRepository,
            EntryRepository entryRepository, AccountRepository accountRepository) {
        this.ledgerTransactionRepository = ledgerTransactionRepository;
        this.entryRepository = entryRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public DepositResponse deposit(CreateDeposit deposit, String idempotencyKey) throws JsonProcessingException {
        List<Account> externalAccountList = accountRepository.findByCurrencyAndType(deposit.currency(),
                AccountType.SYSTEM);
        Optional<Account> customerAccountOptional = accountRepository.findById(deposit.accountId());
        if (externalAccountList.size() == 0) {
            throw new AccountNotFoundException("The external account was not found");
        }
        if (!customerAccountOptional.isPresent()) {
            throw new AccountNotFoundException(deposit.accountId());
        }

        Account externalAccount = externalAccountList.get(0);
        Account customerAccount = customerAccountOptional.get();

        DepositHash hash = new DepositHash(externalAccount.getOwner(), customerAccount.getOwner(), deposit.amount(),
                deposit.currency());

        Optional<LedgerTransaction> ledgerTransactionWithSameIdempotency = ledgerTransactionRepository
                .findByIdempotencyKey(idempotencyKey);

        if (ledgerTransactionWithSameIdempotency.isPresent()) {
            LedgerTransaction foundTransaction = ledgerTransactionWithSameIdempotency.get();
            if (foundTransaction.getRequestHash() == hash) {
                return new DepositResponse(foundTransaction.getId(), foundTransaction.getType(),
                        customerAccount.getId(), 0, 0);
            }

        }

        LedgerTransaction transaction = new LedgerTransaction(idempotencyKey, requestHashHelper(hash),
                TransactionType.DEPOSIT, null);

    }

    private String requestHashHelper(DepositHash depositHash) throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(depositHash);
        return Hashing.sha256().hashString(json, StandardCharsets.UTF_8).toString();
    }

}
