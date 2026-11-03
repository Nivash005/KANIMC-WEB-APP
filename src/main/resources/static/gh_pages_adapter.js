// Global safeguard against recursion
(function() {
    function safeTodayDateStr() {
        const d = new Date();
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return y + '-' + m + '-' + day;
    }
    try {
        Object.defineProperty(window, 'getTodayDateStr', {
            get: function() { return safeTodayDateStr; },
            set: function() {},
            configurable: true
        });
        Object.defineProperty(window, 'getPeriodCurrentDate', {
            get: function() { return safeTodayDateStr; },
            set: function() {},
            configurable: true
        });
    } catch(e) {
        window.getTodayDateStr = safeTodayDateStr;
        window.getPeriodCurrentDate = safeTodayDateStr;
    }
})();

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
    const DB_KEY = 'KANIMC_GHPAGES_DB_V4';
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

    // Load initial data: keep accounts/masters, completely wipe all transactions to start fresh at #1
    const initPromise = (async function initDatabase() {
        const cached = localStorage.getItem(DB_KEY);
        if (cached) {
            try {
                db = JSON.parse(cached);
                console.log('[KANIMC] Loaded DB from localStorage (' + (db.accounts ? db.accounts.length : 0) + ' accounts, ' + (db.chittas ? db.chittas.length : 0) + ' chittas)');
                return;
            } catch(e) {
                console.warn('[KANIMC] Failed parsing cached DB, reloading:', e);
            }
        }

        const oldV3 = localStorage.getItem('KANIMC_GHPAGES_DB_V3') || localStorage.getItem('KANIMC_GHPAGES_DB_V2');
        if (oldV3) {
            try {
                const prev = JSON.parse(oldV3);
                if (prev.accounts && prev.accounts.length > 0) {
                    db.accounts = prev.accounts;
                    db.places = prev.places || [];
                    db.items = prev.items || [];
                    db.groups = prev.groups || [];
                    db.commissionRates = prev.commissionRates || [];
                    db.company = prev.company || {};
                    db.settings = prev.settings || {};
                    db.periods = prev.periods || [];
                }
                // Wipe every transaction entry completely
                db.chittas = [];
                db.salesBills = [];
                db.cashVouchers = [];
                db.receiptVouchers = [];
                db.openingBalances = [];
                if (db.accounts) {
                    db.accounts.forEach(a => {
                        a.opbal = 0;
                        a.openingBalance = 0;
                        a.balance = 0;
                    });
                }
                saveDb();
                try { localStorage.removeItem('KANIMC_GHPAGES_DB_V3'); } catch(e) {}
                try { localStorage.removeItem('KANIMC_GHPAGES_DB_V2'); } catch(e) {}
                console.log('[KANIMC] Upgraded DB to V4 with fresh #1 transaction slate (' + db.accounts.length + ' accounts, 0 chittas)');
                return;
            } catch(e) {
                console.warn('[KANIMC] Failed upgrading from old DB:', e);
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

        db.chittas = [];
        db.salesBills = [];
        db.cashVouchers = [];
        db.receiptVouchers = [];
        db.openingBalances = [];
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

        const urlObj = new URL(url, window.location.origin);
        let path = urlObj.pathname;
        if (path.startsWith('/KANIMC-WEB-APP/')) {
            path = path.substring('/KANIMC-WEB-APP'.length);
        } else if (path.startsWith('/KANIMC-WEB-APP')) {
            path = path.substring('/KANIMC-WEB-APP'.length);
        }
        if (!path.startsWith('/')) path = '/' + path;

        // TRY FORWARDING TO CENTRAL SERVER FOR REAL-TIME MULTI-DEVICE SINGLE DATABASE
        const centralServer = localStorage.getItem('KANIMC_SERVER_URL') || 'https://raw-contrast-enterprises-relates.trycloudflare.com';
        if (centralServer && !window.location.host.includes('trycloudflare.com') && !window.location.host.includes('192.168.1.7')) {
            try {
                const targetUrl = centralServer.replace(/\/$/, '') + path + urlObj.search;
                const newHeaders = Object.assign({}, (init && init.headers) || {}, {
                    'Bypass-Tunnel-Reminder': 'true'
                });
                const remoteInit = Object.assign({}, init, { headers: newHeaders });
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 6000);
                remoteInit.signal = controller.signal;
                
                const remoteRes = await originalFetch(targetUrl, remoteInit);
                clearTimeout(timeoutId);
                if (remoteRes && remoteRes.status !== 502 && remoteRes.status !== 503 && remoteRes.status !== 504) {
                    return remoteRes;
                }
            } catch(remoteErr) {
                console.warn('[KANIMC] Central server unreachable, falling back to local DB:', remoteErr);
            }
        }

        await initPromise;

        const method = (init && init.method ? init.method.toUpperCase() : 'GET');
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
            
            // SYNC STATUS
            if (path === '/api/sync/status') {
                return jsonResponse({
                    version: 1793634402291,
                    chittaCount: (db.chittas || []).length,
                    paidCount: (db.chittas || []).filter(c => c.paid).length,
                    accountCount: (db.accounts || []).length,
                    status: 'STANDALONE_LOCAL'
                });
            }

            // 1. ACCOUNTS
            if (path === '/api/accounts') {
                if (method === 'GET') {
                    if (Array.isArray(db.accounts)) {
                        db.accounts.forEach(a => {
                            if (!a.partyId || String(a.partyId).startsWith('P-') || String(a.partyId).length > 4) {
                                a.partyId = String(a.id || '').substring(0, 4);
                            }
                        });
                    }
                    return jsonResponse(db.accounts);
                } else if (method === 'POST') {
                    const nextId = db.accounts.length > 0 ? Math.max(...db.accounts.map(a => a.id || 0)) + 1 : 1;
                    let pId = reqBody.partyId ? String(reqBody.partyId).trim() : '';
                    if (!pId || pId.startsWith('P-') || pId.length > 4) {
                        pId = String(nextId > 9999 ? (nextId % 10000) : nextId);
                    }
                    if (pId.length > 4) pId = pId.substring(0, 4);
                    const newAcc = {
                        id: nextId,
                        partyId: pId,
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
                let bal = acc ? (parseFloat(acc.openingBalance) || 0) : 0;
                const isFarmer = acc && (acc.isFarmer || (acc.accountGroup && String(acc.accountGroup.groupName).toUpperCase().includes('FARMER')));

                if (isFarmer) {
                    // Farmer balance: Opening + Chittas (we owe them) - Payments (we paid them)
                    (db.chittas || []).forEach(c => {
                        if (c.farmerAccount && (c.farmerAccount.id === id || String(c.farmerAccount.partyId) === String(id))) {
                            bal += (parseFloat(c.netPayable || c.grossAmount) || 0);
                        }
                    });
                    (db.cashVouchers || []).forEach(cv => {
                        if (cv.account && (cv.account.id === id || String(cv.account.partyId) === String(id))) {
                            bal -= (parseFloat(cv.amount) || 0);
                        }
                    });
                } else {
                    // Trader / Buyer balance: Opening + Sales Bills (they owe us) - Receipts (they paid us)
                    (db.salesBills || []).forEach(b => {
                        if (b.buyerAccount && (b.buyerAccount.id === id || String(b.buyerAccount.partyId) === String(id))) {
                            bal += (parseFloat(b.netReceivable || b.netPayable || b.grossAmount) || 0);
                        }
                    });
                    (db.receiptVouchers || []).forEach(rv => {
                        if (rv.account && (rv.account.id === id || String(rv.account.partyId) === String(id))) {
                            bal -= (parseFloat(rv.amount) || 0);
                        }
                    });
                }
                bal = Math.round(bal * 100) / 100;
                return jsonResponse(bal);
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

            const mChittaPay = path.match(/^\/api\/chittas\/(\d+)\/payment$/);
            if (mChittaPay) {
                const id = parseInt(mChittaPay[1], 10);
                const found = (db.chittas || []).find(c => c.id == id);
                if (!found) return jsonResponse({ error: 'Chitta not found with ID: ' + id }, 404);
                
                if (method === 'POST') {
                    const paidTo = search.get('paidTo') || (found.farmerAccount ? found.farmerAccount.accountName : '');
                    const paymentMethod = search.get('paymentMethod') || 'CASH';
                    const payDate = search.get('payDate') || new Date().toISOString().substring(0, 10);

                    found.paid = true;
                    found.paidTo = paidTo;
                    found.paymentMethod = paymentMethod;
                    found.payDate = payDate;
                    found.status = 'PAID';
                    saveDb();
                    return jsonResponse(found, 200);
                } else if (method === 'DELETE') {
                    found.paid = false;
                    found.paidTo = null;
                    found.paymentMethod = null;
                    found.payDate = null;
                    found.status = 'PENDING';
                    saveDb();
                    return jsonResponse(found, 200);
                }
            }

            const mChittaSingle = path.match(/^\/api\/chittas\/(\d+)$/);
            if (mChittaSingle) {
                const id = parseInt(mChittaSingle[1], 10);
                const found = (db.chittas || []).find(c => c.id == id);
                if (found) return jsonResponse(found, 200);
                return jsonResponse({ error: 'Chitta not found with ID: ' + id }, 404);
            }
            if (path === '/api/chittas') {
                if (method === 'GET') {
                    const date = search.get('date');
                    const chittaNo = search.get('chittaNo');
                    let list = db.chittas || [];
                    if (date) {
                        list = list.filter(c => c.chittaDate && c.chittaDate.substring(0, 10) === date);
                    }
                    if (chittaNo) {
                        list = list.filter(c => c.chittaNo && String(c.chittaNo).trim() === String(chittaNo).trim());
                    }
                    return jsonResponse(list);
                } else if (method === 'POST') {
                    const newId = db.chittas.length > 0 ? Math.max(...db.chittas.map(c => c.id || 0)) + 1 : 1;
                    const newC = {
                        id: newId,
                        ...reqBody,
                        paid: false,
                        payDate: null,
                        paidTo: null,
                        paymentMethod: null,
                        createdAt: new Date().toISOString()
                    };
                    let gross = 0;
                    (newC.items || []).forEach(it => {
                        const amt = Math.round((parseFloat(it.quantity) || 0) * (parseFloat(it.rate) || 0) * 100) / 100;
                        it.amount = amt;
                        gross += amt;
                    });
                    newC.grossAmount = Math.round(gross * 100) / 100;
                    const rate = parseFloat(newC.commissionRate) || 0;
                    newC.commissionDeduction = Math.round((newC.grossAmount * (rate / 100.0)) * 100) / 100;
                    const adv = parseFloat(newC.advanceAmount) || 0;
                    const frt = parseFloat(newC.freightAmount) || 0;
                    newC.netPayable = Math.round((newC.grossAmount - newC.commissionDeduction - adv - frt) * 100) / 100;

                    if (newC.farmerAccount) {
                        const acc = (db.accounts || []).find(a => 
                            (newC.farmerAccount.id && a.id == newC.farmerAccount.id) || 
                            (newC.farmerAccount.partyId && String(a.partyId) == String(newC.farmerAccount.partyId)) ||
                            (newC.farmerName && (a.accountName || '').toLowerCase() === newC.farmerName.toLowerCase())
                        );
                        if (acc) {
                            newC.farmerAccount = acc;
                            newC.farmerName = acc.accountName;
                        }
                    }

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
                    let gross = 0;
                    (newB.items || []).forEach(it => {
                        const amt = Math.round((parseFloat(it.quantity) || 0) * (parseFloat(it.rate) || 0) * 100) / 100;
                        it.amount = amt;
                        gross += amt;
                    });
                    newB.grossAmount = Math.round(gross * 100) / 100;
                    const totalCharges = (parseFloat(newB.freightCharges) || 0) + (parseFloat(newB.packingCharges) || 0) + (parseFloat(newB.commissionCharges) || 0) + (parseFloat(newB.otherCharges) || 0);
                    newB.netReceivable = Math.round((newB.grossAmount + totalCharges - (parseFloat(newB.discount) || 0)) * 100) / 100;
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

            // 9. REPORTS & OUTSTANDINGS (High-Speed In-Memory Filtering & Chronological Sorting for GitHub Pages)
            if (path === '/api/reports/chitta-register') {
                const startDate = search.get('startDate');
                const endDate = search.get('endDate');
                const accountId = search.get('accountId');
                let list = (db.chittas || []).slice();
                if (startDate) list = list.filter(c => c.chittaDate && String(c.chittaDate).substring(0, 10) >= startDate);
                if (endDate) list = list.filter(c => c.chittaDate && String(c.chittaDate).substring(0, 10) <= endDate);
                if (accountId) list = list.filter(c => c.farmerAccount && (c.farmerAccount.id == accountId || String(c.farmerAccount.partyId) == String(accountId)));
                list.sort((a, b) => {
                    const da = String(a.chittaDate || '').substring(0, 10);
                    const db = String(b.chittaDate || '').substring(0, 10);
                    if (da !== db) return da.localeCompare(db);
                    return (parseInt(a.chittaNo, 10) || 0) - (parseInt(b.chittaNo, 10) || 0);
                });
                return jsonResponse(list);
            }
            if (path === '/api/reports/chitta-pending-register') {
                const asOfDate = search.get('asOfDate');
                const startDate = search.get('startDate');
                const accountId = search.get('accountId');
                let list = (db.chittas || []).filter(c => !c.paid && !c.payDate);
                if (startDate) list = list.filter(c => c.chittaDate && String(c.chittaDate).substring(0, 10) >= startDate);
                if (asOfDate) list = list.filter(c => c.chittaDate && String(c.chittaDate).substring(0, 10) <= asOfDate);
                if (accountId) list = list.filter(c => c.farmerAccount && (c.farmerAccount.id == accountId || String(c.farmerAccount.partyId) == String(accountId)));
                list.sort((a, b) => {
                    const da = String(a.chittaDate || '').substring(0, 10);
                    const db = String(b.chittaDate || '').substring(0, 10);
                    if (da !== db) return da.localeCompare(db);
                    return (parseInt(a.chittaNo, 10) || 0) - (parseInt(b.chittaNo, 10) || 0);
                });
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
                list.sort((a, b) => {
                    const da = String(a.payDate || a.chittaDate || '').substring(0, 10);
                    const db = String(b.payDate || b.chittaDate || '').substring(0, 10);
                    if (da !== db) return da.localeCompare(db);
                    return (parseInt(a.chittaNo, 10) || 0) - (parseInt(b.chittaNo, 10) || 0);
                });
                return jsonResponse(list);
            }
            if (path === '/api/reports/bill-register') {
                const startDate = search.get('startDate');
                const endDate = search.get('endDate');
                const accountId = search.get('accountId');
                let list = (db.salesBills || []).map(b => {
                    let weight = 0;
                    (b.items || []).forEach(it => weight += (parseFloat(it.quantity) || 0));
                    return {
                        ...b,
                        netReceivable: b.netReceivable !== undefined ? b.netReceivable : b.netAmount,
                        weight: weight
                    };
                });
                if (startDate) list = list.filter(b => b.billDate && String(b.billDate).substring(0, 10) >= startDate);
                if (endDate) list = list.filter(b => b.billDate && String(b.billDate).substring(0, 10) <= endDate);
                if (accountId) list = list.filter(b => b.buyerAccount && (b.buyerAccount.id == accountId || String(b.buyerAccount.partyId) == String(accountId)));
                list.sort((a, b) => {
                    const da = String(a.billDate || '').substring(0, 10);
                    const db = String(b.billDate || '').substring(0, 10);
                    if (da !== db) return da.localeCompare(db);
                    return (parseInt(a.billNo, 10) || 0) - (parseInt(b.billNo, 10) || 0);
                });
                return jsonResponse(list);
            }
            if (path === '/api/reports/cash-payment-register') {
                const startDate = search.get('startDate');
                const endDate = search.get('endDate');
                const accountId = search.get('accountId');
                let list = (db.cashVouchers || []).slice();
                if (startDate) list = list.filter(v => v.voucherDate && String(v.voucherDate).substring(0, 10) >= startDate);
                if (endDate) list = list.filter(v => v.voucherDate && String(v.voucherDate).substring(0, 10) <= endDate);
                if (accountId) list = list.filter(v => v.account && (v.account.id == accountId || String(v.account.partyId) == String(accountId)));
                list.sort((a, b) => {
                    const da = String(a.voucherDate || '').substring(0, 10);
                    const db = String(b.voucherDate || '').substring(0, 10);
                    if (da !== db) return da.localeCompare(db);
                    return (parseInt(a.voucherNo, 10) || 0) - (parseInt(b.voucherNo, 10) || 0);
                });
                return jsonResponse(list);
            }
            if (path === '/api/reports/receipt-register') {
                const startDate = search.get('startDate');
                const endDate = search.get('endDate');
                const accountId = search.get('accountId');
                let list = (db.receiptVouchers || []).slice();
                if (startDate) list = list.filter(r => r.voucherDate && String(r.voucherDate).substring(0, 10) >= startDate);
                if (endDate) list = list.filter(r => r.voucherDate && String(r.voucherDate).substring(0, 10) <= endDate);
                if (accountId) list = list.filter(r => r.account && (r.account.id == accountId || String(r.account.partyId) == String(accountId)));
                list.sort((a, b) => {
                    const da = String(a.voucherDate || '').substring(0, 10);
                    const db = String(b.voucherDate || '').substring(0, 10);
                    if (da !== db) return da.localeCompare(db);
                    return (parseInt(a.voucherNo, 10) || 0) - (parseInt(b.voucherNo, 10) || 0);
                });
                return jsonResponse(list);
            }
            if (path === '/api/reports/commission-summary') {
                const startDate = search.get('startDate');
                const endDate = search.get('endDate');
                const summaryMap = {};
                (db.chittas || []).forEach(c => {
                    const cDate = c.chittaDate ? String(c.chittaDate).substring(0, 10) : '';
                    if (startDate && cDate < startDate) return;
                    if (endDate && cDate > endDate) return;
                    const comm = parseFloat(c.commissionDeduction || c.commission) || 0;
                    if (comm > 0) {
                        const party = c.farmerAccount ? (c.farmerAccount.accountName || c.farmerAccount.name) : (c.farmerName || 'Unknown');
                        summaryMap[party] = (summaryMap[party] || 0) + comm;
                    }
                });
                (db.salesBills || []).forEach(b => {
                    const bDate = b.billDate ? String(b.billDate).substring(0, 10) : '';
                    if (startDate && bDate < startDate) return;
                    if (endDate && bDate > endDate) return;
                    const comm = parseFloat(b.commissionCharges || b.commission) || 0;
                    if (comm > 0) {
                        const party = b.buyerAccount ? (b.buyerAccount.accountName || b.buyerAccount.name) : (b.buyerName || 'Unknown');
                        summaryMap[party] = (summaryMap[party] || 0) + comm;
                    }
                });
                const summaryList = Object.keys(summaryMap).sort().map(party => ({
                    partyName: party,
                    totalCommission: Math.round(summaryMap[party] * 100) / 100,
                    commission: Math.round(summaryMap[party] * 100) / 100
                }));
                return jsonResponse(summaryList);
            }
            if (path === '/api/reports/outstanding') {
                const asOfDate = search.get('asOfDate') || search.get('endDate');
                const list = [];
                (db.accounts || []).forEach(a => {
                    const id = a.id;
                    const isFarmer = a.isFarmer || (a.accountGroup && String(a.accountGroup.groupName).toUpperCase().includes('FARMER'));
                    const ob = parseFloat(a.openingBalance || a.opbal) || 0;
                    let debits = 0;
                    let credits = 0;

                    if (isFarmer) {
                        (db.chittas || []).forEach(c => {
                            const cDate = c.chittaDate ? String(c.chittaDate).substring(0, 10) : '';
                            if (asOfDate && cDate > asOfDate) return;
                            if (c.farmerAccount && (c.farmerAccount.id === id || String(c.farmerAccount.partyId) === String(a.partyId))) {
                                credits += (parseFloat(c.netPayable || c.grossAmount) || 0);
                            }
                        });
                        (db.cashVouchers || []).forEach(cv => {
                            const vDate = cv.voucherDate ? String(cv.voucherDate).substring(0, 10) : '';
                            if (asOfDate && vDate > asOfDate) return;
                            if (cv.account && (cv.account.id === id || String(cv.account.partyId) === String(a.partyId))) {
                                debits += (parseFloat(cv.amount) || 0);
                            }
                        });
                        const net = Math.round((ob + credits - debits) * 100) / 100;
                        if (net !== 0) {
                            const isDr = net < 0;
                            const absBal = Math.abs(net);
                            list.push({
                                accountId: a.id,
                                partyName: a.accountName,
                                accountName: a.accountName,
                                groupName: a.accountGroup ? a.accountGroup.groupName : '',
                                balance: absBal,
                                closingBalance: isDr ? absBal : -absBal,
                                type: isDr ? 'Dr' : 'Cr',
                                debit: isDr ? absBal : 0,
                                credit: !isDr ? absBal : 0,
                                netBalance: absBal,
                                isFarmer: true,
                                isTrader: false
                            });
                        }
                    } else {
                        (db.salesBills || []).forEach(b => {
                            const bDate = b.billDate ? String(b.billDate).substring(0, 10) : '';
                            if (asOfDate && bDate > asOfDate) return;
                            if (b.buyerAccount && (b.buyerAccount.id === id || String(b.buyerAccount.partyId) === String(a.partyId))) {
                                debits += (parseFloat(b.netReceivable || b.grossAmount) || 0);
                            }
                        });
                        (db.receiptVouchers || []).forEach(rv => {
                            const vDate = rv.voucherDate ? String(rv.voucherDate).substring(0, 10) : '';
                            if (asOfDate && vDate > asOfDate) return;
                            if (rv.account && (rv.account.id === id || String(rv.account.partyId) === String(a.partyId))) {
                                credits += (parseFloat(rv.amount) || 0);
                            }
                        });
                        const net = Math.round((ob + debits - credits) * 100) / 100;
                        if (net !== 0) {
                            const isDr = net >= 0;
                            const absBal = Math.abs(net);
                            list.push({
                                accountId: a.id,
                                partyName: a.accountName,
                                accountName: a.accountName,
                                groupName: a.accountGroup ? a.accountGroup.groupName : '',
                                balance: absBal,
                                closingBalance: isDr ? absBal : -absBal,
                                type: isDr ? 'Dr' : 'Cr',
                                debit: isDr ? absBal : 0,
                                credit: !isDr ? absBal : 0,
                                netBalance: absBal,
                                isFarmer: false,
                                isTrader: true
                            });
                        }
                    }
                });
                return jsonResponse(list);
            }
                        if (path === '/api/admin/reset-transactions') {
                db.chittas = [];
                db.salesBills = [];
                db.cashVouchers = [];
                db.receiptVouchers = [];
                db.openingBalances = [];
                if (db.accounts) {
                    db.accounts.forEach(a => {
                        a.opbal = 0;
                        a.openingBalance = 0;
                        a.balance = 0;
                    });
                }
                saveDb();
                return jsonResponse({
                    status: 'SUCCESS',
                    message: 'All transaction entries completely removed! Counters reset to 1.'
                });
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


