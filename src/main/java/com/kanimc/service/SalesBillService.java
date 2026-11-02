package com.kanimc.service;

import com.kanimc.entity.SalesBill;
import com.kanimc.entity.SalesBillItem;
import com.kanimc.repository.SalesBillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class SalesBillService {

    private final SalesBillRepository salesBillRepository;
    private final AccountingService accountingService;

    public SalesBillService(SalesBillRepository salesBillRepository, AccountingService accountingService) {
        this.salesBillRepository = salesBillRepository;
        this.accountingService = accountingService;
    }

    @Transactional
    public SalesBill saveSalesBill(SalesBill bill, com.kanimc.entity.Account salesAccount) {
        if (bill.getBillDate() == null) {
            bill.setBillDate(LocalDate.now());
        }

        // 1. Calculate Gross Amount from items
        BigDecimal gross = BigDecimal.ZERO;
        if (bill.getItems() != null) {
            for (SalesBillItem item : bill.getItems()) {
                if (item.getBags() == null) item.setBags(0);
                if (item.getQuantity() == null) item.setQuantity(BigDecimal.ZERO);
                if (item.getRate() == null) item.setRate(BigDecimal.ZERO);
                BigDecimal amt = item.getQuantity().multiply(item.getRate());
                item.setAmount(amt);
                item.setSalesBill(bill);
                gross = gross.add(amt);
            }
        }
        bill.setGrossAmount(gross);

        // 2. Calculate Net Receivable = Gross + Charges - Discount
        BigDecimal comm = bill.getCommissionCharges() != null ? bill.getCommissionCharges() : BigDecimal.ZERO;
        BigDecimal pack = bill.getPackingCharges() != null ? bill.getPackingCharges() : BigDecimal.ZERO;
        BigDecimal frt = bill.getFreightCharges() != null ? bill.getFreightCharges() : BigDecimal.ZERO;
        BigDecimal other = bill.getOtherCharges() != null ? bill.getOtherCharges() : BigDecimal.ZERO;
        BigDecimal disc = bill.getDiscount() != null ? bill.getDiscount() : BigDecimal.ZERO;

        bill.setCommissionCharges(comm);
        bill.setPackingCharges(pack);
        bill.setFreightCharges(frt);
        bill.setOtherCharges(other);
        bill.setDiscount(disc);

        BigDecimal totalCharges = comm.add(pack).add(frt).add(other);
        BigDecimal netReceivable = gross.add(totalCharges).subtract(disc);
        bill.setNetReceivable(netReceivable);

        // 3. Save to DB
        SalesBill savedBill = salesBillRepository.save(bill);

        // 4. Post Double Entry (DocCode = 6)
        // Dr Buyer Account [Net Receivable]
        // Cr Sales Account [Net Receivable]
        if (savedBill.getBuyerAccount() != null && netReceivable.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.postDoubleEntry(
                savedBill.getBillDate(), 
                savedBill.getBuyerAccount(), // DR
                salesAccount,                // CR
                netReceivable, 6, savedBill.getBillNo(), savedBill.getFinancialYear(), "Sales Bill Invoice " + savedBill.getBillNo()
            );
        }

        return savedBill;
    }

    public Iterable<SalesBill> getAllSalesBills() {
        return salesBillRepository.findAll();
    }

    public Iterable<SalesBill> getFilteredSalesBills(java.time.LocalDate startDate, java.time.LocalDate endDate, Long accountId) {
        return salesBillRepository.findFiltered(startDate, endDate, accountId);
    }

    public java.util.Optional<SalesBill> getSalesBillById(Long id) {
        return salesBillRepository.findById(id);
    }

    @Transactional
    public void deleteSalesBill(Long id) {
        salesBillRepository.deleteById(id);
    }
}
