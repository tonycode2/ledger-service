package com.tonycode2.software.ledger_service.transfer.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tonycode2.software.ledger_service.Utils.RequestHasher;
import com.tonycode2.software.ledger_service.account.model.Account;
import com.tonycode2.software.ledger_service.account.model.enums.AccountType;
import com.tonycode2.software.ledger_service.account.repository.AccountRepository;
import com.tonycode2.software.ledger_service.common.exceptions.AccountNotFoundException;
import com.tonycode2.software.ledger_service.common.exceptions.IdempotencyKeyReusedException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidCurrencyException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidTransferTypeException;
import com.tonycode2.software.ledger_service.common.exceptions.SameAccountTransferException;
import com.tonycode2.software.ledger_service.ledger.model.Entry;
import com.tonycode2.software.ledger_service.ledger.model.LedgerTransaction;
import com.tonycode2.software.ledger_service.ledger.model.enums.EntryDirection;
import com.tonycode2.software.ledger_service.ledger.model.enums.TransactionType;
import com.tonycode2.software.ledger_service.ledger.repository.EntryRepository;
import com.tonycode2.software.ledger_service.ledger.repository.LedgerTransactionRepository;
import com.tonycode2.software.ledger_service.transfer.dto.CreateTransfer;
import com.tonycode2.software.ledger_service.transfer.dto.TransferHash;
import com.tonycode2.software.ledger_service.transfer.dto.TransferResponse;

@Service
public class TransferServiceImpl implements TransferService {

    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final EntryRepository entryRepository;
    private final AccountRepository accountRepository;

    public TransferServiceImpl(LedgerTransactionRepository ledgerTransactionRepository,
            EntryRepository entryRepository, AccountRepository accountRepository) {
        this.ledgerTransactionRepository = ledgerTransactionRepository;
        this.entryRepository = entryRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    @Override
    public TransferResponse transfer(CreateTransfer transfer, String idempotencyId) {

        if (transfer.fromAccount().equals(transfer.toAccount())) {
            throw new SameAccountTransferException("Both accounts are the same");
        }
        TransferHash hash = new TransferHash(TransactionType.TRANSFER.toString(), transfer.fromAccount(),
                transfer.toAccount(),
                transfer.amount(), transfer.currency().toUpperCase());

        String requestHash = RequestHasher.requestHashHelper(hash);

        Optional<LedgerTransaction> ledgerTransactionSameIdempotency = ledgerTransactionRepository
                .findByIdempotencyKey(idempotencyId);

        if (ledgerTransactionSameIdempotency.isPresent()) {
            LedgerTransaction foundTransaction = ledgerTransactionSameIdempotency.get();
            if (foundTransaction.getRequestHash().equals(requestHash)) {
                return new TransferResponse(foundTransaction.getId(), transfer.fromAccount(), transfer.toAccount(),
                        TransactionType.TRANSFER.toString(), transfer.amount());
            }
            throw new IdempotencyKeyReusedException("The key has already been used with another content");
        }
        Optional<Account> fromAccountOptional = accountRepository.findById(transfer.fromAccount());
        Optional<Account> toAccountOptional = accountRepository.findById(transfer.toAccount());

        Account fromAccount = fromAccountOptional
                .orElseThrow(() -> new AccountNotFoundException(transfer.fromAccount()));
        Account toAccount = toAccountOptional
                .orElseThrow(() -> new AccountNotFoundException(transfer.toAccount()));

        if (!fromAccount.getCurrency().toUpperCase().equals(transfer.currency().toUpperCase())
                || !toAccount.getCurrency().toUpperCase().equals(transfer.currency().toUpperCase())) {
            throw new InvalidCurrencyException("The transfer currency does not match the accounts");
        }

        if (fromAccount.getType() == AccountType.SYSTEM || toAccount.getType() == AccountType.SYSTEM) {
            throw new InvalidTransferTypeException("This transfer is not allowed");
        }

        LedgerTransaction transaction = new LedgerTransaction(idempotencyId, requestHash, TransactionType.TRANSFER,
                null);
        LedgerTransaction savedTransaction = ledgerTransactionRepository.save(transaction);
        Entry fromEntry = new Entry(savedTransaction, fromAccount, EntryDirection.DEBIT, transfer.amount());
        fromAccount.debit(transfer.amount());
        Entry toEntry = new Entry(savedTransaction, toAccount, EntryDirection.CREDIT, transfer.amount());
        toAccount.credit(transfer.amount());

        entryRepository.saveAll(List.of(fromEntry, toEntry));

        return new TransferResponse(savedTransaction.getId(), fromAccount.getId(), toAccount.getId(),
                TransactionType.TRANSFER.toString(), transfer.amount());
    }

}
