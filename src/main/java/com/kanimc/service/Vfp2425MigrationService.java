package com.kanimc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class Vfp2425MigrationService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public Vfp2425MigrationService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> migrateAll() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            JsonNode root = loadExportJson();
            if (root == null) {
                stats.put("error", "vfp2425_export.json not found");
                return stats;
            }

            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

            // 1. Clear existing transactional / master data
            jdbcTemplate.execute("TRUNCATE TABLE chitta_items");
            jdbcTemplate.execute("TRUNCATE TABLE chittas");
            jdbcTemplate.execute("TRUNCATE TABLE sales_bill_items");
            jdbcTemplate.execute("TRUNCATE TABLE sales_bills");
            jdbcTemplate.execute("TRUNCATE TABLE cash_vouchers");
            jdbcTemplate.execute("TRUNCATE TABLE receipt_vouchers");
            jdbcTemplate.execute("TRUNCATE TABLE account_transactions");
            jdbcTemplate.execute("TRUNCATE TABLE s_obout");
            jdbcTemplate.execute("TRUNCATE TABLE accounts");
            jdbcTemplate.execute("TRUNCATE TABLE items");
            jdbcTemplate.execute("TRUNCATE TABLE places");
            jdbcTemplate.execute("TRUNCATE TABLE account_groups");
            jdbcTemplate.execute("TRUNCATE TABLE c_list");
            jdbcTemplate.execute("TRUNCATE TABLE sini");
            jdbcTemplate.execute("TRUNCATE TABLE s_rate");
            jdbcTemplate.execute("TRUNCATE TABLE financial_years");
            jdbcTemplate.execute("TRUNCATE TABLE financial_period");
            jdbcTemplate.execute("TRUNCATE TABLE account_mapping");

            // 2. Company
            JsonNode compNode = root.get("company");
            if (compNode != null) {
                jdbcTemplate.update(
                        "INSERT INTO c_list (id, comname, complace, comphone1, comphone2, comadd1, comadd2, comsn, del, crby, modby) VALUES (1, ?, ?, ?, ?, ?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                        compNode.path("comName").asText("SRI KANNIAMMAN TRADERS"),
                        compNode.path("comPlace").asText(""),
                        compNode.path("comPhone1").asText(""),
                        compNode.path("comPhone2").asText(""),
                        compNode.path("comAdd1").asText(""),
                        compNode.path("comAdd2").asText(""),
                        compNode.path("comSn").asText("")
                );
                stats.put("company", 1);
            }

            // 3. Settings
            JsonNode setNode = root.get("settings");
            if (setNode != null) {
                jdbcTemplate.update(
                        "INSERT INTO sini (id, zbpfile, zbillpfile, ziscom, zslreg, zchittano, zbillno, mcustagcode, mfaragcode, del, crby, modby) VALUES (1, ?, ?, false, ?, 0, 0, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                        setNode.path("zbpfile").asText("CPMAMNEW"),
                        setNode.path("billpfile").asText("BPMAMNEW"),
                        setNode.path("zslreg").asText(""),
                        setNode.path("custagcode").asInt(2),
                        setNode.path("faragcode").asInt(1)
                );
                stats.put("settings", 1);
            }

            // 4. Commission Rate
            JsonNode rateNode = root.get("commissionRate");
            if (rateNode != null) {
                jdbcTemplate.update(
                        "INSERT INTO s_rate (id, rateid, wefdate, comp, del, crby, modby) VALUES (1, 1, '2014-04-01', ?, '   ', 'ADMIN', 'ADMIN')",
                        BigDecimal.valueOf(rateNode.path("comp").asDouble(10.0))
                );
                stats.put("commissionRate", 1);
            }

            // 5. Financial Years
            JsonNode fyNodes = root.get("financialYears");
            int fyCount = 0;
            Long activeFyId = 10L;
            Map<String, Long> fyMap = new HashMap<>();
            if (fyNodes != null && fyNodes.isArray()) {
                long fid = 1;
                for (JsonNode fy : fyNodes) {
                    boolean isActive = fy.path("isActive").asBoolean(false);
                    if (isActive) activeFyId = fid;
                    String yCode = fy.path("yearCode").asText("");
                    fyMap.put(yCode, fid);
                    jdbcTemplate.update(
                            "INSERT INTO financial_years (id, year_code, start_date, end_date, is_active, del, crby, modby) VALUES (?, ?, ?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                            fid,
                            yCode,
                            LocalDate.parse(fy.path("startDate").asText("2024-04-26")),
                            LocalDate.parse(fy.path("endDate").asText("2025-04-25")),
                            isActive
                    );
                    jdbcTemplate.update(
                            "INSERT INTO financial_period (id, period_name, start_date, end_date, is_closed) VALUES (?, ?, ?, ?, false)",
                            fid,
                            fy.path("yearName").asText(yCode),
                            LocalDate.parse(fy.path("startDate").asText("2024-04-26")),
                            LocalDate.parse(fy.path("endDate").asText("2025-04-25"))
                    );
                    fid++;
                    fyCount++;
                }
            }
            stats.put("financialYears", fyCount);

            // 6. Account Groups
            JsonNode agNodes = root.get("accountGroups");
            int agCount = 0;
            if (agNodes != null && agNodes.isArray()) {
                jdbcTemplate.execute("ALTER TABLE account_groups ALTER COLUMN agtype VARCHAR(50)");
                for (JsonNode ag : agNodes) {
                    int code = ag.path("groupCode").asInt();
                    jdbcTemplate.update(
                            "INSERT INTO account_groups (id, group_code, group_name, agtype, agout, area, sltax, del, crby, modby) VALUES (?, ?, ?, ?, ?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                            (long) code,
                            code,
                            ag.path("groupName").asText(""),
                            ag.path("agtype").asText(""),
                            BigDecimal.ZERO,
                            ag.path("area").asText(""),
                            BigDecimal.ZERO
                    );
                    agCount++;
                }
            }
            stats.put("accountGroups", agCount);

            // 7. Places
            JsonNode plNodes = root.get("places");
            int plCount = 0;
            if (plNodes != null && plNodes.isArray()) {
                long pid = 1;
                for (JsonNode pl : plNodes) {
                    jdbcTemplate.update(
                            "INSERT INTO places (id, place_code, place_name, del, crby, modby) VALUES (?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                            pid++,
                            pl.path("placeCode").asText(""),
                            pl.path("placeName").asText("")
                    );
                    plCount++;
                }
            }
            stats.put("places", plCount);

            // 8. Items
            JsonNode itNodes = root.get("items");
            int itCount = 0;
            if (itNodes != null && itNodes.isArray()) {
                for (JsonNode it : itNodes) {
                    int icode = it.path("icode").asInt();
                    Integer smsNo = it.hasNonNull("smsNo") ? it.path("smsNo").asInt() : null;
                    jdbcTemplate.update(
                            "INSERT INTO items (id, icode, des, dest, sms_no, del, crby, modby) VALUES (?, ?, ?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                            (long) icode,
                            icode,
                            it.path("des").asText(""),
                            it.path("dest").asText(""),
                            smsNo
                    );
                    itCount++;
                }
            }
            stats.put("items", itCount);

            // 9. Accounts (all 2,463)
            JsonNode accNodes = root.get("accounts");
            int accCount = 0;
            if (accNodes != null && accNodes.isArray()) {
                List<Object[]> accBatch = new ArrayList<>();
                for (JsonNode acc : accNodes) {
                    accBatch.add(new Object[]{
                            acc.path("id").asLong(),
                            acc.path("partyId").asText(""),
                            acc.path("accountName").asText(""),
                            acc.path("regionalName").asText(""),
                            acc.path("groupCode").asInt(),
                            acc.path("phone1").asText(""),
                            acc.path("isFarmer").asBoolean(false),
                            acc.path("isTrader").asBoolean(false),
                            BigDecimal.valueOf(acc.path("openingBalance").asDouble(0.0))
                    });
                }
                jdbcTemplate.batchUpdate(
                        "INSERT INTO accounts (id, party_id, account_name, regional_name, group_code, phone1, is_farmer, is_trader, opening_balance, del, crby, modby) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                        accBatch
                );
                accCount = accBatch.size();
            }
            stats.put("accounts", accCount);

            // 10. Opening Balances (156 from 2425 + 144 from 2324 = 300)
            int obCount = 0;
            List<Object[]> obBatch = new ArrayList<>();
            long obid = 1;

            // 2324 Opening Balances
            JsonNode ob23Nodes = root.get("openingBalances_2324");
            if (ob23Nodes != null && ob23Nodes.isArray()) {
                for (JsonNode ob : ob23Nodes) {
                    obBatch.add(new Object[]{
                            obid++,
                            ob.path("tno").asLong(),
                            LocalDate.parse(ob.path("obDate").asText("2023-04-26")),
                            ob.path("accountId").asLong(),
                            BigDecimal.valueOf(ob.path("amount").asDouble(0.0)),
                            ob.path("atype").asText("DR"),
                            BigDecimal.valueOf(ob.path("obdramt").asDouble(0.0)),
                            BigDecimal.valueOf(ob.path("obcramt").asDouble(0.0))
                    });
                }
            }

            // 2425 Opening Balances
            JsonNode obNodes = root.get("openingBalances");
            if (obNodes != null && obNodes.isArray()) {
                for (JsonNode ob : obNodes) {
                    obBatch.add(new Object[]{
                            obid++,
                            ob.path("tno").asLong(),
                            LocalDate.parse(ob.path("obDate").asText("2024-04-26")),
                            ob.path("accountId").asLong(),
                            BigDecimal.valueOf(ob.path("amount").asDouble(0.0)),
                            ob.path("atype").asText("DR"),
                            BigDecimal.valueOf(ob.path("obdramt").asDouble(0.0)),
                            BigDecimal.valueOf(ob.path("obcramt").asDouble(0.0))
                    });
                }
            }

            if (!obBatch.isEmpty()) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO s_obout (id, tno, ob_date, account_id, amount, atype, obdramt, obcramt, del, crby, modby) VALUES (?, ?, ?, ?, ?, ?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                        obBatch
                );
                obCount = obBatch.size();

                // Synchronize accounts.opening_balance (DR positive, CR negative)
                // First apply 23-24 balances
                jdbcTemplate.update(
                        "UPDATE accounts a SET a.opening_balance = (" +
                        "  SELECT CASE WHEN UPPER(o.atype) IN ('CR', 'CREDIT') THEN -o.amount ELSE o.amount END " +
                        "  FROM s_obout o WHERE o.account_id = a.id AND o.ob_date = '2023-04-26'" +
                        ") WHERE EXISTS (SELECT 1 FROM s_obout o WHERE o.account_id = a.id AND o.ob_date = '2023-04-26')"
                );
                // Then apply 24-25 balances (latest FY overrides)
                jdbcTemplate.update(
                        "UPDATE accounts a SET a.opening_balance = (" +
                        "  SELECT CASE WHEN UPPER(o.atype) IN ('CR', 'CREDIT') THEN -o.amount ELSE o.amount END " +
                        "  FROM s_obout o WHERE o.account_id = a.id AND o.ob_date = '2024-04-26'" +
                        ") WHERE EXISTS (SELECT 1 FROM s_obout o WHERE o.account_id = a.id AND o.ob_date = '2024-04-26')"
                );
            }
            stats.put("openingBalances", obCount);

            // 11. Account Transactions (3,834 from 2324 + 158 from 2425 = 3,992)
            int atCount = 0;
            List<Object[]> atBatch = new ArrayList<>();
            long atid = 1;

            JsonNode at23Nodes = root.get("accountTransactions_2324");
            if (at23Nodes != null && at23Nodes.isArray()) {
                Long fy2324Id = fyMap.getOrDefault("2324", 9L);
                for (JsonNode at : at23Nodes) {
                    atBatch.add(new Object[]{
                            atid++,
                            LocalDate.parse(at.path("transactionDate").asText("2023-04-26")),
                            at.path("accountId").asLong(),
                            at.path("docCode").asInt(0),
                            at.path("docNo").asText(""),
                            BigDecimal.valueOf(at.path("debitAmount").asDouble(0.0)),
                            BigDecimal.valueOf(at.path("creditAmount").asDouble(0.0)),
                            at.path("narration").asText(""),
                            fy2324Id,
                            at.path("paymentMethod").asText("CASH")
                    });
                }
            }

            JsonNode atNodes = root.get("accountTransactions");
            if (atNodes != null && atNodes.isArray()) {
                Long fy2425Id = fyMap.getOrDefault("2425", activeFyId);
                for (JsonNode at : atNodes) {
                    atBatch.add(new Object[]{
                            atid++,
                            LocalDate.parse(at.path("transactionDate").asText("2024-04-26")),
                            at.path("accountId").asLong(),
                            at.path("docCode").asInt(0),
                            at.path("docNo").asText(""),
                            BigDecimal.valueOf(at.path("debitAmount").asDouble(0.0)),
                            BigDecimal.valueOf(at.path("creditAmount").asDouble(0.0)),
                            at.path("narration").asText(""),
                            fy2425Id,
                            at.path("paymentMethod").asText("CASH")
                    });
                }
            }

            if (!atBatch.isEmpty()) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO account_transactions (id, transaction_date, account_id, doc_code, doc_no, debit_amount, credit_amount, narration, financial_year_id, payment_method, del, crby, modby) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                        atBatch
                );
                atCount = atBatch.size();
            }
            stats.put("accountTransactions", atCount);

            // 12. Chittas (5,928 from 2324 + 3 newly created in 2425 = 5,931) and Items (6,618 + 3 = 6,621)
            int chCount = 0;
            int chItemCount = 0;
            List<Object[]> chBatch = new ArrayList<>();
            List<Object[]> itemBatch = new ArrayList<>();
            long chItemId = 1;
            Set<Long> processedTnos = new HashSet<>();

            Long fy2324 = fyMap.getOrDefault("2324", 9L);
            JsonNode ch23Nodes = root.get("chittas_2324");
            if (ch23Nodes != null && ch23Nodes.isArray()) {
                for (JsonNode ch : ch23Nodes) {
                    long tno = ch.path("tno").asLong();
                    processedTnos.add(tno);
                    LocalDate chDate = LocalDate.parse(ch.path("chittaDate").asText("2023-04-26"));
                    LocalDate payDate = ch.hasNonNull("payDate") && !ch.path("payDate").asText("").isEmpty()
                            ? LocalDate.parse(ch.path("payDate").asText())
                            : null;
                    Long payTno = ch.hasNonNull("payTno") && ch.path("payTno").asLong(0) > 0
                            ? ch.path("payTno").asLong()
                            : null;

                    chBatch.add(new Object[]{
                            tno,
                            ch.path("chittaNo").asText(""),
                            chDate,
                            ch.path("farmerAccountId").asLong(),
                            fy2324,
                            BigDecimal.valueOf(ch.path("grossAmount").asDouble(0.0)),
                            BigDecimal.valueOf(ch.path("commissionDeduction").asDouble(0.0)),
                            BigDecimal.valueOf(ch.path("advanceAmount").asDouble(0.0)),
                            BigDecimal.valueOf(ch.path("freightAmount").asDouble(0.0)),
                            BigDecimal.valueOf(ch.path("netPayable").asDouble(0.0)),
                            ch.path("paid").asBoolean(false),
                            payDate,
                            ch.path("paidTo").asText(""),
                            payTno,
                            ch.path("paymentMethod").asText("CASH")
                    });

                    JsonNode items = ch.get("items");
                    if (items != null && items.isArray()) {
                        for (JsonNode it : items) {
                            itemBatch.add(new Object[]{
                                    chItemId++,
                                    tno,
                                    it.path("itemId").asLong(),
                                    BigDecimal.valueOf(it.path("quantity").asDouble(0.0)),
                                    BigDecimal.valueOf(it.path("rate").asDouble(0.0)),
                                    BigDecimal.valueOf(it.path("amount").asDouble(0.0))
                            });
                        }
                    }
                }
            }

            Long fy2425 = fyMap.getOrDefault("2425", activeFyId);
            JsonNode chNodes = root.get("chittas");
            if (chNodes != null && chNodes.isArray()) {
                for (JsonNode ch : chNodes) {
                    long tno = ch.path("tno").asLong();
                    if (processedTnos.contains(tno)) {
                        continue; // Already migrated from 2324
                    }
                    processedTnos.add(tno);

                    LocalDate chDate = LocalDate.parse(ch.path("chittaDate").asText("2024-04-26"));
                    LocalDate payDate = ch.hasNonNull("payDate") && !ch.path("payDate").asText("").isEmpty()
                            ? LocalDate.parse(ch.path("payDate").asText())
                            : null;
                    Long payTno = ch.hasNonNull("payTno") && ch.path("payTno").asLong(0) > 0
                            ? ch.path("payTno").asLong()
                            : null;

                    chBatch.add(new Object[]{
                            tno,
                            ch.path("chittaNo").asText(""),
                            chDate,
                            ch.path("farmerAccountId").asLong(),
                            fy2425,
                            BigDecimal.valueOf(ch.path("grossAmount").asDouble(0.0)),
                            BigDecimal.valueOf(ch.path("commissionDeduction").asDouble(0.0)),
                            BigDecimal.valueOf(ch.path("advanceAmount").asDouble(0.0)),
                            BigDecimal.valueOf(ch.path("freightAmount").asDouble(0.0)),
                            BigDecimal.valueOf(ch.path("netPayable").asDouble(0.0)),
                            ch.path("paid").asBoolean(false),
                            payDate,
                            ch.path("paidTo").asText(""),
                            payTno,
                            ch.path("paymentMethod").asText("CASH")
                    });

                    JsonNode items = ch.get("items");
                    if (items != null && items.isArray()) {
                        for (JsonNode it : items) {
                            itemBatch.add(new Object[]{
                                    chItemId++,
                                    tno,
                                    it.path("itemId").asLong(),
                                    BigDecimal.valueOf(it.path("quantity").asDouble(0.0)),
                                    BigDecimal.valueOf(it.path("rate").asDouble(0.0)),
                                    BigDecimal.valueOf(it.path("amount").asDouble(0.0))
                            });
                        }
                    }
                }
            }

            if (!chBatch.isEmpty()) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO chittas (id, chitta_no, chitta_date, farmer_account_id, financial_year_id, gross_amount, commission_deduction, advance_amount, freight_amount, net_payable, paid, pay_date, paid_to, pay_tno, payment_method, del, crby, modby) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                        chBatch
                );
                chCount = chBatch.size();
            }

            if (!itemBatch.isEmpty()) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO chitta_items (id, chitta_id, item_id, quantity, rate, amount, del, crby, modby) VALUES (?, ?, ?, ?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                        itemBatch
                );
                chItemCount = itemBatch.size();
            }
            stats.put("chittas", chCount);
            stats.put("chittaItems", chItemCount);

            // 13. Sales Bills (324) and Sales Bill Items (626)
            JsonNode sbNodes = root.get("salesBills");
            int sbCount = 0;
            int sbItemCount = 0;
            if (sbNodes != null && sbNodes.isArray()) {
                List<Object[]> sbBatch = new ArrayList<>();
                List<Object[]> sbItemBatch = new ArrayList<>();
                long sbItemId = 1;

                for (JsonNode sb : sbNodes) {
                    long tno = sb.path("tno").asLong();
                    LocalDate bDate = LocalDate.parse(sb.path("billDate").asText("2023-04-26"));
                    String yCode = sb.path("yearCode").asText("2324");
                    Long sbFyId = fyMap.getOrDefault(yCode, 9L);

                    sbBatch.add(new Object[]{
                            tno,
                            sb.path("billNo").asText(""),
                            bDate,
                            sb.path("buyerAccountId").asLong(),
                            sbFyId,
                            BigDecimal.valueOf(sb.path("grossAmount").asDouble(0.0)),
                            BigDecimal.valueOf(sb.path("commissionCharges").asDouble(0.0)),
                            BigDecimal.valueOf(sb.path("packingCharges").asDouble(0.0)),
                            BigDecimal.valueOf(sb.path("freightCharges").asDouble(0.0)),
                            BigDecimal.valueOf(sb.path("otherCharges").asDouble(0.0)),
                            BigDecimal.valueOf(sb.path("discount").asDouble(0.0)),
                            BigDecimal.valueOf(sb.path("netReceivable").asDouble(0.0)),
                            sb.path("paymentMethod").asText("CASH")
                    });

                    JsonNode items = sb.get("items");
                    if (items != null && items.isArray()) {
                        for (JsonNode it : items) {
                            sbItemBatch.add(new Object[]{
                                    sbItemId++,
                                    tno,
                                    it.path("itemId").asLong(),
                                    it.path("bags").asInt(0),
                                    BigDecimal.valueOf(it.path("quantity").asDouble(0.0)),
                                    BigDecimal.valueOf(it.path("rate").asDouble(0.0)),
                                    BigDecimal.valueOf(it.path("amount").asDouble(0.0))
                            });
                        }
                    }
                }

                jdbcTemplate.batchUpdate(
                        "INSERT INTO sales_bills (id, bill_no, bill_date, buyer_account_id, financial_year_id, gross_amount, commission_charges, packing_charges, freight_charges, other_charges, discount, net_receivable, payment_method, del, crby, modby) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                        sbBatch
                );
                sbCount = sbBatch.size();

                if (!sbItemBatch.isEmpty()) {
                    jdbcTemplate.batchUpdate(
                            "INSERT INTO sales_bill_items (id, sales_bill_id, item_id, bags, quantity, rate, amount, del, crby, modby) VALUES (?, ?, ?, ?, ?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                            sbItemBatch
                    );
                    sbItemCount = sbItemBatch.size();
                }
            }
            stats.put("salesBills", sbCount);
            stats.put("salesBillItems", sbItemCount);

            // 14. Cash Vouchers (923)
            JsonNode cvNodes = root.get("cashVouchers");
            int cvCount = 0;
            if (cvNodes != null && cvNodes.isArray()) {
                List<Object[]> cvBatch = new ArrayList<>();
                long cvId = 1;
                for (JsonNode cv : cvNodes) {
                    cvBatch.add(new Object[]{
                            cvId++,
                            LocalDate.parse(cv.path("voucherDate").asText("2023-04-26")),
                            cv.path("tno").asLong(),
                            cv.path("voucherNo").asText(""),
                            cv.path("accountId").asLong(),
                            BigDecimal.valueOf(cv.path("amount").asDouble(0.0)),
                            cv.path("narration").asText(""),
                            cv.path("paymentMethod").asText("CASH")
                    });
                }
                jdbcTemplate.batchUpdate(
                        "INSERT INTO cash_vouchers (id, voucher_date, transaction_no, voucher_no, account_id, amount, narration, payment_method, del, crby, modby) VALUES (?, ?, ?, ?, ?, ?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                        cvBatch
                );
                cvCount = cvBatch.size();
            }
            stats.put("cashVouchers", cvCount);

            // 15. Receipt Vouchers (369)
            JsonNode rvNodes = root.get("receiptVouchers");
            int rvCount = 0;
            if (rvNodes != null && rvNodes.isArray()) {
                List<Object[]> rvBatch = new ArrayList<>();
                long rvId = 1;
                for (JsonNode rv : rvNodes) {
                    rvBatch.add(new Object[]{
                            rvId++,
                            LocalDate.parse(rv.path("voucherDate").asText("2023-04-26")),
                            rv.path("tno").asLong(),
                            rv.path("voucherNo").asText(""),
                            rv.path("accountId").asLong(),
                            BigDecimal.valueOf(rv.path("amount").asDouble(0.0)),
                            rv.path("narration").asText(""),
                            rv.path("paymentMethod").asText("CASH")
                    });
                }
                jdbcTemplate.batchUpdate(
                        "INSERT INTO receipt_vouchers (id, voucher_date, transaction_no, voucher_no, account_id, amount, narration, payment_method, del, crby, modby) VALUES (?, ?, ?, ?, ?, ?, ?, ?, '   ', 'ADMIN', 'ADMIN')",
                        rvBatch
                );
                rvCount = rvBatch.size();
            }
            stats.put("receiptVouchers", rvCount);

            // 16. System Mappings
            jdbcTemplate.update("INSERT INTO account_mapping (mapping_type, account_id) VALUES ('CASH', 517)");
            jdbcTemplate.update("INSERT INTO account_mapping (mapping_type, account_id) VALUES ('COMMISSION', 540)");
            jdbcTemplate.update("INSERT INTO account_mapping (mapping_type, account_id) VALUES ('SALES', 520)");
            jdbcTemplate.update("INSERT INTO account_mapping (mapping_type, account_id) VALUES ('BILL', 520)");
            stats.put("mappings", 4);

            // 17. Restart Auto-Increment Sequences above existing max IDs
            jdbcTemplate.execute("ALTER TABLE accounts ALTER COLUMN id RESTART WITH 3000");
            jdbcTemplate.execute("ALTER TABLE chittas ALTER COLUMN id RESTART WITH 1500000");
            jdbcTemplate.execute("ALTER TABLE chitta_items ALTER COLUMN id RESTART WITH 1000000");
            jdbcTemplate.execute("ALTER TABLE sales_bills ALTER COLUMN id RESTART WITH 1500000");
            jdbcTemplate.execute("ALTER TABLE sales_bill_items ALTER COLUMN id RESTART WITH 1000000");
            jdbcTemplate.execute("ALTER TABLE cash_vouchers ALTER COLUMN id RESTART WITH 10000");
            jdbcTemplate.execute("ALTER TABLE receipt_vouchers ALTER COLUMN id RESTART WITH 10000");
            jdbcTemplate.execute("ALTER TABLE s_obout ALTER COLUMN id RESTART WITH 2000");
            jdbcTemplate.execute("ALTER TABLE account_transactions ALTER COLUMN id RESTART WITH 15000");
            jdbcTemplate.execute("ALTER TABLE items ALTER COLUMN id RESTART WITH 100");
            jdbcTemplate.execute("ALTER TABLE places ALTER COLUMN id RESTART WITH 100");

            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
            stats.put("status", "SUCCESS");

        } catch (Exception e) {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
            stats.put("status", "ERROR");
            stats.put("message", e.getMessage());
            e.printStackTrace();
        }
        return stats;
    }

    private JsonNode loadExportJson() throws Exception {
        // Try file system first (for immediate updates), then classpath
        File f = new File("src/main/resources/vfp2425_export.json");
        if (f.exists()) {
            return objectMapper.readTree(f);
        }
        ClassPathResource res = new ClassPathResource("vfp2425_export.json");
        if (res.exists()) {
            try (InputStream is = res.getInputStream()) {
                return objectMapper.readTree(is);
            }
        }
        return null;
    }
}
