package com.kanimc.service;

import com.kanimc.entity.Account;
import com.kanimc.entity.Chitta;
import com.kanimc.entity.ChittaItem;
import com.kanimc.repository.AccountRepository;
import com.kanimc.repository.ChittaItemRepository;
import com.kanimc.repository.ChittaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class ChittaService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ChittaRepository chittaRepository;
    private final ChittaItemRepository chittaItemRepository;
    private final AccountingService accountingService;
    private final AccountRepository accountRepository;

    public ChittaService(ChittaRepository chittaRepository, ChittaItemRepository chittaItemRepository, AccountingService accountingService, AccountRepository accountRepository) {
        this.chittaRepository = chittaRepository;
        this.chittaItemRepository = chittaItemRepository;
        this.accountingService = accountingService;
        this.accountRepository = accountRepository;
    }

    /**
     * Reproduces the core Chitta arithmetic visible in the recovered VFP data/report schema:
     * TOTAL - COMA - ADV - FRA = NET.
     * Commission entered by the UI is a percentage; commissionDeduction stores the calculated amount.
     */
    @Transactional
    public Chitta saveChitta(Chitta chitta, com.kanimc.entity.Account cashAccount, com.kanimc.entity.Account commissionAccount) {
        if (chitta.getChittaDate() == null) {
            throw new IllegalArgumentException("Chitta Date is required");
        }
        if (chitta.getFarmerAccount() == null || chitta.getFarmerAccount().getId() == null) {
            throw new IllegalArgumentException("Farmer account is required");
        }

        if (chitta.getChittaNo() == null || chitta.getChittaNo().trim().isEmpty() || "AUTO".equalsIgnoreCase(chitta.getChittaNo().trim())) {
            chitta.setChittaNo(getNextChittaNo(chitta.getChittaDate()));
        } else {
            chitta.setChittaNo(chitta.getChittaNo().trim());
        }

        var existing = chittaRepository.findByChittaDateAndChittaNo(chitta.getChittaDate(), chitta.getChittaNo());
        if (existing.isPresent() && !existing.get().getId().equals(chitta.getId())) {
            throw new IllegalArgumentException("Duplicate Chitta No: " + chitta.getChittaNo() + " on date " + chitta.getChittaDate());
        }

        BigDecimal gross = ZERO;
        if (chitta.getItems() != null) {
            for (ChittaItem item : chitta.getItems()) {
                if (item.getItem() == null || item.getItem().getId() == null) {
                    throw new IllegalArgumentException("Every Chitta row must have an item");
                }
                if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Quantity must be greater than zero");
                }
                if (item.getRate() == null || item.getRate().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Rate cannot be negative");
                }
                BigDecimal amount = item.getQuantity().multiply(item.getRate()).setScale(2, RoundingMode.HALF_UP);
                item.setAmount(amount);
                item.setChitta(chitta);
                gross = gross.add(amount);
            }
        }
        if (gross.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("At least one Chitta item is required");
        }

        BigDecimal rate = nz(chitta.getCommissionRate());
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Commission rate must be between 0 and 100");
        }
        BigDecimal commission = gross.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal advance = nz(chitta.getAdvanceAmount());
        BigDecimal freight = nz(chitta.getFreightAmount());
        if (advance.compareTo(BigDecimal.ZERO) < 0 || freight.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Advance and Freight cannot be negative");
        }

        BigDecimal net = gross.subtract(commission).subtract(advance).subtract(freight);
        if (net.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Deductions cannot exceed Chitta total");
        }

        chitta.setGrossAmount(gross.setScale(2, RoundingMode.HALF_UP));
        chitta.setCommissionRate(rate);
        chitta.setCommissionDeduction(commission);
        chitta.setAdvanceAmount(advance);
        chitta.setFreightAmount(freight);
        chitta.setNetPayable(net.setScale(2, RoundingMode.HALF_UP));

        Chitta saved = chittaRepository.save(chitta);

        // Keep accounting posting conservative until the exact VFP account-code mapping is recovered.
        // Only post when both configured accounts exist; never dereference hard-coded IDs.
        if (cashAccount != null && saved.getFarmerAccount() != null && net.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.postDoubleEntry(saved.getChittaDate(), saved.getFarmerAccount(), cashAccount,
                    net, 4, saved.getChittaNo(), saved.getFinancialYear(), "Chitta Payment");
        }
        if (commissionAccount != null && commission.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.postDoubleEntry(saved.getChittaDate(), commissionAccount, saved.getFarmerAccount(),
                    commission, 4, saved.getChittaNo(), saved.getFinancialYear(), "Chitta Commission");
        }
        return saved;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    public Iterable<Chitta> getAllChittas() { return chittaRepository.findAll(); }

    public Iterable<Chitta> getFilteredChittas(java.time.LocalDate startDate, java.time.LocalDate endDate, Long accountId) {
        return chittaRepository.findFiltered(startDate, endDate, accountId);
    }

    public java.util.Optional<Chitta> getChittaById(Long id) { return chittaRepository.findById(id); }

    @Transactional
    public Chitta updateChitta(Long id, Chitta incoming, com.kanimc.entity.Account cashAccount, com.kanimc.entity.Account commissionAccount) {
        Chitta existing = chittaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Chitta not found"));
        if (existing.getItems() != null && !existing.getItems().isEmpty()) {
            chittaItemRepository.deleteAll(existing.getItems());
            existing.getItems().clear();
            chittaRepository.saveAndFlush(existing);
        }
        incoming.setId(id);
        return saveChitta(incoming, cashAccount, commissionAccount);
    }

    @Transactional
    public void deleteChitta(Long id) {
        if (!chittaRepository.existsById(id)) throw new IllegalArgumentException("Chitta not found");
        chittaRepository.deleteById(id);
    }

    public String getNextChittaNo(java.time.LocalDate date) {
        if (date == null) date = java.time.LocalDate.now();
        java.util.List<String> existingNos = chittaRepository.findChittaNosByDate(date);
        long maxNo = 0;
        for (String no : existingNos) {
            if (no != null) {
                try {
                    long n = Long.parseLong(no.trim());
                    if (n > maxNo) maxNo = n;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.valueOf(maxNo + 1);
    }

    @Transactional
    public Chitta recordPayment(Long id, String paidTo, String paymentMethod, LocalDate payDate) {
        Chitta chitta = chittaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Chitta not found with ID: " + id));
        if (paidTo == null || paidTo.trim().isEmpty()) {
            throw new IllegalArgumentException("Paid to is required and cannot be empty.");
        }
        chitta.setPaid(true);
        chitta.setPayDate(payDate != null ? payDate : LocalDate.now());
        chitta.setPaidTo(paidTo.trim());
        chitta.setPaymentMethod((paymentMethod != null && !paymentMethod.trim().isEmpty()) ? paymentMethod.trim() : "CASH");

        Account cashAccount = accountRepository.findById(1L).orElse(null);
        if (cashAccount == null) {
            cashAccount = accountRepository.findByPartyId("1");
        }
        if (cashAccount == null) {
            for (Account acc : accountRepository.findAll()) {
                if (acc.getAccountName() != null && acc.getAccountName().toUpperCase().contains("CASH")) {
                    cashAccount = acc;
                    break;
                }
            }
        }

        if (cashAccount != null && chitta.getFarmerAccount() != null && chitta.getNetPayable() != null && chitta.getNetPayable().compareTo(BigDecimal.ZERO) > 0) {
            accountingService.postDoubleEntry(chitta.getPayDate(), chitta.getFarmerAccount(), cashAccount,
                    chitta.getNetPayable(), 4, "PAY-" + chitta.getChittaNo(), chitta.getFinancialYear(), "Chitta Settlement " + chitta.getChittaNo());
        }

        return chittaRepository.save(chitta);
    }

    @Transactional
    public Chitta clearPayment(Long id) {
        Chitta chitta = chittaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Chitta not found with ID: " + id));
        chitta.setPaid(false);
        chitta.setPayDate(null);
        chitta.setPaidTo(null);
        chitta.setPaymentMethod(null);
        chitta.setPayTno(null);
        return chittaRepository.save(chitta);
    }
}

