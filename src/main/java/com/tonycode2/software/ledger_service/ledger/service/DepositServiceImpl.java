package com.tonycode2.software.ledger_service.ledger.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tonycode2.software.ledger_service.account.model.Account;
import com.tonycode2.software.ledger_service.account.model.enums.AccountType;
import com.tonycode2.software.ledger_service.account.repository.AccountRepository;
import com.tonycode2.software.ledger_service.common.exceptions.AccountNotFoundException;
import com.tonycode2.software.ledger_service.common.exceptions.IdempotencyKeyReusedException;
import com.tonycode2.software.ledger_service.common.exceptions.InvalidCurrencyException;
import com.tonycode2.software.ledger_service.ledger.dto.CreateDeposit;
import com.tonycode2.software.ledger_service.ledger.dto.DepositHash;
import com.tonycode2.software.ledger_service.ledger.dto.DepositResponse;
import com.tonycode2.software.ledger_service.ledger.model.Entry;
import com.tonycode2.software.ledger_service.ledger.model.LedgerTransaction;
import com.tonycode2.software.ledger_service.ledger.model.enums.EntryDirection;
import com.tonycode2.software.ledger_service.ledger.model.enums.TransactionType;
import com.tonycode2.software.ledger_service.ledger.repository.EntryRepository;
import com.tonycode2.software.ledger_service.ledger.repository.LedgerTransactionRepository;

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
    @Override
    public DepositResponse deposit(CreateDeposit deposit, String idempotencyKey) {

        DepositHash hash = new DepositHash(TransactionType.DEPOSIT.toString(), deposit.accountId(), deposit.amount(),
                deposit.currency().toUpperCase());

        String requestHash = requestHashHelper(hash);
        Optional<LedgerTransaction> ledgerTransactionWithSameIdempotency = ledgerTransactionRepository
                .findByIdempotencyKey(idempotencyKey);

        if (ledgerTransactionWithSameIdempotency.isPresent()) {
            LedgerTransaction foundTransaction = ledgerTransactionWithSameIdempotency.get();
            if (foundTransaction.getRequestHash().equals(requestHash)) {
                return new DepositResponse(foundTransaction.getId(), foundTransaction.getType(),
                        deposit.accountId(), deposit.amount());
            }
            throw new IdempotencyKeyReusedException("The key has already been used with another content");
        }
        Optional<Account> customerAccountOptional = accountRepository.findById(deposit.accountId());

        if (customerAccountOptional.isEmpty()) {
            throw new AccountNotFoundException(deposit.accountId());
        }

        Account customerAccount = customerAccountOptional.get();
        if (!customerAccount.getCurrency().equals(deposit.currency().toUpperCase())) {
            throw new InvalidCurrencyException("The deposit currency is not the same as the account currency");
        }

        List<Account> externalAccountList = accountRepository.findByCurrencyAndType(deposit.currency(),
                AccountType.SYSTEM);
        if (externalAccountList.isEmpty()) {
            throw new AccountNotFoundException("The external account was not found");
        }
        Account externalAccount = externalAccountList.get(0);

        LedgerTransaction transaction = new LedgerTransaction(idempotencyKey, requestHash,
                TransactionType.DEPOSIT, null);
        Entry customerEntry = new Entry(transaction, customerAccount, EntryDirection.CREDIT, deposit.amount());
        customerAccount.credit(deposit.amount());
        Entry externalEntry = new Entry(transaction, externalAccount, EntryDirection.DEBIT, deposit.amount());
        externalAccount.debit(deposit.amount());

        LedgerTransaction savedTransaction = ledgerTransactionRepository.save(transaction);
        entryRepository.saveAll(List.of(customerEntry, externalEntry));

        return new DepositResponse(savedTransaction.getId(), savedTransaction.getType(),
                customerAccount.getId(), deposit.amount());

    }

    private String requestHashHelper(DepositHash h) {
        String canonical = String.join("|",
                TransactionType.DEPOSIT.toString(),
                h.accountId().toString(),
                String.valueOf(h.amount()),
                h.currency());

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

}
