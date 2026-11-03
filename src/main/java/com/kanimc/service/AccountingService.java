package com.kanimc.service;

import com.kanimc.entity.Account;
import com.kanimc.entity.AccountTransaction;
import com.kanimc.entity.FinancialYear;
import com.kanimc.repository.AccountRepository;
import com.kanimc.repository.AccountTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AccountingService {

    private final AccountTransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public AccountingService(AccountTransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void postDoubleEntry(LocalDate date, Account debitAccount, Account creditAccount, 
                                BigDecimal amount, Integer docCode, String docNo, 
                                FinancialYear year, String narration) {
        if (date == null) date = LocalDate.now();
        if (amount == null) amount = BigDecimal.ZERO;

        // Debit Entry
        if (debitAccount != null) {
            AccountTransaction dr = new AccountTransaction();
            dr.setTransactionDate(date);
            dr.setAccount(debitAccount);
            dr.setDebitAmount(amount);
            dr.setDocCode(docCode);
            dr.setDocNo(docNo);
            dr.setFinancialYear(year);
            dr.setNarration(narration);
            transactionRepository.save(dr);
        }

        // Credit Entry
        if (creditAccount != null) {
            AccountTransaction cr = new AccountTransaction();
            cr.setTransactionDate(date);
            cr.setAccount(creditAccount);
            cr.setCreditAmount(amount);
            cr.setDocCode(docCode);
            cr.setDocNo(docNo);
            cr.setFinancialYear(year);
            cr.setNarration(narration);
            transactionRepository.save(cr);
        }
    }

    public List<AccountTransaction> getAccountStatement(Long accountId) {
        return transactionRepository.findByAccountIdOrderByTransactionDateAsc(accountId);
    }

    public List<AccountTransaction> getFilteredAccountStatement(LocalDate startDate, LocalDate endDate, Long accountId) {
        return transactionRepository.findFiltered(startDate, endDate, accountId);
    }

    public BigDecimal getAccountBalance(Long accountId) {
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        BigDecimal balance = acc.getOpeningBalance() != null ? acc.getOpeningBalance() : BigDecimal.ZERO;
        List<AccountTransaction> txns = transactionRepository.findByAccountIdOrderByTransactionDateAsc(accountId);
        if (txns != null) {
            boolean isDebtor = Boolean.TRUE.equals(acc.getIsTrader()) || !Boolean.TRUE.equals(acc.getIsFarmer());
            for (AccountTransaction t : txns) {
                BigDecimal dr = t.getDebitAmount() != null ? t.getDebitAmount() : BigDecimal.ZERO;
                BigDecimal cr = t.getCreditAmount() != null ? t.getCreditAmount() : BigDecimal.ZERO;
                if (isDebtor) {
                    balance = balance.add(dr).subtract(cr);
                } else {
                    balance = balance.add(cr).subtract(dr);
                }
            }
        }
        return balance;
    }
}
