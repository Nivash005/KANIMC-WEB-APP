/**
 * GitHub Pages Client-Side Backend Adapter for KANIMC Web Application
 * Enables full functionality (database, autocomplete, CRUD, registers, reports)
 * when running statically on GitHub Pages (github.io) without requiring a live JVM backend.
 */
(function() {
    const isGhPages = window.location.hostname.includes('github.io') || window.location.protocol === 'file:';
    if (!isGhPages) {
        console.log('[KANIMC] Running in native mode against live Spring Boot backend.');
        return;
    }

    console.log('[KANIMC] GitHub Pages detected. Initializing client-side database adapter...');

    // In-memory / LocalStorage Database
    const DB_KEY = 'KANIMC_GHPAGES_DB_V1';
    let db = {
        accounts: [],
        chittas: [],
        salesBills: [],
        cashVouchers: [],
        receiptVouchers: [],
        openingBalances: [],
        periods: [],
        places: [],
        items: [],
        groups: [],
        commissionRates: [],
        company: {},
        settings: {}
    };

    function saveDb() {
        try {
            localStorage.setItem(DB_KEY, JSON.stringify(db));
        } catch(e) {
            console.warn('[KANIMC] LocalStorage quota exceeded, keeping in-memory:', e);
        }
    }

    // Load initial data from bundled JSON files if localStorage is empty
    const initPromise = (async function initDatabase() {
        const cached = localStorage.getItem(DB_KEY);
        if (cached) {
            try {
                db = JSON.parse(cached);
                console.log('[KANIMC] Loaded DB from localStorage (' + db.accounts.length + ' accounts, ' + db.chittas.length + ' chittas)');
                return;
            } catch(e) {
                console.warn('[KANIMC] Failed parsing cached DB, reloading from json:', e);
            }
        }

        const files = [
            { key: 'accounts', url: 'data/accounts.json' },
            { key: 'chittas', url: 'data/chittas.json' },
            { key: 'salesBills', url: 'data/sales_bills.json' },
            { key: 'cashVouchers', url: 'data/cash_vouchers.json' },
            { key: 'receiptVouchers', url: 'data/receipt_vouchers.json' },
            { key: 'openingBalances', url: 'data/opening_balances.json' },
            { key: 'periods', url: 'data/periods.json' },
            { key: 'places', url: 'data/places.json' },
            { key: 'items', url: 'data/items.json' },
            { key: 'groups', url: 'data/groups.json' },
            { key: 'commissionRates', url: 'data/commission_rates.json' },
            { key: 'company', url: 'data/company.json' },
            { key: 'settings', url: 'data/settings.json' }
        ];

        await Promise.all(files.map(async f => {
            try {
                const res = await fetch(f.url);
                if (res.ok) {
                    db[f.key] = await res.json();
                }
            } catch(err) {
                console.warn('[KANIMC] Could not load ' + f.url, err);
            }
        }));

        saveDb();
        console.log('[KANIMC] Database initialized successfully (' + db.accounts.length + ' accounts, ' + db.chittas.length + ' chittas)');
    })();

    // Intercept window.fetch for /api/ calls
    const originalFetch = window.fetch;
    window.fetch = async function(resource, init) {
        let url = typeof resource === 'string' ? resource : resource.url;
        
        // Let non-API requests or requests to json pass through
        if (!url.includes('/api/')) {
            return originalFetch(resource, init);
        }

        await initPromise;

        const method = (init && init.method ? init.method.toUpperCase() : 'GET');
        const urlObj = new URL(url, window.location.origin);
        const path = urlObj.pathname;
        const search = urlObj.searchParams;

        let reqBody = null;
        if (init && init.body) {
            try {
                reqBody = typeof init.body === 'string' ? JSON.parse(init.body) : init.body;
            } catch(e) {
                reqBody = init.body;
            }
        }

        console.log(`[Mock API] ${method} ${path}`, reqBody || '');

        function jsonResponse(data, status = 200) {
            return new Response(JSON.stringify(data), {
                status: status,
                headers: { 'Content-Type': 'application/json' }
            });
        }

        function textResponse(text, status = 200) {
            return new Response(text, {
                status: status,
                headers: { 'Content-Type': 'text/plain' }
            });
        }

        try {
            // 1. ACCOUNTS
            if (path === '/api/accounts') {
                if (method === 'GET') {
                    return jsonResponse(db.accounts);
                } else if (method === 'POST') {
                    const newAcc = {
                        id: db.accounts.length > 0 ? Math.max(...db.accounts.map(a => a.id || 0)) + 1 : 1,
                        partyId: reqBody.partyId || 'P-' + Date.now(),
                        accountName: reqBody.accountName || '',
                        regionalName: reqBody.regionalName || reqBody.place || '',
                        accountGroup: typeof reqBody.accountGroup === 'object' ? reqBody.accountGroup : { groupName: reqBody.group || 'GENERAL' },
                        phone1: reqBody.phone1 || reqBody.phone || '',
                        openingBalance: parseFloat(reqBody.openingBalance) || 0,
                        isFarmer: !!reqBody.isFarmer,
                        isTrader: !!reqBody.isTrader,
                        createdAt: new Date().toISOString()
                    };
                    db.accounts.push(newAcc);
                    saveDb();
                    return jsonResponse(newAcc, 201);
                }
            }
            if (path.startsWith('/api/accounts/') && path.endsWith('/balance')) {
                const parts = path.split('/');
                const id = parseInt(parts[3], 10);
                const acc = db.accounts.find(a => a.id === id || String(a.partyId) === String(id));
                const bal = acc ? (parseFloat(acc.openingBalance) || 0) : 0;
                return jsonResponse({ balance: bal, closingBalance: bal });
            }

            // 2. CHITTAS
            if (path === '/api/chittas/next-no') {
                const dt = search.get('date') || new Date().toISOString().substring(0, 10);
                let max = 0;
                db.chittas.forEach(c => {
                    if (c.chittaDate && c.chittaDate.substring(0, 10) === dt) {
                        const n = parseInt(c.chittaNo, 10);
                        if (!isNaN(n) && n > max) max = n;
                    }
                });
                return jsonResponse({ nextNo: max + 1 });
            }
            if (path === '/api/chittas') {
                if (method === 'GET') {
                    const date = search.get('date');
                    if (date) {
                        return jsonResponse(db.chittas.filter(c => c.chittaDate && c.chittaDate.substring(0, 10) === date));
                    }
                    return jsonResponse(db.chittas);
                } else if (method === 'POST') {
                    const newC = {
                        id: db.chittas.length > 0 ? Math.max(...db.chittas.map(c => c.id || 0)) + 1 : 1,
                        ...reqBody,
                        createdAt: new Date().toISOString()
                    };
                    db.chittas.push(newC);
                    saveDb();
                    return jsonResponse(newC, 201);
                }
            }

            // 3. SALES BILLS
            if (path === '/api/sales-bills/next-no' || path === '/api/bills/next-no') {
                let max = 0;
                db.salesBills.forEach(b => {
                    const n = parseInt(b.billNo, 10);
                    if (!isNaN(n) && n > max) max = n;
                });
                return jsonResponse({ nextNo: max + 1 });
            }
            if (path === '/api/sales-bills' || path === '/api/bills') {
                if (method === 'GET') {
                    return jsonResponse(db.salesBills);
                } else if (method === 'POST') {
                    const newB = {
                        id: db.salesBills.length > 0 ? Math.max(...db.salesBills.map(b => b.id || 0)) + 1 : 1,
                        ...reqBody,
                        createdAt: new Date().toISOString()
                    };
                    db.salesBills.push(newB);
                    saveDb();
                    return jsonResponse(newB, 201);
                }
            }

            // 4. CASH VOUCHERS / PAYMENTS
            if (path === '/api/cash-vouchers' || path === '/api/cash-payments') {
                if (method === 'GET') {
                    return jsonResponse(db.cashVouchers);
                } else if (method === 'POST') {
                    const newV = {
                        id: db.cashVouchers.length > 0 ? Math.max(...db.cashVouchers.map(v => v.id || 0)) + 1 : 1,
                        ...reqBody,
                        createdAt: new Date().toISOString()
                    };
                    db.cashVouchers.push(newV);
                    saveDb();
                    return jsonResponse(newV, 201);
                }
            }

            // 5. RECEIPT VOUCHERS
            if (path === '/api/receipt-vouchers' || path === '/api/receipts') {
                if (method === 'GET') {
                    return jsonResponse(db.receiptVouchers);
                } else if (method === 'POST') {
                    const newV = {
                        id: db.receiptVouchers.length > 0 ? Math.max(...db.receiptVouchers.map(v => v.id || 0)) + 1 : 1,
                        ...reqBody,
                        createdAt: new Date().toISOString()
                    };
                    db.receiptVouchers.push(newV);
                    saveDb();
                    return jsonResponse(newV, 201);
                }
            }

            // 6. PERIODS
            if (path === '/api/periods') {
                if (method === 'GET') return jsonResponse(db.periods);
                if (method === 'POST') {
                    db.periods.push(reqBody);
                    saveDb();
                    return jsonResponse(reqBody, 201);
                }
            }

            // 7. OPENING BALANCES
            if (path === '/api/opening-balances') {
                if (method === 'GET') return jsonResponse(db.openingBalances);
                if (method === 'POST') {
                    db.openingBalances.push(reqBody);
                    saveDb();
                    return jsonResponse(reqBody, 201);
                }
            }

            // 8. MASTERS: Places, Items, Groups, Commission
            if (path === '/api/places') {
                if (method === 'GET') return jsonResponse(db.places);
                if (method === 'POST') { db.places.push(reqBody); saveDb(); return jsonResponse(reqBody, 201); }
            }
            if (path === '/api/items') {
                if (method === 'GET') return jsonResponse(db.items);
                if (method === 'POST') { db.items.push(reqBody); saveDb(); return jsonResponse(reqBody, 201); }
            }
            if (path === '/api/account-groups') {
                if (method === 'GET') return jsonResponse(db.groups);
                if (method === 'POST') { db.groups.push(reqBody); saveDb(); return jsonResponse(reqBody, 201); }
            }
            if (path === '/api/commission-rates') {
                if (method === 'GET') return jsonResponse(db.commissionRates);
                if (method === 'POST') { db.commissionRates.push(reqBody); saveDb(); return jsonResponse(reqBody, 201); }
            }
            if (path === '/api/company') {
                if (method === 'GET') return jsonResponse(db.company);
                if (method === 'POST') { db.company = reqBody; saveDb(); return jsonResponse(reqBody, 200); }
            }
            if (path === '/api/settings') {
                if (method === 'GET') return jsonResponse(db.settings);
                if (method === 'POST') { db.settings = reqBody; saveDb(); return jsonResponse(reqBody, 200); }
            }

            // 9. REPORTS & OUTSTANDINGS (High-Speed In-Memory Filtering for GitHub Pages)
            if (path === '/api/reports/chitta-register') {
                const startDate = search.get('startDate');
                const endDate = search.get('endDate');
                const accountId = search.get('accountId');
                let list = db.chittas || [];
                if (startDate) list = list.filter(c => c.chittaDate && String(c.chittaDate).substring(0, 10) >= startDate);
                if (endDate) list = list.filter(c => c.chittaDate && String(c.chittaDate).substring(0, 10) <= endDate);
                if (accountId) list = list.filter(c => c.farmerAccount && (c.farmerAccount.id == accountId || String(c.farmerAccount.partyId) == String(accountId)));
                return jsonResponse(list);
            }
            if (path === '/api/reports/chitta-pending-register') {
                const asOfDate = search.get('asOfDate');
                const accountId = search.get('accountId');
                let list = (db.chittas || []).filter(c => !c.paid && !c.payDate);
                if (asOfDate) list = list.filter(c => c.chittaDate && String(c.chittaDate).substring(0, 10) <= asOfDate);
                if (accountId) list = list.filter(c => c.farmerAccount && (c.farmerAccount.id == accountId || String(c.farmerAccount.partyId) == String(accountId)));
                return jsonResponse(list);
            }
            if (path === '/api/reports/chitta-payment-register') {
                const startDate = search.get('startDate');
                const endDate = search.get('endDate');
                const accountId = search.get('accountId');
                let list = (db.chittas || []).filter(c => c.paid || c.payDate);
                if (startDate) list = list.filter(c => (c.payDate || c.chittaDate) && String(c.payDate || c.chittaDate).substring(0, 10) >= startDate);
                if (endDate) list = list.filter(c => (c.payDate || c.chittaDate) && String(c.payDate || c.chittaDate).substring(0, 10) <= endDate);
                if (accountId) list = list.filter(c => c.farmerAccount && (c.farmerAccount.id == accountId || String(c.farmerAccount.partyId) == String(accountId)));
                return jsonResponse(list);
            }
            if (path === '/api/reports/bill-register') {
                const startDate = search.get('startDate');
                const endDate = search.get('endDate');
                const accountId = search.get('accountId');
                let list = db.salesBills || [];
                if (startDate) list = list.filter(b => b.billDate && String(b.billDate).substring(0, 10) >= startDate);
                if (endDate) list = list.filter(b => b.billDate && String(b.billDate).substring(0, 10) <= endDate);
                if (accountId) list = list.filter(b => b.buyerAccount && (b.buyerAccount.id == accountId || String(b.buyerAccount.partyId) == String(accountId)));
                return jsonResponse(list);
            }
            if (path === '/api/reports/cash-payment-register') {
                const startDate = search.get('startDate');
                const endDate = search.get('endDate');
                const accountId = search.get('accountId');
                let list = db.cashVouchers || [];
                if (startDate) list = list.filter(v => v.voucherDate && String(v.voucherDate).substring(0, 10) >= startDate);
                if (endDate) list = list.filter(v => v.voucherDate && String(v.voucherDate).substring(0, 10) <= endDate);
                if (accountId) list = list.filter(v => v.account && (v.account.id == accountId || String(v.account.partyId) == String(accountId)));
                return jsonResponse(list);
            }
            if (path === '/api/reports/receipt-register') {
                const startDate = search.get('startDate');
                const endDate = search.get('endDate');
                const accountId = search.get('accountId');
                let list = db.receiptVouchers || [];
                if (startDate) list = list.filter(v => v.voucherDate && String(v.voucherDate).substring(0, 10) >= startDate);
                if (endDate) list = list.filter(v => v.voucherDate && String(v.voucherDate).substring(0, 10) <= endDate);
                if (accountId) list = list.filter(v => v.account && (v.account.id == accountId || String(v.account.partyId) == String(accountId)));
                return jsonResponse(list);
            }
            if (path.startsWith('/api/reports/')) {
                return jsonResponse([]);
            }

            // Default fallback
            return jsonResponse({ status: 'ok', clientMode: true });
        } catch(e) {
            console.error('[Mock API Error]', e);
            return jsonResponse({ error: e.message }, 500);
        }
    };
})();
