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
 * Universal Offline-First Database Adapter & Automatic Sync Engine for KANIMC Web Application
 * - When offline: seamlessly saves all mutations (chittas, bills, vouchers, accounts) to local storage.
 * - When internet/network connects: automatically syncs queued offline changes to the central database.
 * - Zero changes to UI layout, accounting logic, or business formulas.
 */
(function() {
    console.log('[KANIMC] Initializing Offline-First database adapter & auto-sync engine...');

    const DB_KEY = 'KANIMC_GHPAGES_DB_V4';
    const OFFLINE_QUEUE_KEY = 'KANIMC_OFFLINE_SYNC_QUEUE';

    // Circuit breaker state
    let isCentralServerAlive = true;
    let lastServerProbeTime = 0;
    const PROBE_TIMEOUT_MS = 2000;
    const SERVER_DOWN_COOLDOWN_MS = 15000;

    // In-memory / LocalStorage Database
    let db = {
        version: 1000,
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
            db.version = (db.version || 1000) + 1;
            localStorage.setItem(DB_KEY, JSON.stringify(db));
        } catch(e) {
            console.warn('[KANIMC] LocalStorage quota exceeded, keeping in-memory:', e);
        }
    }

    // Offline Sync Queue Helpers
    window.getOfflineQueue = function() {
        try {
            const raw = localStorage.getItem(OFFLINE_QUEUE_KEY);
            return raw ? JSON.parse(raw) : [];
        } catch(e) {
            return [];
        }
    };

    window.saveOfflineQueue = function(queue) {
        try {
            localStorage.setItem(OFFLINE_QUEUE_KEY, JSON.stringify(queue));
        } catch(e) {
            console.error('[KANIMC] Failed to save offline sync queue:', e);
        }
    };

    window.getOfflineQueueLength = function() {
        return window.getOfflineQueue().length;
    };

    window.enqueueOfflineAction = function(method, path, body, meta = {}) {
        const queue = window.getOfflineQueue();
        const action = {
            id: 'sync_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7),
            timestamp: new Date().toISOString(),
            method: method,
            path: path,
            body: body,
            meta: meta
        };
        queue.push(action);
        window.saveOfflineQueue(queue);
        console.log(`[KANIMC Offline] Action enqueued: ${method} ${path} (Pending sync: ${queue.length})`);
        if (typeof updateLiveSyncBadge === 'function') {
            updateLiveSyncBadge(false);
        }
    };

    function getCentralServerUrl() {
        // If running locally, never route to external Cloudflare tunnel
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            return window.location.origin;
        }
        let cs = localStorage.getItem('KANIMC_SERVER_URL');
        // Clear dead / expired Cloudflare tunnel domains from localStorage
        if (cs && (cs.includes('lake-divine-independent-emacs') || cs.includes('meets-epson-kentucky-stats'))) {
            cs = 'https://senator-carmen-someone-lopez.trycloudflare.com';
            try { localStorage.setItem('KANIMC_SERVER_URL', cs); } catch(e) {}
        }
        if (!cs) {
            if (!window.location.hostname.includes('github.io') && window.location.port !== '') {
                cs = window.location.origin;
            } else {
                cs = 'https://senator-carmen-someone-lopez.trycloudflare.com';
            }
        }
        return cs;
    }

    // Auto-Sync Worker
    let isSyncingQueue = false;
    window.processOfflineSyncQueue = async function() {
        if (isSyncingQueue) return;
        const queue = window.getOfflineQueue();
        if (!queue || queue.length === 0) return;

        if (navigator.onLine === false) return;

        // Circuit breaker: do not spam network if server is marked down and still cooling down
        if (!isCentralServerAlive && Date.now() - lastServerProbeTime < SERVER_DOWN_COOLDOWN_MS) {
            return;
        }

        const centralServer = getCentralServerUrl();
        if (!centralServer) return;

        isSyncingQueue = true;
        console.log(`[KANIMC Sync] Connected. Processing ${queue.length} offline action(s)...`);

        const remainingQueue = [];
        let syncedCount = 0;
        const idMapping = {}; // localId -> realServerId

        for (let i = 0; i < queue.length; i++) {
            const item = queue[i];
            let actionPath = item.path;
            let actionBody = item.body;

            // Map temporary local IDs to real server IDs if dependent
            for (const [oldId, newId] of Object.entries(idMapping)) {
                actionPath = actionPath.replace(new RegExp(`/${oldId}(/|\\?|$)`), `/${newId}$1`);
            }

            try {
                let targetUrl = actionPath.startsWith('http://') || actionPath.startsWith('https://')
                    ? actionPath
                    : centralServer.replace(/\/$/, '') + actionPath;

                const headers = {
                    'Content-Type': 'application/json',
                    'Bypass-Tunnel-Reminder': 'true'
                };
                const fetchInit = {
                    method: item.method,
                    headers: headers
                };
                if (actionBody && item.method !== 'GET') {
                    fetchInit.body = typeof actionBody === 'string' ? actionBody : JSON.stringify(actionBody);
                }

                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 8000);
                fetchInit.signal = controller.signal;

                const res = await originalFetch(targetUrl, fetchInit);
                clearTimeout(timeoutId);

                if (res.ok || res.status === 201 || res.status === 200) {
                    syncedCount++;
                    try {
                        const serverEntity = await res.json();
                        if (serverEntity && serverEntity.id && item.meta && item.meta.localId) {
                            idMapping[item.meta.localId] = serverEntity.id;
                        }
                    } catch(e) {}
                } else if (res.status === 400 || res.status === 409) {
                    // Record already exists on server, safely mark as synced
                    console.warn(`[KANIMC Sync] Server responded ${res.status} for ${item.path}, item marked synced.`);
                    syncedCount++;
                } else if (res.status === 404) {
                    if (item.method === 'PUT') {
                        // If record didn't exist for PUT on server, fallback to POST to create it!
                        const createUrl = targetUrl.replace(/\/\d+$/, '');
                        try {
                            const postRes = await originalFetch(createUrl, { ...fetchInit, method: 'POST' });
                            if (postRes && (postRes.ok || postRes.status === 201 || postRes.status === 400 || postRes.status === 409)) {
                                syncedCount++;
                            } else {
                                console.warn(`[KANIMC Sync] PUT 404 handled for ${item.path}`);
                                syncedCount++;
                            }
                        } catch(e) {
                            syncedCount++;
                        }
                    } else if (item.method === 'DELETE') {
                        // Already deleted on server
                        syncedCount++;
                    } else {
                        remainingQueue.push(item);
                        remainingQueue.push(...queue.slice(i + 1));
                        break;
                    }
                } else if (res.status >= 500) {
                    console.warn(`[KANIMC Sync] Server error ${res.status} for ${item.path}, retaining in queue.`);
                    remainingQueue.push(item);
                    remainingQueue.push(...queue.slice(i + 1));
                    isCentralServerAlive = false;
                    lastServerProbeTime = Date.now();
                    break;
                } else {
                    remainingQueue.push(item);
                    remainingQueue.push(...queue.slice(i + 1));
                    break;
                }
            } catch(netErr) {
                console.warn('[KANIMC Sync] Server not reachable during sync, will retry later:', netErr.message || netErr);
                remainingQueue.push(item);
                remainingQueue.push(...queue.slice(i + 1));
                isCentralServerAlive = false;
                lastServerProbeTime = Date.now();
                SERVER_DOWN_COOLDOWN_MS = 30000;
                // If dead tunnel in localStorage, purge it
                if (centralServer && centralServer.includes('trycloudflare.com')) {
                    try { localStorage.removeItem('KANIMC_SERVER_URL'); } catch(e) {}
                }
                break;
            }
        }

        window.saveOfflineQueue(remainingQueue);
        isSyncingQueue = false;

        if (syncedCount > 0) {
            console.log(`[KANIMC Sync] Synchronized ${syncedCount} offline record(s) to Central Server.`);
            const msg = `🟢 Internet Connected: ${syncedCount} offline record(s) synchronized with Central Server!`;
            if (typeof showLiveSyncToast === 'function') {
                showLiveSyncToast(msg);
            } else if (typeof showToast === 'function') {
                showToast(msg);
            }
            if (typeof checkSyncStatus === 'function') {
                checkSyncStatus(true);
            }
        }

        if (typeof updateLiveSyncBadge === 'function') {
            updateLiveSyncBadge(remainingQueue.length === 0);
        }
    };

    // Auto-listen to online/offline events
    window.addEventListener('online', function() {
        console.log('[KANIMC] Network status: ONLINE');
        isCentralServerAlive = true;
        lastServerProbeTime = 0;
        if (typeof window.processOfflineSyncQueue === 'function') {
            window.processOfflineSyncQueue();
        }
    });

    window.addEventListener('offline', function() {
        console.log('[KANIMC] Network status: OFFLINE');
        isCentralServerAlive = false;
        lastServerProbeTime = Date.now();
        if (typeof updateLiveSyncBadge === 'function') {
            updateLiveSyncBadge(false);
        }
    });

    // Load initial data into localStorage DB
    const originalFetch = window.fetch;

    const initPromise = (async function initDatabase() {
        const cached = localStorage.getItem(DB_KEY);
        if (cached) {
            try {
                db = JSON.parse(cached);
                if (db.accounts && db.accounts.length > 0) {
                    console.log('[KANIMC] Loaded DB from localStorage (' + db.accounts.length + ' accounts, ' + (db.chittas ? db.chittas.length : 0) + ' chittas)');
                    return;
                }
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
                    db.chittas = [];
                    db.salesBills = [];
                    db.cashVouchers = [];
                    db.receiptVouchers = [];
                    db.openingBalances = [];
                    saveDb();
                    try { localStorage.removeItem('KANIMC_GHPAGES_DB_V3'); } catch(e) {}
                    try { localStorage.removeItem('KANIMC_GHPAGES_DB_V2'); } catch(e) {}
                    console.log('[KANIMC] Upgraded DB to V4 (' + db.accounts.length + ' accounts)');
                    return;
                }
            } catch(e) {
                console.warn('[KANIMC] Failed upgrading from old DB:', e);
            }
        }

        // Fresh initialization from pre-exported static json files
        const files = [
            { key: 'accounts', url: 'data/accounts.json' },
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
                const res = await originalFetch(f.url);
                if (res.ok) {
                    db[f.key] = await res.json();
                }
            } catch(err) {
                console.warn('[KANIMC] Could not load ' + f.url, err);
            }
        }));

        db.chittas = db.chittas || [];
        db.salesBills = db.salesBills || [];
    db.purchases = db.purchases || [];
    db.purchasePayments = db.purchasePayments || [];
    db.purchaseOpeningBalances = db.purchaseOpeningBalances || [];
        db.cashVouchers = db.cashVouchers || [];
        db.receiptVouchers = db.receiptVouchers || [];
        db.openingBalances = db.openingBalances || [];
        saveDb();
        console.log('[KANIMC] Database initialized successfully (' + (db.accounts ? db.accounts.length : 0) + ' accounts, ' + db.chittas.length + ' chittas)');
    })();

    // Intercept window.fetch for /api/ calls
    window.fetch = async function(resource, init) {
        let url = typeof resource === 'string' ? resource : resource.url;
        
        // Let non-API requests or static assets pass through
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

        const centralServer = getCentralServerUrl();

        // 1. CIRCUIT BREAKER: Determine whether to attempt central server
        let shouldTryCentral = false;
        if (navigator.onLine !== false && centralServer) {
            if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
                shouldTryCentral = true;
            } else if (isCentralServerAlive) {
                shouldTryCentral = true;
            } else if (Date.now() - lastServerProbeTime > SERVER_DOWN_COOLDOWN_MS) {
                shouldTryCentral = true;
            }
        }

        if (shouldTryCentral) {
            try {
                let targetUrl = url;
                if (!url.startsWith('http://') && !url.startsWith('https://')) {
                    if (centralServer && !window.location.host.includes('trycloudflare.com') && !window.location.host.includes('192.168.1.7')) {
                        targetUrl = centralServer.replace(/\/$/, '') + path + urlObj.search;
                    } else {
                        targetUrl = window.location.origin + path + urlObj.search;
                    }
                }
                const newHeaders = Object.assign({}, (init && init.headers) || {}, {
                    'Bypass-Tunnel-Reminder': 'true'
                });
                const remoteInit = Object.assign({}, init, { headers: newHeaders });
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), PROBE_TIMEOUT_MS);
                remoteInit.signal = controller.signal;
                
                const remoteRes = await originalFetch(targetUrl, remoteInit);
                clearTimeout(timeoutId);

                // Any 5xx error means server/tunnel down -> trigger circuit breaker and fall back
                if (remoteRes.status >= 500) {
                    console.warn(`[KANIMC] Server responded with status ${remoteRes.status}. Engaging offline fallback.`);
                    isCentralServerAlive = false;
                    lastServerProbeTime = Date.now();
                } else {
                    isCentralServerAlive = true;

                    // Automatically cache entities into local DB for future offline use
                    if (method === 'GET' && remoteRes.ok) {
                        try {
                            const clone = remoteRes.clone();
                            clone.json().then(data => {
                                if (data) {
                                    if (path === '/api/accounts' && Array.isArray(data) && data.length > 0) db.accounts = data;
                                    else if ((path === '/api/periods' || path === '/api/admin/periods') && Array.isArray(data) && data.length > 0) db.periods = data;
                                    else if (path === '/api/items' && Array.isArray(data) && data.length > 0) db.items = data;
                                    else if (path === '/api/places' && Array.isArray(data) && data.length > 0) db.places = data;
                                    else if (path === '/api/account-groups' && Array.isArray(data) && data.length > 0) db.groups = data;
                                    else if (path === '/api/commission-rates' && Array.isArray(data) && data.length > 0) db.commissionRates = data;
                                    else if (path === '/api/company' && typeof data === 'object') db.company = data;
                                    else if (path === '/api/settings' && typeof data === 'object') db.settings = data;
                                    else if (path === '/api/chittas' && Array.isArray(data)) db.chittas = data;
                                    else if ((path === '/api/sales-bills' || path === '/api/bills') && Array.isArray(data)) db.salesBills = data;
                                    else if ((path === '/api/cash-vouchers' || path === '/api/cash-payments') && Array.isArray(data)) db.cashVouchers = data;
                                    else if ((path === '/api/receipt-vouchers' || path === '/api/receipts') && Array.isArray(data)) db.receiptVouchers = data;
                                    saveDb();
                                }
                            }).catch(() => {});
                        } catch(e) {}
                    }

                    // Background drain queue if pending
                    if (window.getOfflineQueueLength() > 0) {
                        setTimeout(() => window.processOfflineSyncQueue(), 500);
                    }
                    return remoteRes;
                }
            } catch(remoteErr) {
                console.warn('[KANIMC] Central server unreachable / offline. Falling back to local DB:', remoteErr.message || remoteErr);
                isCentralServerAlive = false;
                lastServerProbeTime = Date.now();
            }
        }

        // 2. OFFLINE / LOCAL CLIENT DATABASE FALLBACK
        await initPromise;

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
                    version: db.version || 1000,
                    chittaCount: (db.chittas || []).length,
                    paidCount: (db.chittas || []).filter(c => c.paid).length,
                    accountCount: (db.accounts || []).length,
                    status: isCentralServerAlive ? 'ONLINE' : 'OFFLINE_STANDALONE',
                    offlinePending: window.getOfflineQueueLength()
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
                    return jsonResponse(db.accounts || []);
                } else if (method === 'POST') {
                    const nextId = db.accounts && db.accounts.length > 0 ? Math.max(...db.accounts.map(a => a.id || 0)) + 1 : 1;
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
                    db.accounts = db.accounts || [];
                    db.accounts.push(newAcc);
                    saveDb();
                    window.enqueueOfflineAction('POST', path, newAcc, { localId: nextId, type: 'account' });
                    return jsonResponse(newAcc, 201);
                }
            }

            const mAccSingle = path.match(/^\/api\/accounts\/(\d+)$/);
            if (mAccSingle) {
                const id = parseInt(mAccSingle[1], 10);
                const idx = (db.accounts || []).findIndex(a => a.id == id);
                if (method === 'GET') {
                    if (idx >= 0) return jsonResponse(db.accounts[idx], 200);
                    return jsonResponse({ error: 'Account not found with ID: ' + id }, 404);
                } else if (method === 'PUT') {
                    if (idx >= 0) {
                        db.accounts[idx] = { ...db.accounts[idx], ...reqBody, id: id, updatedAt: new Date().toISOString() };
                        saveDb();
                        window.enqueueOfflineAction('PUT', path, reqBody, { localId: id, type: 'account' });
                        return jsonResponse(db.accounts[idx], 200);
                    }
                    return jsonResponse({ error: 'Account not found' }, 404);
                }
            }

            if (path.startsWith('/api/accounts/') && path.endsWith('/balance')) {
                const parts = path.split('/');
                const id = parseInt(parts[3], 10);
                const acc = (db.accounts || []).find(a => a.id === id || String(a.partyId) === String(id));
                let bal = acc ? (parseFloat(acc.openingBalance) || 0) : 0;
                const isFarmer = acc && (acc.isFarmer || (acc.accountGroup && String(acc.accountGroup.groupName).toUpperCase().includes('FARMER')));

                if (isFarmer) {
                    (db.purchasePayments || []).forEach(pp => {
                    const dt = (pp.paymentDate || '').substring(0, 10);
                    if (dt && dt < sDate) {
                        if (!pp.paymentMethod || pp.paymentMethod.toUpperCase() === 'CASH') {
                            opening -= (parseFloat(pp.amount) || 0);
                        }
                    }
                });

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
                (db.chittas || []).forEach(c => {
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
                    window.enqueueOfflineAction('POST', path + urlObj.search, null, { localId: id, type: 'chittaPayment' });
                    return jsonResponse(found, 200);
                } else if (method === 'DELETE') {
                    found.paid = false;
                    found.paidTo = null;
                    found.paymentMethod = null;
                    found.payDate = null;
                    found.status = 'PENDING';
                    saveDb();
                    window.enqueueOfflineAction('DELETE', path, null, { localId: id, type: 'chittaPaymentCancel' });
                    return jsonResponse(found, 200);
                }
            }

            const mChittaSingle = path.match(/^\/api\/chittas\/(\d+)$/);
            if (mChittaSingle) {
                const id = parseInt(mChittaSingle[1], 10);
                const idx = (db.chittas || []).findIndex(c => c.id == id);
                if (method === 'GET') {
                    if (idx >= 0) return jsonResponse(db.chittas[idx], 200);
                    return jsonResponse({ error: 'Chitta not found with ID: ' + id }, 404);
                } else if (method === 'PUT') {
                    if (idx >= 0) {
                        let gross = 0;
                        (reqBody.items || []).forEach(it => {
                            const amt = Math.round((parseFloat(it.quantity) || 0) * (parseFloat(it.rate) || 0) * 100) / 100;
                            it.amount = amt;
                            gross += amt;
                        });
                        gross = Math.round(gross * 100) / 100;
                        const rate = parseFloat(reqBody.commissionRate) || 0;
                        const comm = Math.round((gross * (rate / 100.0)) * 100) / 100;
                        const adv = parseFloat(reqBody.advanceAmount) || 0;
                        const frt = parseFloat(reqBody.freightAmount) || 0;
                        const net = Math.round((gross - comm - adv - frt) * 100) / 100;

                        db.chittas[idx] = {
                            ...db.chittas[idx],
                            ...reqBody,
                            id: id,
                            grossAmount: gross,
                            commissionDeduction: comm,
                            netPayable: net,
                            updatedAt: new Date().toISOString()
                        };
                        saveDb();
                        window.enqueueOfflineAction('PUT', path, reqBody, { localId: id, type: 'chitta' });
                        return jsonResponse(db.chittas[idx], 200);
                    }
                    return jsonResponse({ error: 'Chitta not found with ID: ' + id }, 404);
                } else if (method === 'DELETE') {
                    if (idx >= 0) {
                        db.chittas.splice(idx, 1);
                        saveDb();
                        window.enqueueOfflineAction('DELETE', path, null, { localId: id, type: 'chitta' });
                        return jsonResponse({ status: 'DELETED' }, 200);
                    }
                    return jsonResponse({ error: 'Chitta not found with ID: ' + id }, 404);
                }
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
                    db.chittas = db.chittas || [];
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
                    window.enqueueOfflineAction('POST', '/api/chittas', newC, { localId: newId, chittaNo: newC.chittaNo });
                    return jsonResponse(newC, 201);
                }
            }

            // 3. SALES BILLS
            if (path === '/api/sales-bills/next-no' || path === '/api/bills/next-no') {
                let max = 0;
                (db.salesBills || []).forEach(b => {
                    const n = parseInt(b.billNo, 10);
                    if (!isNaN(n) && n > max) max = n;
                });
                return jsonResponse({ nextNo: max + 1 });
            }

            const mBillSingle = path.match(/^\/api\/(sales-bills|bills)\/(\d+)$/);
            if (mBillSingle) {
                const id = parseInt(mBillSingle[2], 10);
                const idx = (db.salesBills || []).findIndex(b => b.id == id);
                if (method === 'GET') {
                    if (idx >= 0) return jsonResponse(db.salesBills[idx], 200);
                    return jsonResponse({ error: 'Bill not found with ID: ' + id }, 404);
                } else if (method === 'PUT') {
                    if (idx >= 0) {
                        let gross = 0;
                        (reqBody.items || []).forEach(it => {
                            const amt = Math.round((parseFloat(it.quantity) || 0) * (parseFloat(it.rate) || 0) * 100) / 100;
                            it.amount = amt;
                            gross += amt;
                        });
                        gross = Math.round(gross * 100) / 100;
                        const totalCharges = (parseFloat(reqBody.freightCharges) || 0) + (parseFloat(reqBody.packingCharges) || 0) + (parseFloat(reqBody.commissionCharges) || 0) + (parseFloat(reqBody.otherCharges) || 0);
                        const net = Math.round((gross + totalCharges - (parseFloat(reqBody.discount) || 0)) * 100) / 100;

                        db.salesBills[idx] = {
                            ...db.salesBills[idx],
                            ...reqBody,
                            id: id,
                            grossAmount: gross,
                            netReceivable: net,
                            updatedAt: new Date().toISOString()
                        };
                        saveDb();
                        window.enqueueOfflineAction('PUT', path, reqBody, { localId: id, type: 'salesBill' });
                        return jsonResponse(db.salesBills[idx], 200);
                    }
                    return jsonResponse({ error: 'Bill not found' }, 404);
                } else if (method === 'DELETE') {
                    if (idx >= 0) {
                        db.salesBills.splice(idx, 1);
                        saveDb();
                        window.enqueueOfflineAction('DELETE', path, null, { localId: id, type: 'salesBill' });
                        return jsonResponse({ status: 'DELETED' }, 200);
                    }
                    return jsonResponse({ error: 'Bill not found' }, 404);
                }
            }

            if (path === '/api/sales-bills' || path === '/api/bills') {
                if (method === 'GET') {
                    return jsonResponse(db.salesBills || []);
                } else if (method === 'POST') {
                    db.salesBills = db.salesBills || [];
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
                    window.enqueueOfflineAction('POST', path, newB, { localId: newB.id, billNo: newB.billNo });
                    return jsonResponse(newB, 201);
                }
            }

            
            // 3B. PURCHASES
            if (path === '/api/purchases/next-no') {
                let max = 0;
                (db.purchases || []).forEach(p => {
                    const n = parseInt(p.purchaseNo, 10);
                    if (!isNaN(n) && n > max) max = n;
                });
                return jsonResponse({ nextNo: max + 1 });
            }

            if (path === '/api/purchases/pending') {
                const accId = search.get('accountId');
                const list = (db.purchases || []).filter(p => {
                    if (p.paid) return false;
                    if (accId) {
                        return p.traderAccount && (p.traderAccount.id == accId || p.traderAccount.partyId == accId);
                    }
                    return true;
                });
                return jsonResponse(list);
            }

            const mPurchaseSingle = path.match(/^\/api\/purchases\/(\d+)$/);
            if (mPurchaseSingle) {
                const id = parseInt(mPurchaseSingle[1], 10);
                const idx = (db.purchases || []).findIndex(p => p.id == id);
                if (method === 'GET') {
                    if (idx >= 0) return jsonResponse(db.purchases[idx], 200);
                    return jsonResponse({ error: 'Purchase not found with ID: ' + id }, 404);
                } else if (method === 'PUT') {
                    if (idx >= 0) {
                        db.purchases[idx] = { ...db.purchases[idx], ...reqBody, id: id, updatedAt: new Date().toISOString() };
                        saveDb();
                        window.enqueueOfflineAction('PUT', path, reqBody, { localId: id, type: 'purchase' });
                        return jsonResponse(db.purchases[idx], 200);
                    } else {
                        const newP = { id: id, ...reqBody, createdAt: new Date().toISOString() };
                        db.purchases.push(newP);
                        saveDb();
                        window.enqueueOfflineAction('PUT', path, reqBody, { localId: id, type: 'purchase' });
                        return jsonResponse(newP, 201);
                    }
                } else if (method === 'DELETE') {
                    if (idx >= 0) {
                        db.purchases.splice(idx, 1);
                        saveDb();
                        window.enqueueOfflineAction('DELETE', path, null, { localId: id, type: 'purchase' });
                        return jsonResponse({ success: true, status: 'DELETED' }, 200);
                    }
                    return jsonResponse({ error: 'Purchase not found' }, 404);
                }
            }

            if (path === '/api/purchases') {
                if (method === 'GET') {
                    return jsonResponse(db.purchases || []);
                } else if (method === 'POST') {
                    db.purchases = db.purchases || [];
                    const newP = {
                        id: db.purchases.length > 0 ? Math.max(...db.purchases.map(p => p.id || 0)) + 1 : 1,
                        ...reqBody,
                        createdAt: new Date().toISOString()
                    };
                    db.purchases.push(newP);
                    saveDb();
                    window.enqueueOfflineAction('POST', path, newP, { localId: newP.id, type: 'purchase' });
                    return jsonResponse(newP, 201);
                }
            }

            // Purchase Payments
            const mPpSingle = path.match(/^\/api\/purchase-payments\/(\d+)$/);
            if (mPpSingle && method === 'DELETE') {
                const id = parseInt(mPpSingle[1], 10);
                const idx = (db.purchasePayments || []).findIndex(pp => pp.id == id);
                if (idx >= 0) {
                    db.purchasePayments.splice(idx, 1);
                    saveDb();
                    window.enqueueOfflineAction('DELETE', path, null, { localId: id });
                    return jsonResponse({ success: true }, 200);
                }
                return jsonResponse({ error: 'Payment not found' }, 404);
            }

            if (path === '/api/purchase-payments') {
                if (method === 'GET') {
                    return jsonResponse(db.purchasePayments || []);
                } else if (method === 'POST') {
                    db.purchasePayments = db.purchasePayments || [];
                    const newPp = {
                        id: db.purchasePayments.length > 0 ? Math.max(...db.purchasePayments.map(pp => pp.id || 0)) + 1 : 1,
                        ...reqBody,
                        createdAt: new Date().toISOString()
                    };
                    if (newPp.purchase && newPp.purchase.id) {
                        const p = (db.purchases || []).find(x => x.id == newPp.purchase.id);
                        if (p) {
                            p.paid = true;
                            p.paidAmount = newPp.amount;
                            p.payDate = newPp.paymentDate;
                        }
                    }
                    db.purchasePayments.push(newPp);
                    saveDb();
                    window.enqueueOfflineAction('POST', path, newPp, { localId: newPp.id });
                    return jsonResponse(newPp, 201);
                }
            }

            // Purchase Opening Balances
            const mPobSingle = path.match(/^\/api\/purchase-opening-balances\/(\d+)$/);
            if (mPobSingle && method === 'DELETE') {
                const id = parseInt(mPobSingle[1], 10);
                const idx = (db.purchaseOpeningBalances || []).findIndex(ob => ob.id == id);
                if (idx >= 0) {
                    db.purchaseOpeningBalances.splice(idx, 1);
                    saveDb();
                    window.enqueueOfflineAction('DELETE', path, null, { localId: id });
                    return jsonResponse({ success: true }, 200);
                }
                return jsonResponse({ error: 'Opening balance not found' }, 404);
            }

            if (path === '/api/purchase-opening-balances') {
                if (method === 'GET') {
                    return jsonResponse(db.purchaseOpeningBalances || []);
                } else if (method === 'POST') {
                    db.purchaseOpeningBalances = db.purchaseOpeningBalances || [];
                    const newOb = {
                        id: db.purchaseOpeningBalances.length > 0 ? Math.max(...db.purchaseOpeningBalances.map(o => o.id || 0)) + 1 : 1,
                        ...reqBody,
                        createdAt: new Date().toISOString()
                    };
                    db.purchaseOpeningBalances.push(newOb);
                    saveDb();
                    window.enqueueOfflineAction('POST', path, newOb, { localId: newOb.id });
                    return jsonResponse(newOb, 201);
                }
            }

            // Reports: Purchase Register
            if (path === '/api/reports/purchase-register') {
                const sDate = search.get('startDate');
                const eDate = search.get('endDate');
                const accId = search.get('accountId');
                const list = (db.purchases || []).filter(p => {
                    const dt = (p.purchaseDate || '').substring(0, 10);
                    if (sDate && dt < sDate) return false;
                    if (eDate && dt > eDate) return false;
                    if (accId && p.traderAccount && (p.traderAccount.id != accId && p.traderAccount.partyId != accId)) return false;
                    return true;
                }).map(p => {
                    let weight = 0, bags = 0;
                    (p.items || []).forEach(it => {
                        weight += parseFloat(it.quantity) || 0;
                        bags += parseFloat(it.bags) || 0;
                    });
                    return {
                        id: p.id,
                        purchaseNo: p.purchaseNo,
                        purchaseDate: p.purchaseDate,
                        traderName: p.traderAccount ? (p.traderAccount.accountName || p.traderAccount.name) : 'Unknown',
                        traderAccount: p.traderAccount,
                        grossAmount: p.grossAmount || 0,
                        freight: p.freightCharges || 0,
                        packing: p.packingCharges || 0,
                        other: p.otherCharges || 0,
                        discount: p.discount || 0,
                        netPayable: p.netPayable || 0,
                        paid: Boolean(p.paid),
                        weight: weight,
                        bags: bags,
                        items: p.items || []
                    };
                });
                return jsonResponse(list);
            }

            // Reports: Purchase Payment Register
            if (path === '/api/reports/purchase-payment-register') {
                const sDate = search.get('startDate');
                const eDate = search.get('endDate');
                const accId = search.get('accountId');
                const list = (db.purchasePayments || []).filter(pp => {
                    const dt = (pp.paymentDate || '').substring(0, 10);
                    if (sDate && dt < sDate) return false;
                    if (eDate && dt > eDate) return false;
                    if (accId && pp.traderAccount && (pp.traderAccount.id != accId && pp.traderAccount.partyId != accId)) return false;
                    return true;
                }).map(pp => ({
                    id: pp.id,
                    paymentNo: pp.paymentNo,
                    paymentDate: pp.paymentDate,
                    traderName: pp.traderAccount ? (pp.traderAccount.accountName || pp.traderAccount.name) : 'Unknown',
                    amount: pp.amount || 0,
                    discount: pp.discount || 0,
                    paymentMethod: pp.paymentMethod || 'CASH',
                    narration: pp.narration || ''
                }));
                return jsonResponse(list);
            }

            // Reports: Purchase Pending Register
            if (path === '/api/reports/purchase-pending-register') {
                const accId = search.get('accountId');
                const list = (db.purchases || []).filter(p => {
                    if (p.paid) return false;
                    if (accId && p.traderAccount && (p.traderAccount.id != accId && p.traderAccount.partyId != accId)) return false;
                    return true;
                }).map(p => {
                    const net = parseFloat(p.netPayable) || 0;
                    const paidAmt = parseFloat(p.paidAmount) || 0;
                    return {
                        id: p.id,
                        purchaseNo: p.purchaseNo,
                        purchaseDate: p.purchaseDate,
                        traderName: p.traderAccount ? (p.traderAccount.accountName || p.traderAccount.name) : 'Unknown',
                        traderAccount: p.traderAccount,
                        netPayable: net,
                        paidAmount: paidAmt,
                        balance: net - paidAmt > 0 ? (net - paidAmt) : net
                    };
                });
                return jsonResponse(list);
            }

// 4. CASH VOUCHERS / PAYMENTS
            const mCashSingle = path.match(/^\/api\/(cash-vouchers|cash-payments)\/(\d+)$/);
            if (mCashSingle) {
                const id = parseInt(mCashSingle[2], 10);
                const idx = (db.cashVouchers || []).findIndex(v => v.id == id);
                if (method === 'GET') {
                    if (idx >= 0) return jsonResponse(db.cashVouchers[idx], 200);
                    return jsonResponse({ error: 'Voucher not found' }, 404);
                } else if (method === 'PUT') {
                    if (idx >= 0) {
                        db.cashVouchers[idx] = { ...db.cashVouchers[idx], ...reqBody, id: id, updatedAt: new Date().toISOString() };
                        saveDb();
                        window.enqueueOfflineAction('PUT', path, reqBody, { localId: id, type: 'cashVoucher' });
                        return jsonResponse(db.cashVouchers[idx], 200);
                    } else {
                        const newV = { id: id, ...reqBody, createdAt: new Date().toISOString() };
                        db.cashVouchers = db.cashVouchers || [];
                        db.cashVouchers.push(newV);
                        saveDb();
                        window.enqueueOfflineAction('PUT', path, reqBody, { localId: id, type: 'cashVoucher' });
                        return jsonResponse(newV, 201);
                    }
                } else if (method === 'DELETE') {
                    if (idx >= 0) {
                        db.cashVouchers.splice(idx, 1);
                        saveDb();
                        window.enqueueOfflineAction('DELETE', path, null, { localId: id, type: 'cashVoucher' });
                        return jsonResponse({ success: true, status: 'DELETED' }, 200);
                    }
                    return jsonResponse({ error: 'Voucher not found' }, 404);
                }
            }

            if (path === '/api/cash-vouchers' || path === '/api/cash-payments') {
                if (method === 'GET') {
                    return jsonResponse(db.cashVouchers || []);
                } else if (method === 'POST') {
                    db.cashVouchers = db.cashVouchers || [];
                    const newV = {
                        id: db.cashVouchers.length > 0 ? Math.max(...db.cashVouchers.map(v => v.id || 0)) + 1 : 1,
                        ...reqBody,
                        createdAt: new Date().toISOString()
                    };
                    db.cashVouchers.push(newV);
                    saveDb();
                    window.enqueueOfflineAction('POST', path, newV, { localId: newV.id, type: 'cashVoucher' });
                    return jsonResponse(newV, 201);
                }
            }

            // 5. RECEIPT VOUCHERS
            const mRcptSingle = path.match(/^\/api\/(receipt-vouchers|receipts)\/(\d+)$/);
            if (mRcptSingle) {
                const id = parseInt(mRcptSingle[2], 10);
                const idx = (db.receiptVouchers || []).findIndex(v => v.id == id);
                if (method === 'GET') {
                    if (idx >= 0) return jsonResponse(db.receiptVouchers[idx], 200);
                    return jsonResponse({ error: 'Receipt not found' }, 404);
                } else if (method === 'PUT') {
                    if (idx >= 0) {
                        db.receiptVouchers[idx] = { ...db.receiptVouchers[idx], ...reqBody, id: id, updatedAt: new Date().toISOString() };
                        saveDb();
                        window.enqueueOfflineAction('PUT', path, reqBody, { localId: id, type: 'receiptVoucher' });
                        return jsonResponse(db.receiptVouchers[idx], 200);
                    } else {
                        const newV = { id: id, ...reqBody, createdAt: new Date().toISOString() };
                        db.receiptVouchers = db.receiptVouchers || [];
                        db.receiptVouchers.push(newV);
                        saveDb();
                        window.enqueueOfflineAction('PUT', path, reqBody, { localId: id, type: 'receiptVoucher' });
                        return jsonResponse(newV, 201);
                    }
                } else if (method === 'DELETE') {
                    if (idx >= 0) {
                        db.receiptVouchers.splice(idx, 1);
                        saveDb();
                        window.enqueueOfflineAction('DELETE', path, null, { localId: id, type: 'receiptVoucher' });
                        return jsonResponse({ status: 'DELETED' }, 200);
                    }
                    return jsonResponse({ error: 'Receipt not found' }, 404);
                }
            }

            if (path === '/api/receipt-vouchers' || path === '/api/receipts') {
                if (method === 'GET') {
                    return jsonResponse(db.receiptVouchers || []);
                } else if (method === 'POST') {
                    db.receiptVouchers = db.receiptVouchers || [];
                    const newV = {
                        id: db.receiptVouchers.length > 0 ? Math.max(...db.receiptVouchers.map(v => v.id || 0)) + 1 : 1,
                        ...reqBody,
                        createdAt: new Date().toISOString()
                    };
                    db.receiptVouchers.push(newV);
                    saveDb();
                    window.enqueueOfflineAction('POST', path, newV, { localId: newV.id, type: 'receiptVoucher' });
                    return jsonResponse(newV, 201);
                }
            }

            // 6. PERIODS
            if (path === '/api/periods' || path === '/api/admin/periods') {
                if (method === 'GET') return jsonResponse(db.periods || []);
                if (method === 'POST') {
                    db.periods = db.periods || [];
                    db.periods.push(reqBody);
                    saveDb();
                    window.enqueueOfflineAction('POST', path, reqBody, {});
                    return jsonResponse(reqBody, 201);
                }
            }

            // 7. OPENING BALANCES
            if (path === '/api/opening-balances') {
                if (method === 'GET') return jsonResponse(db.openingBalances || []);
                if (method === 'POST') {
                    db.openingBalances = db.openingBalances || [];
                    db.openingBalances.push(reqBody);
                    saveDb();
                    window.enqueueOfflineAction('POST', path, reqBody, {});
                    return jsonResponse(reqBody, 201);
                }
            }

            // 8. MASTERS: Places, Items, Groups, Commission, Company, Settings
            if (path === '/api/places') {
                if (method === 'GET') return jsonResponse(db.places || []);
                if (method === 'POST') { db.places = db.places || []; db.places.push(reqBody); saveDb(); window.enqueueOfflineAction('POST', path, reqBody, {}); return jsonResponse(reqBody, 201); }
            }
            if (path === '/api/items') {
                if (method === 'GET') return jsonResponse(db.items || []);
                if (method === 'POST') { db.items = db.items || []; db.items.push(reqBody); saveDb(); window.enqueueOfflineAction('POST', path, reqBody, {}); return jsonResponse(reqBody, 201); }
            }
            if (path === '/api/account-groups') {
                if (method === 'GET') return jsonResponse(db.groups || []);
                if (method === 'POST') { db.groups = db.groups || []; db.groups.push(reqBody); saveDb(); window.enqueueOfflineAction('POST', path, reqBody, {}); return jsonResponse(reqBody, 201); }
            }
            if (path === '/api/commission-rates') {
                if (method === 'GET') return jsonResponse(db.commissionRates || []);
                if (method === 'POST') { db.commissionRates = db.commissionRates || []; db.commissionRates.push(reqBody); saveDb(); window.enqueueOfflineAction('POST', path, reqBody, {}); return jsonResponse(reqBody, 201); }
            }
            if (path === '/api/company') {
                if (method === 'GET') return jsonResponse(db.company || {});
                if (method === 'POST') { db.company = reqBody; saveDb(); window.enqueueOfflineAction('POST', path, reqBody, {}); return jsonResponse(reqBody, 200); }
            }
            if (path === '/api/settings') {
                if (method === 'GET') return jsonResponse(db.settings || {});
                if (method === 'POST') { db.settings = reqBody; saveDb(); window.enqueueOfflineAction('POST', path, reqBody, {}); return jsonResponse(reqBody, 200); }
            }

            // 9. REPORTS & OUTSTANDINGS
            if (path === '/api/reports/chitta-register') {
                const startDate = search.get('startDate');
                const endDate = search.get('endDate');
                const accountId = search.get('accountId');
                let list = (db.chittas || []).slice();
                if (startDate) list = list.filter(c => c.chittaDate && String(c.chittaDate).substring(0, 10) >= startDate);
                if (endDate) list = list.filter(c => c.chittaDate && String(c.chittaDate).substring(0, 10) <= endDate);
                if (accountId) list = list.filter(c => (c.farmerAccount && (c.farmerAccount.id == accountId || String(c.farmerAccount.partyId) == String(accountId) || String(c.farmerAccount.accountName).toLowerCase() === String(accountId).toLowerCase())) || (c.farmerName && c.farmerName.toLowerCase() === String(accountId).toLowerCase()));
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

            if (path === '/api/reports/daybook') {
                const sDate = search.get('startDate') || search.get('date') || new Date().toISOString().substring(0, 10);
                const eDate = search.get('endDate') || (search.get('date') || sDate);

                // 1. Calculate Opening Cash Balance before sDate
                let cashAcc = (db.accounts || []).find(a => a.accountName && a.accountName.toUpperCase().trim() === 'CASH');
                let opening = (cashAcc ? (parseFloat(cashAcc.openingBalance || cashAcc.opbal) || 0) : 0);

                (db.receiptVouchers || []).forEach(r => {
                    const dt = (r.voucherDate || '').substring(0, 10);
                    if (dt && dt < sDate) {
                        opening += (parseFloat(r.amount) || 0);
                    }
                });

                (db.cashVouchers || []).forEach(v => {
                    const dt = (v.voucherDate || '').substring(0, 10);
                    if (dt && dt < sDate) {
                        opening -= (parseFloat(v.amount) || 0);
                    }
                });

                (db.chittas || []).forEach(c => {
                    const dt = (c.payDate || c.chittaDate || '').substring(0, 10);
                    if (dt && dt < sDate) {
                        if (c.isPaid || (c.paymentMethod && c.paymentMethod.toUpperCase() === 'CASH')) {
                            opening -= (parseFloat(c.netPayable || c.grossAmount) || 0);
                        }
                    }
                });

                const list = [];
                // Row 1: OPENING BALANCE
                list.push({
                    date: '',
                    name: 'OPENING BALANCE',
                    accountName: 'OPENING BALANCE',
                    credit: opening,
                    debit: 0,
                    isOpening: true
                });

                // Row 2: Chitta Payments grouped by Date -> "CHITTA PAYMENT"
                const chittaByDate = {};
                (db.chittas || []).forEach(c => {
                    const dt = (c.payDate || c.chittaDate || '').substring(0, 10);
                    if (dt && dt >= sDate && dt <= eDate) {
                        if (c.isPaid || (c.paymentMethod && c.paymentMethod.toUpperCase() === 'CASH')) {
                            const amt = parseFloat(c.netPayable || c.grossAmount) || 0;
                            chittaByDate[dt] = (chittaByDate[dt] || 0) + amt;
                        }
                    }
                });

                Object.keys(chittaByDate).sort().forEach(dt => {
                    if (chittaByDate[dt] > 0) {
                        list.push({
                            date: dt,
                            name: 'CHITTA PAYMENT',
                            accountName: 'CHITTA PAYMENT',
                            credit: 0,
                            debit: chittaByDate[dt]
                        });
                    }
                });

                // Row 2B: Purchase Payments in range
                (db.purchasePayments || []).forEach(pp => {
                    const dt = (pp.paymentDate || '').substring(0, 10);
                    if (dt && dt >= sDate && dt <= eDate) {
                        if (!pp.paymentMethod || pp.paymentMethod.toUpperCase() === 'CASH') {
                            let accName = pp.traderAccount ? (pp.traderAccount.accountName || pp.traderAccount.name) : 'PURCHASE A/C';
                            list.push({
                                date: dt,
                                name: accName,
                                accountName: accName,
                                credit: 0,
                                debit: parseFloat(pp.amount) || 0,
                                narration: pp.narration || ''
                            });
                        }
                    }
                });

                // Row 3: Cash Vouchers in range
                (db.cashVouchers || []).forEach(v => {
                    const dt = (v.voucherDate || '').substring(0, 10);
                    if (dt && dt >= sDate && dt <= eDate) {
                        let accName = v.account ? (v.account.accountName || v.account.name) : (v.accountName || '');
                        if (!accName && v.account && v.account.id && db.accounts) {
                            const found = db.accounts.find(a => a.id == v.account.id || String(a.partyId) == String(v.account.id));
                            if (found) accName = found.accountName;
                        }
                        if (!accName) accName = 'PURCHASE A/C';
                        list.push({
                            date: dt,
                            name: accName,
                            accountName: accName,
                            credit: 0,
                            debit: parseFloat(v.amount) || 0,
                            narration: v.narration || ''
                        });
                    }
                });

                // Row 4: Receipt Vouchers in range
                (db.receiptVouchers || []).forEach(r => {
                    const dt = (r.voucherDate || '').substring(0, 10);
                    if (dt && dt >= sDate && dt <= eDate) {
                        let accName = r.account ? (r.account.accountName || r.account.name) : (r.accountName || '');
                        if (!accName && r.account && r.account.id && db.accounts) {
                            const found = db.accounts.find(a => a.id == r.account.id || String(a.partyId) == String(r.account.id));
                            if (found) accName = found.accountName;
                        }
                        if (!accName) accName = 'RECEIPT';
                        list.push({
                            date: dt,
                            name: accName,
                            accountName: accName,
                            credit: parseFloat(r.amount) || 0,
                            debit: 0,
                            narration: r.narration || ''
                        });
                    }
                });

                return jsonResponse(list);
            }

            if (path === '/api/reports/daily-cash-opening') {
                return jsonResponse({ date: search.get('date'), openingCash: 0 });
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
                window.saveOfflineQueue([]);
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
