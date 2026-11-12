package com.kanimc.service;

import com.kanimc.entity.Account;
import com.kanimc.entity.OpeningBalance;
import com.kanimc.repository.AccountRepository;
import com.kanimc.repository.OpeningBalanceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class OpeningBalanceService {
    private final OpeningBalanceRepository openingBalanceRepository;
    private final AccountRepository accountRepository;
    private final JdbcTemplate jdbcTemplate;

    public OpeningBalanceService(OpeningBalanceRepository openingBalanceRepository,
                                 AccountRepository accountRepository,
                                 JdbcTemplate jdbcTemplate) {
        this.openingBalanceRepository = openingBalanceRepository;
        this.accountRepository = accountRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OpeningBalance> getAll() {
        return openingBalanceRepository.findAll();
    }

    public List<OpeningBalance> getByAccountId(Long accountId) {
        return openingBalanceRepository.findByAccountId(accountId);
    }

    @Transactional
    public void syncAccountsOpeningBalance() {
        jdbcTemplate.update(
            "UPDATE accounts a SET a.opening_balance = (" +
            "  SELECT CASE WHEN UPPER(o.atype) IN ('CR', 'CREDIT') THEN -o.amount ELSE o.amount END " +
            "  FROM s_obout o WHERE o.account_id = a.id ORDER BY o.ob_date DESC, o.id DESC LIMIT 1" +
            ") WHERE EXISTS (SELECT 1 FROM s_obout o WHERE o.account_id = a.id)"
        );
    }

    @Transactional
    public OpeningBalance saveOpeningBalance(OpeningBalance ob) {
        if (ob.getObDate() == null) {
            ob.setObDate(LocalDate.now());
        }
        if (ob.getAmount() == null) {
            ob.setAmount(BigDecimal.ZERO);
        }

        // Avoid duplicate opening balance records for the same account
        if (ob.getId() == null && ob.getAccount() != null && ob.getAccount().getId() != null) {
            List<OpeningBalance> existing = openingBalanceRepository.findByAccountId(ob.getAccount().getId());
            if (existing != null && !existing.isEmpty()) {
                ob.setId(existing.get(0).getId());
            }
        }

        if (ob.getTno() == null) {
            if (ob.getAccount() != null && ob.getAccount().getId() != null) {
                ob.setTno(ob.getAccount().getId());
            } else {
                ob.setTno(1L);
            }
        }

        String atype = ob.getAtype() != null ? ob.getAtype().toUpperCase() : "DR";
        boolean isDebit = atype.startsWith("D");
        ob.setAtype(isDebit ? "DR" : "CR");
        if (isDebit) {
            ob.setObdramt(ob.getAmount());
            ob.setObcramt(BigDecimal.ZERO);
        } else {
            ob.setObcramt(ob.getAmount());
            ob.setObdramt(BigDecimal.ZERO);
        }

        OpeningBalance saved = openingBalanceRepository.save(ob);

        // Update Account's opening balance (DR positive, CR negative)
        if (saved.getAccount() != null && saved.getAccount().getId() != null) {
            Account acc = accountRepository.findById(saved.getAccount().getId()).orElse(null);
            if (acc != null) {
                BigDecimal balanceValue = isDebit ? saved.getAmount() : saved.getAmount().negate();
                acc.setOpeningBalance(balanceValue);
                accountRepository.save(acc);
            }
        }
        return saved;
    }

    @Transactional
    public void deleteOpeningBalance(Long id) {
        OpeningBalance ob = openingBalanceRepository.findById(id).orElse(null);
        if (ob != null) {
            if (ob.getAccount() != null) {
                Account acc = accountRepository.findById(ob.getAccount().getId()).orElse(null);
                if (acc != null) {
                    acc.setOpeningBalance(BigDecimal.ZERO);
                    accountRepository.save(acc);
                }
            }
            openingBalanceRepository.deleteById(id);
        }
    }
}
