package com.kanimc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "purchase_payments")
public class PurchasePayment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_no", length = 50)
    private String paymentNo;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @ManyToOne
    @JoinColumn(name = "trader_account_id", nullable = false)
    private Account traderAccount;

    @ManyToOne
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "discount", precision = 14, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod = "CASH";

    @Column(name = "narration", length = 250)
    private String narration;

    public PurchasePayment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public Account getTraderAccount() { return traderAccount; }
    public void setTraderAccount(Account traderAccount) { this.traderAccount = traderAccount; }

    public Purchase getPurchase() { return purchase; }
    public void setPurchase(Purchase purchase) { this.purchase = purchase; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }
}
