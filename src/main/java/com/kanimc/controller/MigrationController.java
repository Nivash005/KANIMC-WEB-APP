package com.kanimc.controller;

import com.kanimc.entity.*;
import com.kanimc.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/migrate")
@CrossOrigin(origins = "*")
public class MigrationController {

    @Autowired private ChittaRepository chittaRepository;
    @Autowired private SalesBillRepository salesBillRepository;
    @Autowired private CashVoucherRepository cashVoucherRepository;
    @Autowired private ReceiptVoucherRepository receiptVoucherRepository;
    @Autowired private AccountTransactionRepository accountTransactionRepository;
    @Autowired private AccountRepository accountRepository;

    @PostMapping("/chittas")
    public String migrateChittas(@RequestBody List<Chitta> chittas) {
        for (Chitta c : chittas) {
            if (c.getFarmerAccount() != null && c.getFarmerAccount().getId() != null) {
                c.setFarmerAccount(accountRepository.findById(c.getFarmerAccount().getId()).orElse(null));
            }
            if (c.getItems() != null) {
                for (ChittaItem item : c.getItems()) {
                    item.setChitta(c);
                }
            }
        }
        chittaRepository.saveAll(chittas);
        return "Migrated " + chittas.size() + " chittas successfully!";
    }

    @PostMapping("/sales")
    public String migrateSales(@RequestBody List<SalesBill> sales) {
        for (SalesBill s : sales) {
            if (s.getBuyerAccount() != null && s.getBuyerAccount().getId() != null) {
                s.setBuyerAccount(accountRepository.findById(s.getBuyerAccount().getId()).orElse(null));
            }
            if (s.getItems() != null) {
                for (SalesBillItem item : s.getItems()) {
                    item.setSalesBill(s);
                }
            }
        }
        salesBillRepository.saveAll(sales);
        return "Migrated " + sales.size() + " sales successfully!";
    }

    @PostMapping("/cash")
    public String migrateCash(@RequestBody List<CashVoucher> vouchers) {
        for (CashVoucher v : vouchers) {
            if (v.getAccount() != null && v.getAccount().getId() != null) {
                v.setAccount(accountRepository.findById(v.getAccount().getId()).orElse(null));
            }
        }
        cashVoucherRepository.saveAll(vouchers);
        return "Migrated " + vouchers.size() + " cash vouchers successfully!";
    }

    @PostMapping("/receipts")
    public String migrateReceipts(@RequestBody List<ReceiptVoucher> vouchers) {
        for (ReceiptVoucher v : vouchers) {
            if (v.getAccount() != null && v.getAccount().getId() != null) {
                v.setAccount(accountRepository.findById(v.getAccount().getId()).orElse(null));
            }
        }
        receiptVoucherRepository.saveAll(vouchers);
        return "Migrated " + vouchers.size() + " receipt vouchers successfully!";
    }

    @PostMapping("/ledger")
    public String migrateLedger(@RequestBody List<AccountTransaction> transactions) {
        for (AccountTransaction t : transactions) {
            if (t.getAccount() != null && t.getAccount().getId() != null) {
                t.setAccount(accountRepository.findById(t.getAccount().getId()).orElse(null));
            }
        }
        accountTransactionRepository.saveAll(transactions);
        return "Migrated " + transactions.size() + " ledger entries successfully!";
    }
}
