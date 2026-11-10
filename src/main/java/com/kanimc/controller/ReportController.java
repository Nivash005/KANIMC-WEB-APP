package com.kanimc.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import com.kanimc.repository.ChittaRepository;
import com.kanimc.repository.AccountTransactionRepository;
import com.kanimc.repository.AccountRepository;
import com.kanimc.repository.SalesBillRepository;
import com.kanimc.repository.SalesBillItemRepository;
import com.kanimc.entity.Chitta;
import com.kanimc.entity.SalesBill;
import com.kanimc.entity.Account;
import com.kanimc.service.AccountMappingService;
import java.util.TreeMap;


import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired private ChittaRepository chittaRepository;
    @Autowired private AccountTransactionRepository txnRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountMappingService accountMappingService;

    @Autowired private SalesBillRepository salesBillRepository;
    @Autowired private SalesBillItemRepository salesBillItemRepository;
    @Autowired private com.kanimc.repository.ChittaItemRepository chittaItemRepository;
    @Autowired private com.kanimc.repository.CashVoucherRepository cashVoucherRepository;
    @Autowired private com.kanimc.repository.ReceiptVoucherRepository receiptVoucherRepository;
    @Autowired private com.kanimc.repository.OpeningBalanceRepository openingBalanceRepository;
    @Autowired private com.kanimc.repository.PurchaseRepository purchaseRepository;
    @Autowired private com.kanimc.repository.PurchasePaymentRepository purchasePaymentRepository;
    @Autowired private com.kanimc.repository.PurchaseOpeningBalanceRepository purchaseOpeningBalanceRepository;

    @GetMapping("/cash-payment-register")
    public List<com.kanimc.entity.CashVoucher> getCashPaymentRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {
        return cashVoucherRepository.findFiltered(startDate, endDate, accountId);
    }

    @GetMapping("/chitta-register")
    public List<Map<String, Object>> getChittaRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {
        
        // Use optimized query with JOIN FETCH to avoid N+1 lazy loading
        List<Chitta> chittas = chittaRepository.findFilteredForRegister(startDate, endDate, accountId);
        if (chittas.isEmpty()) {
            return Collections.emptyList();
        }

        return chittas.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("chittaNo", c.getChittaNo());
            map.put("chittaDate", c.getChittaDate());
            String farmerName = c.getFarmerAccount() != null ? c.getFarmerAccount().getAccountName() : "Unknown";
            map.put("farmerName", farmerName);
            map.put("farmerAccount", c.getFarmerAccount() != null ? 
                Map.of("id", c.getFarmerAccount().getId(), "accountName", farmerName) : null);
            map.put("grossAmount", c.getGrossAmount() != null ? c.getGrossAmount() : BigDecimal.ZERO);
            map.put("commission", c.getCommissionDeduction() != null ? c.getCommissionDeduction() : BigDecimal.ZERO);
            map.put("commissionDeduction", c.getCommissionDeduction() != null ? c.getCommissionDeduction() : BigDecimal.ZERO);
            map.put("advance", c.getAdvanceAmount() != null ? c.getAdvanceAmount() : BigDecimal.ZERO);
            map.put("advanceAmount", c.getAdvanceAmount() != null ? c.getAdvanceAmount() : BigDecimal.ZERO);
            map.put("freight", c.getFreightAmount() != null ? c.getFreightAmount() : BigDecimal.ZERO);
            map.put("freightAmount", c.getFreightAmount() != null ? c.getFreightAmount() : BigDecimal.ZERO);
            map.put("netPayable", c.getNetPayable() != null ? c.getNetPayable() : BigDecimal.ZERO);
            map.put("paid", c.getPaid() != null ? c.getPaid() : false);
            map.put("payDate", c.getPayDate());
            map.put("paidTo", c.getPaidTo() != null ? c.getPaidTo() : "");
            // Calculate weight from already-fetched items (no extra query)
            BigDecimal weight = BigDecimal.ZERO;
            if (c.getItems() != null) {
                weight = c.getItems().stream()
                    .map(i -> i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            map.put("weight", weight);
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getChittaRegister(LocalDate startDate, LocalDate endDate) {
        return getChittaRegister(startDate, endDate, null);
    }

    @GetMapping("/chitta-payment-register")
    public List<Map<String, Object>> getChittaPaymentRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {
            
        List<com.kanimc.entity.AccountTransaction> txns = txnRepository.findChittaPayments(startDate, endDate, accountId);

        List<Map<String, Object>> list = new ArrayList<>();
        for (var t : txns) {
            Map<String, Object> map = new HashMap<>();
            map.put("paymentNo", t.getDocNo());
            map.put("date", t.getTransactionDate());
            map.put("payDate", t.getTransactionDate());
            map.put("chittaDate", t.getTransactionDate());
            String cNo = t.getDocNo() != null ? t.getDocNo().replace("CP-", "").replace("PAY-", "") : "";
            map.put("chittaNo", cNo);
            map.put("farmerName", t.getAccount() != null ? t.getAccount().getAccountName() : "Unknown");
            map.put("amount", t.getDebitAmount());
            map.put("grossAmount", t.getDebitAmount());
            map.put("commission", BigDecimal.ZERO);
            map.put("advance", BigDecimal.ZERO);
            map.put("freight", BigDecimal.ZERO);
            map.put("weight", BigDecimal.ZERO);
            map.put("total", t.getDebitAmount());
            map.put("netPayable", t.getDebitAmount());
            map.put("paidTo", t.getAccount() != null ? t.getAccount().getAccountName() : "");
            map.put("narration", t.getNarration());
            list.add(map);
        }

        Set<String> existingKeys = new HashSet<>();
        for (var m : list) {
            String mKey = (m.get("chittaNo") != null ? m.get("chittaNo").toString() : "") + "_" + (m.get("chittaDate") != null ? m.get("chittaDate").toString() : "");
            existingKeys.add(mKey);
        }

        List<Chitta> paidChittas = chittaRepository.findPaidFiltered(startDate, endDate, accountId);
        
        Map<Long, BigDecimal> weightMap = new HashMap<>();
        if (!paidChittas.isEmpty()) {
            List<Long> paidIds = paidChittas.stream().map(Chitta::getId).collect(Collectors.toList());
            // batch in chunks of 1000 if needed, or query direct
            for (int i = 0; i < paidIds.size(); i += 1000) {
                List<Long> subList = paidIds.subList(i, Math.min(i + 1000, paidIds.size()));
                List<Object[]> weightRows = chittaItemRepository.findTotalWeightByChittaIds(subList);
                for (Object[] row : weightRows) {
                    if (row != null && row.length >= 2 && row[0] != null) {
                        Long cId = (Long) row[0];
                        BigDecimal wt = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                        weightMap.put(cId, wt);
                    }
                }
            }
        }
            
        for (Chitta c : paidChittas) {
            String cKey = (c.getChittaNo() != null ? c.getChittaNo() : "") + "_" + (c.getChittaDate() != null ? c.getChittaDate().toString() : "");
            if (!existingKeys.contains(cKey)) {
                existingKeys.add(cKey);
                Map<String, Object> map = new HashMap<>();
                map.put("id", c.getId());
                map.put("paymentNo", "CP-" + c.getChittaNo());
                map.put("date", c.getPayDate() != null ? c.getPayDate() : c.getChittaDate());
                map.put("payDate", c.getPayDate() != null ? c.getPayDate() : c.getChittaDate());
                map.put("chittaDate", c.getChittaDate());
                map.put("chittaNo", c.getChittaNo());
                map.put("farmerName", c.getFarmerAccount() != null ? c.getFarmerAccount().getAccountName() : "Unknown");
                map.put("farmerAccount", c.getFarmerAccount());
                map.put("amount", c.getNetPayable() != null ? c.getNetPayable() : BigDecimal.ZERO);
                map.put("grossAmount", c.getGrossAmount() != null ? c.getGrossAmount() : BigDecimal.ZERO);
                map.put("commission", c.getCommissionDeduction() != null ? c.getCommissionDeduction() : BigDecimal.ZERO);
                map.put("advance", c.getAdvanceAmount() != null ? c.getAdvanceAmount() : BigDecimal.ZERO);
                map.put("freight", c.getFreightAmount() != null ? c.getFreightAmount() : BigDecimal.ZERO);
                map.put("weight", weightMap.getOrDefault(c.getId(), BigDecimal.ZERO));
                map.put("total", c.getNetPayable() != null ? c.getNetPayable() : BigDecimal.ZERO);
                map.put("netPayable", c.getNetPayable() != null ? c.getNetPayable() : BigDecimal.ZERO);
                map.put("paidTo", c.getPaidTo() != null ? c.getPaidTo() : "");
                map.put("narration", "Chitta Payment No: " + c.getChittaNo());
                list.add(map);
            }
        }
        
        return list;
    }

    public List<Map<String, Object>> getChittaPaymentRegister(LocalDate startDate, LocalDate endDate) {
        return getChittaPaymentRegister(startDate, endDate, null);
    }

    @GetMapping("/chitta-pending-register")
    public List<Map<String, Object>> getChittaPendingRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) Long accountId) {
            
        List<Map<String, Object>> pending = new ArrayList<>();
        
        List<Chitta> unpaidChittas = chittaRepository.findUnpaidFiltered(asOfDate, accountId);
        
        Map<Long, BigDecimal> weightMap = new HashMap<>();
        if (!unpaidChittas.isEmpty()) {
            List<Long> unpaidIds = unpaidChittas.stream().map(Chitta::getId).collect(Collectors.toList());
            for (int i = 0; i < unpaidIds.size(); i += 1000) {
                List<Long> subList = unpaidIds.subList(i, Math.min(i + 1000, unpaidIds.size()));
                List<Object[]> weightRows = chittaItemRepository.findTotalWeightByChittaIds(subList);
                for (Object[] row : weightRows) {
                    if (row != null && row.length >= 2 && row[0] != null) {
                        Long cId = (Long) row[0];
                        BigDecimal wt = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                        weightMap.put(cId, wt);
                    }
                }
            }
        }
                
        for (Chitta c : unpaidChittas) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("chittaNo", c.getChittaNo());
            map.put("chittaDate", c.getChittaDate());
            String fName = c.getFarmerAccount() != null ? c.getFarmerAccount().getAccountName() : "Unknown";
            map.put("farmerName", fName);
            map.put("farmerAccount", c.getFarmerAccount());
            map.put("grossAmount", c.getGrossAmount() != null ? c.getGrossAmount() : BigDecimal.ZERO);
            map.put("commission", c.getCommissionDeduction() != null ? c.getCommissionDeduction() : BigDecimal.ZERO);
            map.put("advance", c.getAdvanceAmount() != null ? c.getAdvanceAmount() : BigDecimal.ZERO);
            map.put("freight", c.getFreightAmount() != null ? c.getFreightAmount() : BigDecimal.ZERO);
            map.put("netPayable", c.getNetPayable() != null ? c.getNetPayable() : BigDecimal.ZERO);
            map.put("pendingAmount", c.getNetPayable() != null ? c.getNetPayable() : BigDecimal.ZERO);
            map.put("weight", weightMap.getOrDefault(c.getId(), BigDecimal.ZERO));
            pending.add(map);
        }
        
        return pending;
    }

    public List<Map<String, Object>> getChittaPendingRegister() {
        return getChittaPendingRegister(null, null);
    }

    @GetMapping("/bill-register")
    public List<Map<String, Object>> getBillRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {
        List<SalesBill> bills = salesBillRepository.findFiltered(startDate, endDate, accountId);
        return bills.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("billNo", b.getBillNo());
            map.put("billDate", b.getBillDate());
            map.put("buyerName", b.getBuyerAccount() != null ? b.getBuyerAccount().getAccountName() : "Unknown");
            map.put("buyerAccount", b.getBuyerAccount());
            map.put("grossAmount", b.getGrossAmount() != null ? b.getGrossAmount() : BigDecimal.ZERO);
            map.put("commission", b.getCommissionCharges() != null ? b.getCommissionCharges() : BigDecimal.ZERO);
            map.put("packing", b.getPackingCharges() != null ? b.getPackingCharges() : BigDecimal.ZERO);
            map.put("freight", b.getFreightCharges() != null ? b.getFreightCharges() : BigDecimal.ZERO);
            map.put("netReceivable", b.getNetReceivable() != null ? b.getNetReceivable() : BigDecimal.ZERO);
            
            BigDecimal weight = BigDecimal.ZERO;
            if (b.getItems() != null) {
                for (var item : b.getItems()) {
                    if (item.getQuantity() != null) weight = weight.add(item.getQuantity());
                }
            }
            map.put("weight", weight);
            map.put("items", b.getItems() != null ? b.getItems() : Collections.emptyList());
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getBillRegister(LocalDate startDate, LocalDate endDate) {
        return getBillRegister(startDate, endDate, null);
    }

    @GetMapping("/receipt-register")
    public List<Map<String, Object>> getReceiptRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {
        List<com.kanimc.entity.ReceiptVoucher> vouchers = receiptVoucherRepository.findFiltered(startDate, endDate, accountId);
        return vouchers.stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("voucherNo", v.getVoucherNo());
            map.put("voucherDate", v.getVoucherDate());
            map.put("accountName", v.getAccount() != null ? v.getAccount().getAccountName() : "Unknown");
            map.put("amount", v.getAmount());
            map.put("narration", v.getNarration());
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/ledger")
    public Map<String, Object> getLedger(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String accountCode,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Account acc = null;
        if (accountId != null) {
            acc = accountRepository.findById(accountId).orElse(null);
        }
        if (acc == null && accountCode != null && !accountCode.trim().isEmpty()) {
            String c = accountCode.trim();
            acc = accountRepository.findAll().stream()
                    .filter(a -> c.equalsIgnoreCase(String.valueOf(a.getId())) || 
                                 (a.getPartyId() != null && c.equalsIgnoreCase(a.getPartyId().trim())))
                    .findFirst().orElse(null);
        }
        if (acc == null && accountName != null && !accountName.trim().isEmpty()) {
            String n = accountName.trim();
            acc = accountRepository.findAll().stream()
                    .filter(a -> a.getAccountName() != null && n.equalsIgnoreCase(a.getAccountName().trim()))
                    .findFirst().orElse(null);
        }
        if (acc == null) {
            var all = accountRepository.findAll();
            if (!all.isEmpty()) acc = all.get(0);
        }

        if (acc == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("accountName", "—");
            empty.put("openingBalance", BigDecimal.ZERO);
            empty.put("transactions", Collections.emptyList());
            empty.put("totalDebit", BigDecimal.ZERO);
            empty.put("totalCredit", BigDecimal.ZERO);
            empty.put("closingBalance", BigDecimal.ZERO);
            return empty;
        }

        final Long targetAccId = acc.getId();
        List<com.kanimc.entity.AccountTransaction> allTxns = txnRepository.findByAccountIdOrderByTransactionDateAsc(targetAccId);
        if (allTxns == null) allTxns = new ArrayList<>();
        else allTxns = new ArrayList<>(allTxns);

        Set<String> existingKeys = new HashSet<>();
        for (com.kanimc.entity.AccountTransaction t : allTxns) {
            String key = (t.getDocCode() != null ? t.getDocCode() : "") + "_" + (t.getDocNo() != null ? t.getDocNo().trim() : "");
            existingKeys.add(key.toUpperCase());
        }

        // Merge any unposted or standalone transactions:
        // 1. Chittas
        try {
            List<Chitta> chittas = chittaRepository.findAll();
            for (Chitta c : chittas) {
                if (c.getFarmerAccount() != null && targetAccId.equals(c.getFarmerAccount().getId())) {
                    String docNoStr = String.valueOf(c.getChittaNo());
                    if (!existingKeys.contains("1_" + docNoStr) && !existingKeys.contains("CH_" + docNoStr) && !existingKeys.contains("CHITTA_" + docNoStr)) {
                        com.kanimc.entity.AccountTransaction t = new com.kanimc.entity.AccountTransaction();
                        t.setTransactionDate(c.getChittaDate() != null ? c.getChittaDate() : LocalDate.now());
                        t.setDocCode(1);
                        t.setDocNo("CH-" + docNoStr);
                        t.setNarration("CHITTA #" + docNoStr);
                        BigDecimal amt = c.getNetPayable() != null ? c.getNetPayable() : (c.getGrossAmount() != null ? c.getGrossAmount() : BigDecimal.ZERO);
                        t.setCreditAmount(amt);
                        t.setDebitAmount(BigDecimal.ZERO);
                        allTxns.add(t);
                    }
                }
            }
        } catch(Exception ignored) {}

        // 2. Sales Bills
        try {
            List<SalesBill> bills = salesBillRepository.findAll();
            for (SalesBill b : bills) {
                if (b.getBuyerAccount() != null && targetAccId.equals(b.getBuyerAccount().getId())) {
                    String docNoStr = String.valueOf(b.getBillNo());
                    if (!existingKeys.contains("3_" + docNoStr) && !existingKeys.contains("BL_" + docNoStr) && !existingKeys.contains("BILL_" + docNoStr)) {
                        com.kanimc.entity.AccountTransaction t = new com.kanimc.entity.AccountTransaction();
                        t.setTransactionDate(b.getBillDate() != null ? b.getBillDate() : LocalDate.now());
                        t.setDocCode(3);
                        t.setDocNo("BL-" + docNoStr);
                        t.setNarration("SALES BILL #" + docNoStr);
                        BigDecimal amt = b.getNetReceivable() != null ? b.getNetReceivable() : (b.getGrossAmount() != null ? b.getGrossAmount() : BigDecimal.ZERO);
                        t.setDebitAmount(amt);
                        t.setCreditAmount(BigDecimal.ZERO);
                        allTxns.add(t);
                    }
                }
            }
        } catch(Exception ignored) {}

        // 3. Cash Payments
        try {
            List<com.kanimc.entity.CashVoucher> cvs = cashVoucherRepository.findAll();
            for (com.kanimc.entity.CashVoucher cv : cvs) {
                if (cv.getAccount() != null && targetAccId.equals(cv.getAccount().getId())) {
                    String docNoStr = String.valueOf(cv.getVoucherNo());
                    if (!existingKeys.contains("2_" + docNoStr) && !existingKeys.contains("CP_" + docNoStr)) {
                        com.kanimc.entity.AccountTransaction t = new com.kanimc.entity.AccountTransaction();
                        t.setTransactionDate(cv.getVoucherDate() != null ? cv.getVoucherDate() : LocalDate.now());
                        t.setDocCode(2);
                        t.setDocNo("CP-" + docNoStr);
                        t.setNarration(cv.getNarration() != null && !cv.getNarration().isEmpty() ? cv.getNarration() : "CASH PAYMENT");
                        t.setDebitAmount(cv.getAmount() != null ? cv.getAmount() : BigDecimal.ZERO);
                        t.setCreditAmount(BigDecimal.ZERO);
                        allTxns.add(t);
                    }
                }
            }
        } catch(Exception ignored) {}

        // 4. Receipt Vouchers
        try {
            List<com.kanimc.entity.ReceiptVoucher> rvs = receiptVoucherRepository.findAll();
            for (com.kanimc.entity.ReceiptVoucher rv : rvs) {
                if (rv.getAccount() != null && targetAccId.equals(rv.getAccount().getId())) {
                    String docNoStr = String.valueOf(rv.getVoucherNo());
                    if (!existingKeys.contains("5_" + docNoStr) && !existingKeys.contains("RC_" + docNoStr)) {
                        com.kanimc.entity.AccountTransaction t = new com.kanimc.entity.AccountTransaction();
                        t.setTransactionDate(rv.getVoucherDate() != null ? rv.getVoucherDate() : LocalDate.now());
                        t.setDocCode(5);
                        t.setDocNo("RC-" + docNoStr);
                        t.setNarration(rv.getNarration() != null && !rv.getNarration().isEmpty() ? rv.getNarration() : "RECEIPT VOUCHER");
                        t.setCreditAmount(rv.getAmount() != null ? rv.getAmount() : BigDecimal.ZERO);
                        t.setDebitAmount(BigDecimal.ZERO);
                        allTxns.add(t);
                    }
                }
            }
        } catch(Exception ignored) {}

        // 5. Purchases
        try {
            List<com.kanimc.entity.Purchase> purs = purchaseRepository.findAll();
            for (com.kanimc.entity.Purchase p : purs) {
                if (p.getTraderAccount() != null && targetAccId.equals(p.getTraderAccount().getId())) {
                    String docNoStr = String.valueOf(p.getPurchaseNo());
                    if (!existingKeys.contains("6_" + docNoStr) && !existingKeys.contains("PUR_" + docNoStr)) {
                        com.kanimc.entity.AccountTransaction t = new com.kanimc.entity.AccountTransaction();
                        t.setTransactionDate(p.getPurchaseDate() != null ? p.getPurchaseDate() : LocalDate.now());
                        t.setDocCode(6);
                        t.setDocNo("PUR-" + docNoStr);
                        t.setNarration("PURCHASE #" + docNoStr);
                        BigDecimal amt = p.getNetPayable() != null ? p.getNetPayable() : (p.getGrossAmount() != null ? p.getGrossAmount() : BigDecimal.ZERO);
                        t.setCreditAmount(amt);
                        t.setDebitAmount(BigDecimal.ZERO);
                        allTxns.add(t);
                    }
                }
            }
        } catch(Exception ignored) {}

        // Sort all by transactionDate ascending
        allTxns.sort(Comparator.comparing(t -> (t.getTransactionDate() != null ? t.getTransactionDate() : LocalDate.MIN)));

        boolean isFarmer = Boolean.TRUE.equals(acc.getIsFarmer()) || 
            (acc.getAccountGroup() != null && acc.getAccountGroup().getGroupName() != null && 
             acc.getAccountGroup().getGroupName().toUpperCase().contains("FARMER"));
        
        BigDecimal opening = acc.getOpeningBalance() != null ? acc.getOpeningBalance() : BigDecimal.ZERO;
        if (opening.compareTo(BigDecimal.ZERO) == 0) {
            try {
                var obs = openingBalanceRepository.findByAccountId(targetAccId);
                if (obs != null && !obs.isEmpty() && obs.get(0).getAmount() != null) {
                    opening = obs.get(0).getAmount();
                }
            } catch(Exception ignored) {}
        }

        for (com.kanimc.entity.AccountTransaction t : allTxns) {
            if (startDate != null && t.getTransactionDate() != null && t.getTransactionDate().isBefore(startDate)) {
                BigDecimal dr = t.getDebitAmount() != null ? t.getDebitAmount() : BigDecimal.ZERO;
                BigDecimal cr = t.getCreditAmount() != null ? t.getCreditAmount() : BigDecimal.ZERO;
                if (isFarmer) {
                    opening = opening.add(cr).subtract(dr);
                } else {
                    opening = opening.add(dr).subtract(cr);
                }
            }
        }
        
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal running = opening;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        
        for (com.kanimc.entity.AccountTransaction t : allTxns) {
            LocalDate tDate = t.getTransactionDate() != null ? t.getTransactionDate() : LocalDate.now();
            if ((startDate == null || !tDate.isBefore(startDate)) &&
                (endDate == null || !tDate.isAfter(endDate))) {
                
                BigDecimal dr = t.getDebitAmount() != null ? t.getDebitAmount() : BigDecimal.ZERO;
                BigDecimal cr = t.getCreditAmount() != null ? t.getCreditAmount() : BigDecimal.ZERO;
                if (isFarmer) {
                    running = running.add(cr).subtract(dr);
                } else {
                    running = running.add(dr).subtract(cr);
                }
                totalDebit = totalDebit.add(dr);
                totalCredit = totalCredit.add(cr);
                
                Map<String, Object> row = new HashMap<>();
                row.put("date", tDate);
                row.put("docNo", t.getDocNo() != null ? t.getDocNo() : "");
                row.put("docCode", t.getDocCode() != null ? String.valueOf(t.getDocCode()) : "");
                row.put("narration", t.getNarration() != null ? t.getNarration() : "");
                row.put("debit", dr);
                row.put("credit", cr);
                row.put("balance", running);
                rows.add(row);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("accountId", acc.getId());
        result.put("accountCode", acc.getPartyId() != null ? acc.getPartyId() : String.valueOf(acc.getId()));
        result.put("accountName", acc.getAccountName());
        result.put("isFarmer", isFarmer);
        result.put("openingBalance", opening);
        result.put("transactions", rows);
        result.put("totalDebit", totalDebit);
        result.put("totalCredit", totalCredit);
        result.put("closingBalance", running);
        return result;
    }

    @GetMapping("/daybook")
    public List<Map<String, Object>> getDaybook(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDate sDate = startDate != null ? startDate : (date != null ? date : LocalDate.now());
        LocalDate eDate = endDate != null ? endDate : (date != null ? date : sDate);

        Account cashAccount = accountRepository.findAll().stream()
                .filter(a -> a.getAccountName() != null && "CASH".equalsIgnoreCase(a.getAccountName().trim()))
                .findFirst().orElse(null);
        if (cashAccount == null) {
            cashAccount = accountRepository.findById(360L).orElse(null);
        }

        BigDecimal opening = BigDecimal.ZERO;
        if (cashAccount != null) {
            if (cashAccount.getOpeningBalance() != null && cashAccount.getOpeningBalance().compareTo(BigDecimal.ZERO) != 0) {
                opening = cashAccount.getOpeningBalance();
            } else {
                var obList = openingBalanceRepository.findByAccountId(cashAccount.getId());
                if (obList != null && !obList.isEmpty() && obList.get(0).getAmount() != null) {
                    opening = obList.get(0).getAmount();
                }
            }
        }

        List<com.kanimc.entity.ReceiptVoucher> allReceipts = receiptVoucherRepository.findAll();
        for (var r : allReceipts) {
            if (r.getVoucherDate() != null && r.getVoucherDate().isBefore(sDate) && r.getAmount() != null) {
                opening = opening.add(r.getAmount());
            }
        }

        List<com.kanimc.entity.CashVoucher> allCashVouchers = cashVoucherRepository.findAll();
        for (var v : allCashVouchers) {
            if (v.getVoucherDate() != null && v.getVoucherDate().isBefore(sDate) && v.getAmount() != null) {
                opening = opening.subtract(v.getAmount());
            }
        }

        List<Chitta> allChittas = chittaRepository.findAll();
        for (var c : allChittas) {
            LocalDate cDate = c.getPayDate() != null ? c.getPayDate() : c.getChittaDate();
            if (cDate != null && cDate.isBefore(sDate)) {
                if (Boolean.TRUE.equals(c.getPaid()) || (c.getPaymentMethod() != null && "CASH".equalsIgnoreCase(c.getPaymentMethod())) || c.getPayDate() != null) {
                    BigDecimal amt = c.getNetPayable() != null ? c.getNetPayable() : (c.getGrossAmount() != null ? c.getGrossAmount() : BigDecimal.ZERO);
                    opening = opening.subtract(amt);
                }
            }
        }

        List<com.kanimc.entity.Purchase> allPurchases = purchaseRepository.findAll();
        for (var p : allPurchases) {
            LocalDate pDate = p.getPayDate() != null ? p.getPayDate() : p.getPurchaseDate();
            if (pDate != null && pDate.isBefore(sDate)) {
                BigDecimal amt = p.getNetPayable() != null ? p.getNetPayable() : (p.getGrossAmount() != null ? p.getGrossAmount() : BigDecimal.ZERO);
                opening = opening.subtract(amt);
            }
        }

        List<com.kanimc.entity.PurchasePayment> allPPs = purchasePaymentRepository.findAll();
        for (var pp : allPPs) {
            if (pp.getPaymentDate() != null && pp.getPaymentDate().isBefore(sDate)) {
                BigDecimal amt = pp.getAmount() != null ? pp.getAmount() : BigDecimal.ZERO;
                opening = opening.subtract(amt);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();

        Map<String, Object> opRow = new LinkedHashMap<>();
        opRow.put("date", "");
        opRow.put("name", "OPENING BALANCE");
        opRow.put("accountName", "OPENING BALANCE");
        opRow.put("credit", opening);
        opRow.put("debit", BigDecimal.ZERO);
        opRow.put("isOpening", true);
        result.add(opRow);

        // 1. CHITTA PAYMENT
        Map<LocalDate, BigDecimal> chittaPayByDate = new TreeMap<>();
        for (var c : allChittas) {
            LocalDate cDate = c.getPayDate() != null ? c.getPayDate() : c.getChittaDate();
            if (cDate != null && !cDate.isBefore(sDate) && !cDate.isAfter(eDate)) {
                if (Boolean.TRUE.equals(c.getPaid()) || (c.getPaymentMethod() != null && "CASH".equalsIgnoreCase(c.getPaymentMethod())) || c.getPayDate() != null) {
                    BigDecimal amt = c.getNetPayable() != null ? c.getNetPayable() : (c.getGrossAmount() != null ? c.getGrossAmount() : BigDecimal.ZERO);
                    chittaPayByDate.put(cDate, chittaPayByDate.getOrDefault(cDate, BigDecimal.ZERO).add(amt));
                }
            }
        }

        for (Map.Entry<LocalDate, BigDecimal> entry : chittaPayByDate.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("date", entry.getKey().toString());
                row.put("name", "CHITTA PAYMENT");
                row.put("accountName", "CHITTA PAYMENT");
                row.put("credit", BigDecimal.ZERO);
                row.put("debit", entry.getValue());
                result.add(row);
            }
        }

        // 2. PURCHASE A/C
        Map<LocalDate, BigDecimal> purchaseByDate = new TreeMap<>();
        for (var p : allPurchases) {
            LocalDate pDate = p.getPayDate() != null ? p.getPayDate() : p.getPurchaseDate();
            if (pDate != null && !pDate.isBefore(sDate) && !pDate.isAfter(eDate)) {
                BigDecimal amt = p.getNetPayable() != null ? p.getNetPayable() : (p.getGrossAmount() != null ? p.getGrossAmount() : BigDecimal.ZERO);
                purchaseByDate.put(pDate, purchaseByDate.getOrDefault(pDate, BigDecimal.ZERO).add(amt));
            }
        }
        for (var pp : allPPs) {
            if (pp.getPaymentDate() != null && !pp.getPaymentDate().isBefore(sDate) && !pp.getPaymentDate().isAfter(eDate)) {
                BigDecimal amt = pp.getAmount() != null ? pp.getAmount() : BigDecimal.ZERO;
                purchaseByDate.put(pp.getPaymentDate(), purchaseByDate.getOrDefault(pp.getPaymentDate(), BigDecimal.ZERO).add(amt));
            }
        }

        for (Map.Entry<LocalDate, BigDecimal> entry : purchaseByDate.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("date", entry.getKey().toString());
                row.put("name", "PURCHASE A/C");
                row.put("accountName", "PURCHASE A/C");
                row.put("credit", BigDecimal.ZERO);
                row.put("debit", entry.getValue());
                result.add(row);
            }
        }

        // 3. Receipts
        for (var r : allReceipts) {
            if (r.getVoucherDate() != null && !r.getVoucherDate().isBefore(sDate) && !r.getVoucherDate().isAfter(eDate)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("date", r.getVoucherDate().toString());
                String pName = r.getAccount() != null && r.getAccount().getAccountName() != null ? r.getAccount().getAccountName() : "RECEIPT";
                row.put("name", pName);
                row.put("accountName", pName);
                row.put("credit", r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO);
                row.put("debit", BigDecimal.ZERO);
                row.put("narration", r.getNarration());
                result.add(row);
            }
        }

        // 4. Cash Payments
        for (var v : allCashVouchers) {
            if (v.getVoucherDate() != null && !v.getVoucherDate().isBefore(sDate) && !v.getVoucherDate().isAfter(eDate)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("date", v.getVoucherDate().toString());
                String pName = v.getAccount() != null && v.getAccount().getAccountName() != null ? v.getAccount().getAccountName() : "CASH PAYMENT";
                row.put("name", pName);
                row.put("accountName", pName);
                row.put("credit", BigDecimal.ZERO);
                row.put("debit", v.getAmount() != null ? v.getAmount() : BigDecimal.ZERO);
                row.put("narration", v.getNarration());
                result.add(row);
            }
        }

        return result;
    }

    @GetMapping("/item-register")
    public List<Map<String, Object>> getItemRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long itemId) {
        
        List<com.kanimc.entity.SalesBillItem> items = salesBillItemRepository.findItemRegister(startDate, endDate, itemId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (items != null && !items.isEmpty()) {
            for (com.kanimc.entity.SalesBillItem i : items) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", i.getSalesBill() != null ? i.getSalesBill().getId() : null);
                map.put("billId", i.getSalesBill() != null ? i.getSalesBill().getId() : null);
                map.put("docType", "BILL");
                map.put("date", i.getSalesBill() != null ? i.getSalesBill().getBillDate() : null);
                map.put("docNo", i.getSalesBill() != null ? i.getSalesBill().getBillNo() : "");
                map.put("partyName", (i.getSalesBill() != null && i.getSalesBill().getBuyerAccount() != null) ? i.getSalesBill().getBuyerAccount().getAccountName() : "");
                map.put("itemName", i.getItem() != null ? (i.getItem().getDes() != null ? i.getItem().getDes() : i.getItem().getDest()) : "SEDI");
                map.put("quantity", i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO);
                map.put("rate", i.getRate() != null ? i.getRate() : BigDecimal.ZERO);
                map.put("amount", i.getAmount() != null ? i.getAmount() : (i.getQuantity() != null && i.getRate() != null ? i.getQuantity().multiply(i.getRate()) : BigDecimal.ZERO));
                result.add(map);
            }
        } else {
            // Fallback: If no sales bills, check chittas
            try {
                List<Chitta> chittas = chittaRepository.findFiltered(startDate, endDate, null);
                for (Chitta c : chittas) {
                    if (c.getItems() != null) {
                        for (com.kanimc.entity.ChittaItem ci : c.getItems()) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", c.getId());
                            map.put("chittaId", c.getId());
                            map.put("docType", "CHITTA");
                            map.put("date", c.getChittaDate());
                            map.put("docNo", c.getChittaNo());
                            map.put("partyName", c.getFarmerAccount() != null ? c.getFarmerAccount().getAccountName() : "");
                            map.put("itemName", ci.getItem() != null ? (ci.getItem().getDes() != null ? ci.getItem().getDes() : ci.getItem().getDest()) : "SEDI");
                            map.put("quantity", ci.getQuantity() != null ? ci.getQuantity() : BigDecimal.ZERO);
                            map.put("rate", ci.getRate() != null ? ci.getRate() : BigDecimal.ZERO);
                            map.put("amount", ci.getAmount() != null ? ci.getAmount() : (ci.getQuantity() != null && ci.getRate() != null ? ci.getQuantity().multiply(ci.getRate()) : BigDecimal.ZERO));
                            result.add(map);
                        }
                    }
                }
            } catch(Exception ignored) {}
        }
        return result;
    }

    @GetMapping("/item-summary")
    public List<Map<String, Object>> getItemSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<Object[]> rows = salesBillItemRepository.getItemSummary(startDate, endDate);
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (rows != null && !rows.isEmpty()) {
            for (Object[] r : rows) {
                Map<String, Object> map = new HashMap<>();
                map.put("itemName", r[0] != null ? r[0].toString() : "Unknown");
                map.put("totalQuantity", r[1] != null ? new BigDecimal(r[1].toString()) : BigDecimal.ZERO);
                map.put("totalAmount", r[2] != null ? new BigDecimal(r[2].toString()) : BigDecimal.ZERO);
                result.add(map);
            }
        } else {
            // Fallback: If no sales bills, aggregate from chitta items
            try {
                List<Chitta> chittas = chittaRepository.findFiltered(startDate, endDate, null);
                Map<String, BigDecimal[]> agg = new TreeMap<>();
                for (Chitta c : chittas) {
                    if (c.getItems() != null) {
                        for (com.kanimc.entity.ChittaItem ci : c.getItems()) {
                            String name = ci.getItem() != null ? (ci.getItem().getDes() != null ? ci.getItem().getDes() : ci.getItem().getDest()) : "SEDI";
                            BigDecimal qty = ci.getQuantity() != null ? ci.getQuantity() : BigDecimal.ZERO;
                            BigDecimal amt = ci.getAmount() != null ? ci.getAmount() : (qty.multiply(ci.getRate() != null ? ci.getRate() : BigDecimal.ZERO));
                            agg.computeIfAbsent(name, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                            agg.get(name)[0] = agg.get(name)[0].add(qty);
                            agg.get(name)[1] = agg.get(name)[1].add(amt);
                        }
                    }
                }
                for (Map.Entry<String, BigDecimal[]> entry : agg.entrySet()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("itemName", entry.getKey());
                    map.put("totalQuantity", entry.getValue()[0]);
                    map.put("totalAmount", entry.getValue()[1]);
                    result.add(map);
                }
            } catch(Exception ignored) {}
        }
        return result;
    }

    @GetMapping("/commission-register")
    public List<Map<String, Object>> getCommissionRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<Map<String, Object>> register = new ArrayList<>();
        
        // From Chittas (Purchases)
        List<Chitta> chittas = chittaRepository.findFiltered(startDate, endDate, null);
        for(Chitta c : chittas) {
            if(c.getCommissionDeduction() != null && c.getCommissionDeduction().compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> map = new HashMap<>();
                map.put("docNo", c.getChittaNo());
                map.put("date", c.getChittaDate());
                map.put("type", "Purchase");
                map.put("partyName", c.getFarmerAccount() != null ? c.getFarmerAccount().getAccountName() : "Unknown");
                map.put("grossAmount", c.getGrossAmount() != null ? c.getGrossAmount() : BigDecimal.ZERO);
                map.put("commission", c.getCommissionDeduction());
                register.add(map);
            }
        }
        
        // From Sales Bills (Sales)
        List<SalesBill> bills = salesBillRepository.findFiltered(startDate, endDate, null);
        for(SalesBill b : bills) {
            if(b.getCommissionCharges() != null && b.getCommissionCharges().compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> map = new HashMap<>();
                map.put("docNo", b.getBillNo());
                map.put("date", b.getBillDate());
                map.put("type", "Sale");
                map.put("partyName", b.getBuyerAccount() != null ? b.getBuyerAccount().getAccountName() : "Unknown");
                map.put("grossAmount", b.getGrossAmount() != null ? b.getGrossAmount() : BigDecimal.ZERO);
                map.put("commission", b.getCommissionCharges());
                register.add(map);
            }
        }
        
        register.sort((m1, m2) -> ((LocalDate)m1.get("date")).compareTo((LocalDate)m2.get("date")));
        return register;
    }

    @GetMapping("/commission-summary")
    public List<Map<String, Object>> getCommissionSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
            
        // Grouping by Party Name
        Map<String, BigDecimal> summaryMap = new HashMap<>();
        
        List<Chitta> chittas = chittaRepository.findFiltered(startDate, endDate, null);
        for(Chitta c : chittas) {
            if(c.getCommissionDeduction() != null && c.getCommissionDeduction().compareTo(BigDecimal.ZERO) > 0) {
                String party = c.getFarmerAccount() != null ? c.getFarmerAccount().getAccountName() : "Unknown";
                summaryMap.put(party, summaryMap.getOrDefault(party, BigDecimal.ZERO).add(c.getCommissionDeduction()));
            }
        }
        
        List<SalesBill> bills = salesBillRepository.findFiltered(startDate, endDate, null);
        for(SalesBill b : bills) {
            if(b.getCommissionCharges() != null && b.getCommissionCharges().compareTo(BigDecimal.ZERO) > 0) {
                String party = b.getBuyerAccount() != null ? b.getBuyerAccount().getAccountName() : "Unknown";
                summaryMap.put(party, summaryMap.getOrDefault(party, BigDecimal.ZERO).add(b.getCommissionCharges()));
            }
        }
        
        List<Map<String, Object>> summary = new ArrayList<>();
        for(Map.Entry<String, BigDecimal> entry : summaryMap.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("partyName", entry.getKey());
            map.put("totalCommission", entry.getValue());
            summary.add(map);
        }
        
        summary.sort((m1, m2) -> ((String)m1.get("partyName")).compareTo((String)m2.get("partyName")));
        return summary;
    }

        @GetMapping("/outstanding")
    public List<Map<String, Object>> getOutstanding(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        
        List<Account> accounts = accountRepository.findAll();
        List<com.kanimc.entity.AccountTransaction> allTxns = txnRepository.findAll();
        
        Map<Long, BigDecimal[]> txnTotals = new HashMap<>();
        for (com.kanimc.entity.AccountTransaction t : allTxns) {
            if (t.getAccount() != null && t.getAccount().getId() != null) {
                if (asOfDate == null || !t.getTransactionDate().isAfter(asOfDate)) {
                    Long accId = t.getAccount().getId();
                    BigDecimal[] totals = txnTotals.computeIfAbsent(accId, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                    if (t.getCreditAmount() != null) totals[0] = totals[0].add(t.getCreditAmount());
                    if (t.getDebitAmount() != null) totals[1] = totals[1].add(t.getDebitAmount());
                }
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Account a : accounts) {
            BigDecimal ob = a.getOpeningBalance() != null ? a.getOpeningBalance() : BigDecimal.ZERO;
            BigDecimal[] totals = txnTotals.get(a.getId());
            BigDecimal credits = totals != null ? totals[0] : BigDecimal.ZERO;
            BigDecimal debits = totals != null ? totals[1] : BigDecimal.ZERO;
            
            boolean isFarmer = Boolean.TRUE.equals(a.getIsFarmer()) || 
                (a.getAccountGroup() != null && a.getAccountGroup().getGroupName() != null && 
                 a.getAccountGroup().getGroupName().toUpperCase().contains("FARMER"));

            BigDecimal net;
            boolean isDr;
            if (isFarmer) {
                net = ob.add(credits).subtract(debits);
                isDr = net.compareTo(BigDecimal.ZERO) < 0;
            } else {
                net = ob.add(debits).subtract(credits);
                isDr = net.compareTo(BigDecimal.ZERO) >= 0;
            }
            
            if (net.compareTo(BigDecimal.ZERO) != 0) {
                Map<String, Object> map = new HashMap<>();
                map.put("accountId", a.getId());
                map.put("partyName", a.getAccountName());
                map.put("accountName", a.getAccountName());
                map.put("groupName", a.getAccountGroup() != null ? a.getAccountGroup().getGroupName() : "");
                map.put("balance", net.abs());
                map.put("closingBalance", isDr ? net.abs() : net.abs().negate());
                map.put("type", isDr ? "Dr" : "Cr");
                map.put("debit", isDr ? net.abs() : BigDecimal.ZERO);
                map.put("credit", !isDr ? net.abs() : BigDecimal.ZERO);
                map.put("netBalance", net.abs());
                map.put("isFarmer", isFarmer);
                map.put("isTrader", !isFarmer);
                result.add(map);
            }
        }
        
        result.sort((m1, m2) -> ((String)m1.get("partyName")).compareToIgnoreCase((String)m2.get("partyName")));
        return result;
    }

    @GetMapping("/opening-balance-register")
    public List<Map<String, Object>> getOpeningBalanceRegister(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String groupName) {
        List<com.kanimc.entity.OpeningBalance> obList = openingBalanceRepository.findAll();
        Map<Long, Map<String, Object>> mapByAccountId = new LinkedHashMap<>();
        
        for (com.kanimc.entity.OpeningBalance ob : obList) {
            if (ob.getDel() != null && ob.getDel().trim().equalsIgnoreCase("Y")) continue;
            Account a = ob.getAccount();
            if (a == null) continue;
            
            BigDecimal amount = ob.getAmount() != null ? ob.getAmount() : BigDecimal.ZERO;
            String atype = ob.getAtype() != null ? ob.getAtype().toUpperCase() : "DR";
            boolean isDebit = atype.startsWith("D");
            
            Map<String, Object> map = new HashMap<>();
            map.put("id", ob.getId());
            map.put("tno", ob.getTno());
            map.put("date", ob.getObDate());
            map.put("accountId", a.getId());
            map.put("partyId", a.getPartyId() != null ? a.getPartyId() : String.valueOf(a.getId()));
            map.put("partyName", a.getAccountName());
            map.put("accountName", a.getAccountName());
            map.put("groupName", a.getAccountGroup() != null ? a.getAccountGroup().getGroupName() : "");
            map.put("openingBalance", amount);
            map.put("type", isDebit ? "Dr" : "Cr");
            map.put("debit", isDebit ? amount : BigDecimal.ZERO);
            map.put("credit", !isDebit ? amount : BigDecimal.ZERO);
            map.put("netBalance", isDebit ? amount.negate() : amount);
            mapByAccountId.put(a.getId(), map);
        }

        // Also include any accounts that have an openingBalance on Account entity
        List<Account> accounts = accountRepository.findAll();
        for (Account a : accounts) {
            if (mapByAccountId.containsKey(a.getId())) continue;
            BigDecimal ob = a.getOpeningBalance() != null ? a.getOpeningBalance() : BigDecimal.ZERO;
            if (ob.compareTo(BigDecimal.ZERO) != 0) {
                boolean isDebit = ob.compareTo(BigDecimal.ZERO) > 0;
                BigDecimal amt = ob.abs();
                Map<String, Object> map = new HashMap<>();
                map.put("accountId", a.getId());
                map.put("partyId", a.getPartyId() != null ? a.getPartyId() : String.valueOf(a.getId()));
                map.put("partyName", a.getAccountName());
                map.put("accountName", a.getAccountName());
                map.put("groupName", a.getAccountGroup() != null ? a.getAccountGroup().getGroupName() : "");
                map.put("openingBalance", amt);
                map.put("type", isDebit ? "Dr" : "Cr");
                map.put("debit", isDebit ? amt : BigDecimal.ZERO);
                map.put("credit", !isDebit ? amt : BigDecimal.ZERO);
                map.put("netBalance", ob);
                mapByAccountId.put(a.getId(), map);
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>(mapByAccountId.values());
        if (accountId != null && !accountId.trim().isEmpty()) {
            String accStr = accountId.trim();
            result.removeIf(m -> {
                Object idObj = m.get("accountId");
                Object pidObj = m.get("partyId");
                boolean matchId = idObj != null && idObj.toString().equals(accStr);
                boolean matchPid = pidObj != null && pidObj.toString().equalsIgnoreCase(accStr);
                return !matchId && !matchPid;
            });
        }
        if (accountName != null && !accountName.trim().isEmpty()) {
            String q = accountName.trim().toLowerCase();
            result.removeIf(m -> m.get("partyName") == null || !((String)m.get("partyName")).toLowerCase().contains(q));
        }
        if (groupName != null && !groupName.trim().isEmpty()) {
            String g = groupName.trim().toLowerCase();
            result.removeIf(m -> m.get("groupName") == null || !((String)m.get("groupName")).toLowerCase().contains(g));
        }
        result.sort((m1, m2) -> ((String)m1.get("partyName")).compareToIgnoreCase((String)m2.get("partyName")));
        return result;
    }

            @GetMapping("/daily-cash-opening")
    public List<Map<String, Object>> getDailyCashOpening(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Long cashAccountId = accountMappingService.getAccountId("CASH", null);
        if (cashAccountId == null) {
            cashAccountId = accountRepository.findAll().stream()
                    .filter(a -> a.getAccountName() != null && a.getAccountName().trim().equalsIgnoreCase("CASH"))
                    .map(com.kanimc.entity.Account::getId)
                    .findFirst()
                    .orElse(null);
        }
        if (cashAccountId == null) {
            return java.util.Collections.emptyList();
        }
        
        Account cashAccount = accountRepository.findById(cashAccountId).orElse(null);
        if (cashAccount == null) {
            return java.util.Collections.emptyList();
        }
                
        BigDecimal baseOb = cashAccount.getOpeningBalance() != null ? cashAccount.getOpeningBalance() : BigDecimal.ZERO;
        
        List<com.kanimc.entity.AccountTransaction> txns = txnRepository.findByAccountIdOrderByTransactionDateAsc(cashAccountId);
        
        Map<LocalDate, List<com.kanimc.entity.AccountTransaction>> grouped = new TreeMap<>();
        for (com.kanimc.entity.AccountTransaction t : txns) {
            // Only consider non-deleted transactions that impact cash
            if (t.getDel() != null && t.getDel().trim().equalsIgnoreCase("Y")) continue;
            grouped.computeIfAbsent(t.getTransactionDate(), k -> new ArrayList<>()).add(t);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        BigDecimal runningBalance = baseOb;
        
        for (Map.Entry<LocalDate, List<com.kanimc.entity.AccountTransaction>> entry : grouped.entrySet()) {
            LocalDate date = entry.getKey();
            BigDecimal receipts = BigDecimal.ZERO;
            BigDecimal payments = BigDecimal.ZERO;
            
            for (com.kanimc.entity.AccountTransaction t : entry.getValue()) {
                if (t.getDebitAmount() != null) receipts = receipts.add(t.getDebitAmount());
                if (t.getCreditAmount() != null) payments = payments.add(t.getCreditAmount());
            }
            
            BigDecimal opening = runningBalance;
            BigDecimal closing = opening.add(receipts).subtract(payments);
            runningBalance = closing;
            
            if ((startDate == null || !date.isBefore(startDate)) && (endDate == null || !date.isAfter(endDate))) {
                Map<String, Object> map = new HashMap<>();
                map.put("date", date);
                map.put("openingBalance", opening);
                map.put("receipts", receipts);
                map.put("payments", payments);
                map.put("closingBalance", closing);
                result.add(map);
            }
        }

        if (result.isEmpty() && startDate != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("date", startDate);
            map.put("openingBalance", runningBalance);
            map.put("receipts", BigDecimal.ZERO);
            map.put("payments", BigDecimal.ZERO);
            map.put("closingBalance", runningBalance);
            result.add(map);
        }
        
        return result;
    }


    @GetMapping("/sales-register")
    public List<Map<String, Object>> getSalesRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<com.kanimc.entity.SalesBillItem> items = salesBillItemRepository.findSalesRegister(startDate, endDate);
        return items.stream().map(i -> {
            Map<String, Object> map = new HashMap<>();
            map.put("billDate", i.getSalesBill().getBillDate());
            map.put("billNo", i.getSalesBill().getBillNo());
            map.put("buyerName", i.getSalesBill().getBuyerAccount() != null ? i.getSalesBill().getBuyerAccount().getAccountName() : "Unknown");
            map.put("itemName", i.getItem().getDes());
            map.put("bags", i.getBags());
            map.put("quantity", i.getQuantity());
            map.put("rate", i.getRate());
            map.put("amount", i.getAmount());
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/{reportName}/export")
    public org.springframework.http.ResponseEntity<byte[]> exportReportCsv(
            @PathVariable String reportName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        StringBuilder csv = new StringBuilder();
        if ("chitta-register".equalsIgnoreCase(reportName)) {
            csv.append("Chitta No,Chitta Date,Farmer Name,Gross Amount,Commission,Advance,Freight,Net Payable\n");
            List<Map<String, Object>> list = getChittaRegister(startDate, endDate);
            for (Map<String, Object> r : list) {
                csv.append(String.format("\"%s\",\"%s\",\"%s\",%s,%s,%s,%s,%s\n",
                    r.get("chittaNo"), r.get("chittaDate"), r.get("farmerName"),
                    r.get("grossAmount"), r.get("commission"), r.get("advance"), r.get("freight"), r.get("netPayable")));
            }
        } else if ("sales-register".equalsIgnoreCase(reportName)) {
            csv.append("Bill Date,Bill No,Buyer Name,Item Name,Bags,Quantity,Rate,Amount\n");
            List<Map<String, Object>> list = getSalesRegister(startDate, endDate);
            for (Map<String, Object> r : list) {
                csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,%s,%s\n",
                    r.get("billDate"), r.get("billNo"), r.get("buyerName"), r.get("itemName"),
                    r.get("bags"), r.get("quantity"), r.get("rate"), r.get("amount")));
            }
        } else if ("chitta-payment-register".equalsIgnoreCase(reportName)) {
            csv.append("Payment No,Date,Farmer Name,Amount,Narration\n");
            List<Map<String, Object>> list = getChittaPaymentRegister(startDate, endDate);
            for (Map<String, Object> r : list) {
                csv.append(String.format("\"%s\",\"%s\",\"%s\",%s,\"%s\"\n",
                    r.get("paymentNo"), r.get("date"), r.get("farmerName"), r.get("amount"), r.get("narration")));
            }
        } else if ("cash-register".equalsIgnoreCase(reportName)) {
            csv.append("Date,Opening Balance,Receipts,Payments,Closing Balance\n");
            List<Map<String, Object>> list = getDailyCashOpening(startDate, endDate);
            for (Map<String, Object> r : list) {
                csv.append(String.format("\"%s\",%s,%s,%s,%s\n",
                    r.get("date"), r.get("openingBalance"), r.get("receipts"), r.get("payments"), r.get("closingBalance")));
            }
        } else {
            csv.append("Report: ").append(reportName).append("\n");
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return org.springframework.http.ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + reportName + ".csv\"")
            .contentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(bytes);
    }

    @GetMapping("/purchase-register")
    public List<Map<String, Object>> getPurchaseRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {
        List<com.kanimc.entity.Purchase> list = purchaseRepository.findFiltered(startDate, endDate, accountId);
        return list.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("purchaseNo", p.getPurchaseNo());
            map.put("purchaseDate", p.getPurchaseDate());
            map.put("traderName", p.getTraderAccount() != null ? p.getTraderAccount().getAccountName() : "Unknown");
            map.put("traderAccount", p.getTraderAccount());
            map.put("grossAmount", p.getGrossAmount() != null ? p.getGrossAmount() : BigDecimal.ZERO);
            map.put("freight", p.getFreightCharges() != null ? p.getFreightCharges() : BigDecimal.ZERO);
            map.put("packing", p.getPackingCharges() != null ? p.getPackingCharges() : BigDecimal.ZERO);
            map.put("other", p.getOtherCharges() != null ? p.getOtherCharges() : BigDecimal.ZERO);
            map.put("discount", p.getDiscount() != null ? p.getDiscount() : BigDecimal.ZERO);
            map.put("netPayable", p.getNetPayable() != null ? p.getNetPayable() : BigDecimal.ZERO);
            map.put("paid", Boolean.TRUE.equals(p.getPaid()));
            
            BigDecimal weight = BigDecimal.ZERO;
            BigDecimal bags = BigDecimal.ZERO;
            if (p.getItems() != null) {
                for (var item : p.getItems()) {
                    if (item.getQuantity() != null) weight = weight.add(item.getQuantity());
                    if (item.getBags() != null) bags = bags.add(item.getBags());
                }
            }
            map.put("weight", weight);
            map.put("bags", bags);
            map.put("items", p.getItems() != null ? p.getItems() : Collections.emptyList());
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/purchase-payment-register")
    public List<Map<String, Object>> getPurchasePaymentRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {
        List<com.kanimc.entity.PurchasePayment> list = purchasePaymentRepository.findFiltered(startDate, endDate, accountId);
        return list.stream().map(pp -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", pp.getId());
            map.put("paymentNo", pp.getPaymentNo());
            map.put("paymentDate", pp.getPaymentDate());
            map.put("traderName", pp.getTraderAccount() != null ? pp.getTraderAccount().getAccountName() : "Unknown");
            map.put("amount", pp.getAmount() != null ? pp.getAmount() : BigDecimal.ZERO);
            map.put("discount", pp.getDiscount() != null ? pp.getDiscount() : BigDecimal.ZERO);
            map.put("paymentMethod", pp.getPaymentMethod());
            map.put("narration", pp.getNarration());
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/purchase-pending-register")
    public List<Map<String, Object>> getPurchasePendingRegister(
            @RequestParam(required = false) Long accountId) {
        List<com.kanimc.entity.Purchase> list = purchaseRepository.findPending(accountId);
        return list.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("purchaseNo", p.getPurchaseNo());
            map.put("purchaseDate", p.getPurchaseDate());
            map.put("traderName", p.getTraderAccount() != null ? p.getTraderAccount().getAccountName() : "Unknown");
            map.put("traderAccount", p.getTraderAccount());
            BigDecimal net = p.getNetPayable() != null ? p.getNetPayable() : BigDecimal.ZERO;
            BigDecimal paidAmt = p.getPaidAmount() != null ? p.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal balance = net.subtract(paidAmt);
            map.put("netPayable", net);
            map.put("paidAmount", paidAmt);
            map.put("balance", balance.compareTo(BigDecimal.ZERO) > 0 ? balance : net);
            return map;
        }).collect(Collectors.toList());
    }

}
