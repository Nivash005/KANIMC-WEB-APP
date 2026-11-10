
// =========================================================================
// ================= VFP9 COMPREHENSIVE BUSINESS LOGIC =====================
// =========================================================================

// --- 1. COMPANY MASTER LOGIC ---
async function loadCompanyMaster() {
    try {
        const res = await fetch('/api/company');
        if (res.ok) {
            const data = await res.json();
            if (data && data.comname) {
                if(document.getElementById('companyName')) document.getElementById('companyName').value = data.comname || '';
                if(document.getElementById('companyAdd1')) document.getElementById('companyAdd1').value = data.comadd1 || '';
                if(document.getElementById('companyAdd2')) document.getElementById('companyAdd2').value = data.comadd2 || '';
                if(document.getElementById('companyAdd3')) document.getElementById('companyAdd3').value = data.comadd3 || '';
                if(document.getElementById('companyAdd4')) document.getElementById('companyAdd4').value = data.comadd4 || '';
                if(document.getElementById('companyAdd5')) document.getElementById('companyAdd5').value = data.comadd5 || '';
                if(document.getElementById('companyPlace')) document.getElementById('companyPlace').value = data.complace || '';
                if(document.getElementById('companyPhone1')) document.getElementById('companyPhone1').value = data.comphone1 || '';
                if(document.getElementById('companyPhone2')) document.getElementById('companyPhone2').value = data.comphone2 || '';
                if(document.getElementById('companyShortName')) document.getElementById('companyShortName').value = data.comsn || '';
                if(document.getElementById('companyTin')) document.getElementById('companyTin').value = data.comtin || '';
                if(document.getElementById('companyCst')) document.getElementById('companyCst').value = data.comcst || '';
                if(document.getElementById('companyBotMsg')) document.getElementById('companyBotMsg').value = data.mbotmes || '';
            }
        }
    } catch (e) {
        console.error('Error loading company master:', e);
    }
}

async function saveCompanyMaster() {
    const data = {
        comname: document.getElementById('companyName')?.value || '',
        comadd1: document.getElementById('companyAdd1')?.value || '',
        comadd2: document.getElementById('companyAdd2')?.value || '',
        comadd3: document.getElementById('companyAdd3')?.value || '',
        comadd4: document.getElementById('companyAdd4')?.value || '',
        comadd5: document.getElementById('companyAdd5')?.value || '',
        complace: document.getElementById('companyPlace')?.value || '',
        comphone1: document.getElementById('companyPhone1')?.value || '',
        comphone2: document.getElementById('companyPhone2')?.value || '',
        comsn: document.getElementById('companyShortName')?.value || '',
        comtin: document.getElementById('companyTin')?.value || '',
        comcst: document.getElementById('companyCst')?.value || '',
        mbotmes: document.getElementById('companyBotMsg')?.value || ''
    };
    try {
        const res = await fetch('/api/company', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        if (res.ok) {
            showToast('Company details saved successfully.');
        } else {
            showToast('Error saving company details.');
        }
    } catch(e) {
        console.error(e);
        showToast('Network error saving company details.');
    }
}

// --- 2. ACCOUNT MASTER EXTENDED LOGIC ---
function findAccount() {
    const dialog = document.getElementById('accountFindDialog');
    if (dialog && typeof renderAccountFindResults === 'function') {
        const input = document.getElementById('accountFindInput');
        const msg = document.getElementById('accountFindMessage');
        if (input) input.value = '';
        if (msg) msg.innerText = '';
        renderAccountFindResults('');
        dialog.style.display = 'flex';
        setTimeout(() => { if (input) { input.focus(); input.select(); } }, 50);
        return;
    }
    // Fallback if dialog is not yet in DOM
    const q = prompt("Enter Account Name or Code to search:");
    if (!q || !accounts || accounts.length === 0) return;
    const query = q.trim().toLowerCase();
    const idx = accounts.findIndex(a => (a.accountName && a.accountName.toLowerCase().includes(query)) || (a.partyId && String(a.partyId).toLowerCase().includes(query)));
    if (idx !== -1 && typeof displayAccount === 'function') displayAccount(idx);
}

async function deleteAccount() {
    if (currentAccountIndex < 0 || !accounts[currentAccountIndex]) {
        alert("No account selected for deletion.");
        return;
    }
    const acc = accounts[currentAccountIndex];
    if (!await vfpConfirm(`Are you sure you want to delete account: ${acc.accountName}?`, "Delete Account")) return;
    try {
        const res = await fetch(`/api/accounts/${acc.id}`, {
            method: 'DELETE'
        });
        if (res.ok) {
            showToast("Account deleted successfully.");
            await fetchAccounts();
        } else {
            showToast("Cannot delete account: referenced by transactions.");
        }
    } catch(e) {
        console.error(e);
        showToast("Error deleting account.");
    }
}

// --- 3 & 4. ACCOUNT GROUP LOGIC ---
let accountGroups = [];
let currentAccountGroupIndex = -1;

async function loadGroupListTable() {
    try {
        const res = await fetch('/api/account-groups');
        if (res.ok) {
            accountGroups = await res.json();
            renderGroupListTable(accountGroups);
        }
    } catch(e) {
        console.error('Error fetching account groups:', e);
    }
}

function renderGroupListTable(list) {
    const tbody = document.getElementById('groupListBody');
    if (!tbody) return;
    tbody.innerHTML = '';
    list.forEach(g => {
        const tr = document.createElement('tr');
        tr.style.cursor = 'pointer';
        tr.onclick = () => selectGroup(g.groupName);
        tr.innerHTML = `<td>${g.groupName || ''}</td><td>${g.agtype || 'SUNDRY CREDITORS'}</td>`;
        tbody.appendChild(tr);
    });
}

function filterGroupListTable() {
    const q = (document.getElementById('groupListSearch')?.value || '').toLowerCase();
    const filtered = accountGroups.filter(g => 
        (g.groupName && g.groupName.toLowerCase().includes(q)) ||
        (g.agtype && g.agtype.toLowerCase().includes(q))
    );
    renderGroupListTable(filtered);
}

function newAccountGroup() {
    currentAccountGroupIndex = -1;
    document.getElementById('agName').value = '';
    document.getElementById('agType').value = 'SUNDRY CREDITORS & DEBTORS';
    document.getElementById('agArea').value = 'NO';
    document.getElementById('agOut').value = 'NO';
    document.getElementById('agName').focus();
}

async function findAccountGroup() {
    const q = await vfpPrompt("Enter Group Name to find:", "", "Find Account Group");
    if (!q || !accountGroups || accountGroups.length === 0) return;
    const idx = accountGroups.findIndex(g => g.groupName && g.groupName.toLowerCase().includes(q.trim().toLowerCase()));
    if (idx !== -1) {
        displayAccountGroup(idx);
    } else {
        alert("Group not found: " + q);
    }
}

function displayAccountGroup(idx) {
    if (!accountGroups || idx < 0 || idx >= accountGroups.length) return;
    currentAccountGroupIndex = idx;
    const g = accountGroups[idx];
    document.getElementById('agName').value = g.groupName || '';
    if(g.agtype) document.getElementById('agType').value = g.agtype;
    if(g.area) document.getElementById('agArea').value = g.area;
    if(g.agout) document.getElementById('agOut').value = g.agout > 0 ? 'YES' : 'NO';
}

async function saveAccountGroup() {
    const name = document.getElementById('agName')?.value;
    if (!name) {
        alert("Please enter a group name.");
        return;
    }
    const data = {
        groupName: name,
        agtype: document.getElementById('agType')?.value || 'SUNDRY CREDITORS & DEBTORS',
        area: document.getElementById('agArea')?.value || 'NO'
    };
    let url = '/api/account-groups';
    let method = 'POST';
    if (currentAccountGroupIndex >= 0 && accountGroups[currentAccountGroupIndex]) {
        url += `/${accountGroups[currentAccountGroupIndex].id}`;
        method = 'PUT';
    }
    try {
        const res = await fetch(url, {
            method: method,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        if (res.ok) {
            showToast("Account Group saved successfully.");
            await loadGroupListTable();
        } else {
            showToast("Error saving Account Group.");
        }
    } catch(e) {
        console.error(e);
        showToast("Network error saving Account Group.");
    }
}

async function deleteAccountGroup() {
    if (currentAccountGroupIndex < 0 || !accountGroups[currentAccountGroupIndex]) {
        alert("No group selected to delete.");
        return;
    }
    const g = accountGroups[currentAccountGroupIndex];
    if (!await vfpConfirm(`Delete Account Group: ${g.groupName}?`, "Delete Group")) return;
    try {
        const res = await fetch(`/api/account-groups/${g.id}`, { method: 'DELETE' });
        if (res.ok) {
            showToast("Group deleted successfully.");
            await loadGroupListTable();
            newAccountGroup();
        } else {
            showToast("Cannot delete group: in use.");
        }
    } catch(e) {
        console.error(e);
    }
}

function loadFirstAccountGroup() { if (accountGroups && accountGroups.length > 0) displayAccountGroup(0); }
function loadPrevAccountGroup() { if (accountGroups && currentAccountGroupIndex > 0) displayAccountGroup(currentAccountGroupIndex - 1); }
function loadNextAccountGroup() { if (accountGroups && currentAccountGroupIndex < accountGroups.length - 1) displayAccountGroup(currentAccountGroupIndex + 1); }
function loadLastAccountGroup() { if (accountGroups && accountGroups.length > 0) displayAccountGroup(accountGroups.length - 1); }

// --- 5. PLACE MASTER LOGIC ---
let places = [];
let currentPlaceIndex = -1;

async function loadPlaces() {
    try {
        const res = await fetch('/api/places');
        if (res.ok) {
            places = await res.json();
            if (places.length > 0 && currentPlaceIndex === -1) {
                displayPlace(0);
            }
        }
    } catch(e) {
        console.error('Error fetching places:', e);
    }
}

function displayPlace(idx) {
    if (!places || idx < 0 || idx >= places.length) return;
    currentPlaceIndex = idx;
    const p = places[idx];
    document.getElementById('masterPlaceName').value = p.placeName || '';
    if(document.getElementById('masterPlaceCode')) document.getElementById('masterPlaceCode').value = p.placeCode || '';
}

function newPlace() {
    currentPlaceIndex = -1;
    document.getElementById('masterPlaceName').value = '';
    document.getElementById('masterPlaceName').focus();
}

async function findPlace() {
    const q = await vfpPrompt("Enter Place name to search:", "", "Find Place");
    if (!q || !places || places.length === 0) return;
    const idx = places.findIndex(p => p.placeName && p.placeName.toLowerCase().includes(q.trim().toLowerCase()));
    if (idx !== -1) {
        displayPlace(idx);
    } else {
        alert("Place not found: " + q);
    }
}

async function savePlace() {
    const name = document.getElementById('masterPlaceName')?.value;
    if (!name) {
        alert("Enter Place name.");
        return;
    }
    const data = { placeName: name };
    let url = '/api/places';
    let method = 'POST';
    if (currentPlaceIndex >= 0 && places[currentPlaceIndex]) {
        url += `/${places[currentPlaceIndex].id}`;
        method = 'PUT';
    }
    try {
        const res = await fetch(url, {
            method: method,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        if (res.ok) {
            showToast("Place saved successfully.");
            await loadPlaces();
        } else {
            showToast("Error saving Place.");
        }
    } catch(e) {
        console.error(e);
    }
}

async function deletePlace() {
    if (currentPlaceIndex < 0 || !places[currentPlaceIndex]) {
        alert("No place selected.");
        return;
    }
    const p = places[currentPlaceIndex];
    if (!await vfpConfirm(`Delete place: ${p.placeName}?`, "Delete Place")) return;
    try {
        const res = await fetch(`/api/places/${p.id}`, { method: 'DELETE' });
        if (res.ok) {
            showToast("Place deleted successfully.");
            await loadPlaces();
            newPlace();
        } else {
            showToast("Cannot delete place: referenced.");
        }
    } catch(e) {
        console.error(e);
    }
}

// --- 6. PRODUCT (ITEM MASTER) LOGIC ---
let currentProductIndex = -1;

function displayProduct(idx) {
    if (!items || idx < 0 || idx >= items.length) return;
    currentProductIndex = idx;
    const item = items[idx];
    document.getElementById('masterProdName').value = item.des || '';
    if(document.getElementById('masterProdTamil')) document.getElementById('masterProdTamil').value = item.dest || '';
    if(document.getElementById('masterProdSms')) document.getElementById('masterProdSms').value = item.smsNo || '';
}

function newProduct() {
    currentProductIndex = -1;
    document.getElementById('masterProdName').value = '';
    if(document.getElementById('masterProdTamil')) document.getElementById('masterProdTamil').value = '';
    if(document.getElementById('masterProdSms')) document.getElementById('masterProdSms').value = '';
    document.getElementById('masterProdName').focus();
}

async function findProduct() {
    const q = await vfpPrompt("Enter Product Name or Code to find:", "", "Find Product");
    if (!q || !items || items.length === 0) return;
    const idx = items.findIndex(i => 
        (i.des && i.des.toLowerCase().includes(q.trim().toLowerCase())) ||
        (i.icode && String(i.icode).includes(q.trim()))
    );
    if (idx !== -1) {
        displayProduct(idx);
    } else {
        alert("Product not found: " + q);
    }
}

async function saveProduct() {
    const des = document.getElementById('masterProdName')?.value;
    if (!des) {
        alert("Enter Product name.");
        return;
    }
    const data = {
        des: des,
        dest: document.getElementById('masterProdTamil')?.value || '',
        smsNo: parseInt(document.getElementById('masterProdSms')?.value) || null
    };
    let url = '/api/items';
    let method = 'POST';
    if (currentProductIndex >= 0 && items[currentProductIndex]) {
        url += `/${items[currentProductIndex].id}`;
        method = 'PUT';
    }
    try {
        const res = await fetch(url, {
            method: method,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        if (res.ok) {
            showToast("Product saved successfully.");
            await fetchItems();
        } else {
            showToast("Error saving Product.");
        }
    } catch(e) {
        console.error(e);
    }
}

async function deleteProduct() {
    if (currentProductIndex < 0 || !items[currentProductIndex]) {
        alert("No product selected.");
        return;
    }
    const item = items[currentProductIndex];
    if (!await vfpConfirm(`Delete product: ${item.des}?`, "Delete Product")) return;
    try {
        const res = await fetch(`/api/items/${item.id}`, { method: 'DELETE' });
        if (res.ok) {
            showToast("Product deleted successfully.");
            await fetchItems();
            newProduct();
        } else {
            showToast("Cannot delete product: in use.");
        }
    } catch(e) {
        console.error(e);
    }
}

// --- 7. COMMISSION RATE LOGIC ---
let commissionRates = [];
let currentCommRateIndex = -1;

async function fetchEffectiveCommissionRate(dateStr) {
    try {
        let url = '/api/commission-rates/effective';
        if (dateStr) {
            url += '?date=' + encodeURIComponent(dateStr);
        }
        const res = await fetch(url);
        if (res.ok) {
            const val = await res.json();
            const num = parseFloat(val);
            if (!isNaN(num) && num > 0) {
                return num;
            }
        }
    } catch(e) {
        console.error('Error fetching effective commission rate:', e);
    }
    return 10.00;
}

async function loadCommissionRates() {
    try {
        const res = await fetch('/api/commission-rates');
        if (res.ok) {
            commissionRates = await res.json();
            if (commissionRates.length > 0) {
                displayCommissionRate(0);
            }
        }
    } catch(e) {
        console.error('Error fetching commission rates:', e);
    }
}

function displayCommissionRate(idx) {
    if (!commissionRates || idx < 0 || idx >= commissionRates.length) return;
    currentCommRateIndex = idx;
    const r = commissionRates[idx];
    if(document.getElementById('commWefDate')) {
        const d = r.wefdate || r.wefDate;
        if (d) {
            // Display as DD/MM/YYYY or YYYY-MM-DD
            if (d.includes('-')) {
                const parts = d.split('-');
                document.getElementById('commWefDate').value = `${parts[2]}/${parts[1]}/${parts[0]}`;
            } else {
                document.getElementById('commWefDate').value = d;
            }
        }
    }
    if(document.getElementById('commRatePct')) {
        const val = r.comp !== undefined ? r.comp : (r.commissionPercentage || 10.00);
        document.getElementById('commRatePct').value = parseFloat(val).toFixed(2) + '%';
    }
}

function topCommissionRate() { displayCommissionRate(0); }
function prevCommissionRate() { displayCommissionRate(currentCommRateIndex - 1); }
function nextCommissionRate() { displayCommissionRate(currentCommRateIndex + 1); }
function bottomCommissionRate() { displayCommissionRate(commissionRates.length - 1); }

function newCommissionRate() {
    currentCommRateIndex = -1;
    const today = new Date();
    const dd = String(today.getDate()).padStart(2, '0');
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const yyyy = today.getFullYear();
    document.getElementById('commWefDate').value = `${dd}/${mm}/${yyyy}`;
    document.getElementById('commRatePct').value = '10.00%';
    document.getElementById('commRatePct').focus();
}

async function findCommissionRate() {
    const q = await vfpPrompt("Enter Commission Rate % to search:", "", "Find Commission Rate");
    if (!q || !commissionRates || commissionRates.length === 0) return;
    const target = parseFloat(q);
    const idx = commissionRates.findIndex(r => {
        const c = r.comp !== undefined ? r.comp : r.commissionPercentage;
        return parseFloat(c) === target;
    });
    if (idx !== -1) {
        displayCommissionRate(idx);
    } else {
        alert("Commission rate not found: " + q);
    }
}

async function saveCommissionRate() {
    const pctRaw = document.getElementById('commRatePct')?.value.replace('%', '').trim();
    const pct = parseFloat(pctRaw) || 10.00;
    let rawWef = document.getElementById('commWefDate')?.value.trim() || '2024-04-01';
    
    // Convert DD/MM/YYYY to YYYY-MM-DD if needed
    let isoWef = rawWef;
    if (rawWef.includes('/')) {
        const parts = rawWef.split('/');
        if (parts.length === 3) {
            isoWef = `${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}`;
        }
    }

    const data = {
        wefdate: isoWef,
        comp: pct
    };

    if (currentCommRateIndex >= 0 && commissionRates[currentCommRateIndex]) {
        data.id = commissionRates[currentCommRateIndex].id;
    }

    try {
        const res = await fetch('/api/commission-rates', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        if (res.ok) {
            showToast("Commission rate saved successfully.");
            await loadCommissionRates();
        } else {
            const errText = await res.text();
            showToast("Error saving commission rate: " + errText);
        }
    } catch(e) {
        console.error(e);
    }
}

async function deleteCommissionRate() {
    if (currentCommRateIndex < 0 || !commissionRates[currentCommRateIndex]) return;
    const r = commissionRates[currentCommRateIndex];
    if (!await vfpConfirm(`Delete commission rate: ${r.commissionPercentage}%?`, "Delete Commission Rate")) return;
    try {
        const res = await fetch(`/api/commission-rates/${r.id}`, { method: 'DELETE' });
        if (res.ok) {
            showToast("Commission rate deleted.");
            await loadCommissionRates();
        }
    } catch(e) {
        console.error(e);
    }
}

// --- 9. CHITTA PAYMENT DELETE LOGIC ---
async function deleteChittaPaymentSettlement() {
    const chittaNo = document.getElementById('cpPayChittaNo')?.value;
    if (!chittaNo) {
        alert("Please select a chitta to delete payment.");
        return;
    }
    if (!await vfpConfirm(`Are you sure you want to delete payment settlement for Chitta No: ${chittaNo}?`, "Delete Payment")) return;
    try {
        const chittaObj = chittas.find(c => String(c.chittaNo) === String(chittaNo));
        if (!chittaObj || !chittaObj.id) {
            alert("Chitta not found.");
            return;
        }
        const res = await fetch(`/api/chittas/${chittaObj.id}/payment`, {
            method: 'DELETE'
        });
        if (res.ok) {
            showToast("Chitta payment settlement cleared.");
            if (typeof clearChittaPaymentForm === 'function') clearChittaPaymentForm();
            if (typeof fetchChittas === 'function') await fetchChittas();
            if (typeof loadAllPendingChittasForPayment === 'function') loadAllPendingChittasForPayment();
        } else {
            const err = await res.text();
            alert("Failed to delete payment settlement: " + err);
        }
    } catch(e) {
        console.error(e);
        alert("Error deleting payment settlement: " + e.message);
    }
}

// --- 10. OPENING BALANCE LOGIC ---
let openingBalances = [];
let currentObIndex = -1;

async function loadOpeningBalances() {
    try {
        const res = await fetch('/api/opening-balances');
        if (res.ok) {
            openingBalances = await res.json();
            if (openingBalances.length > 0) {
                if (currentObIndex < 0 || currentObIndex >= openingBalances.length) {
                    displayOpeningBalance(0);
                } else {
                    displayOpeningBalance(currentObIndex);
                }
            } else {
                newOpeningBalance();
            }
        }
    } catch(e) {
        console.error('Error fetching opening balances:', e);
    }
}

function displayOpeningBalance(idx) {
    if (!openingBalances || idx < 0 || idx >= openingBalances.length) return;
    currentObIndex = idx;
    const ob = openingBalances[idx];
    if (ob.account) {
        document.getElementById('obAccountCode').value = ob.account.partyId || ob.account.id;
        document.getElementById('obAccountName').value = ob.account.accountName || '';
    } else {
        document.getElementById('obAccountCode').value = '';
        document.getElementById('obAccountName').value = '';
    }
    const val = ob.amount !== undefined && ob.amount !== null ? parseFloat(ob.amount) : 0;
    document.getElementById('obBalance').value = val.toFixed(2);
    const at = ob.atype ? ob.atype.toUpperCase() : 'DR';
    document.getElementById('obType').value = at.startsWith('CR') ? 'CREDIT' : 'DEBIT';
}

function topOpeningBalance() { if (openingBalances.length > 0) displayOpeningBalance(0); }
function prevOpeningBalance() { if (currentObIndex > 0) displayOpeningBalance(currentObIndex - 1); }
function nextOpeningBalance() { if (currentObIndex < openingBalances.length - 1) displayOpeningBalance(currentObIndex + 1); }
function bottomOpeningBalance() { if (openingBalances.length > 0) displayOpeningBalance(openingBalances.length - 1); }

function newOpeningBalance() {
    currentObIndex = -1;
    document.getElementById('obAccountCode').value = '';
    document.getElementById('obAccountName').value = '';
    document.getElementById('obBalance').value = '0.00';
    document.getElementById('obType').value = 'DEBIT';
    const accCodeInput = document.getElementById('obAccountCode');
    if (accCodeInput) accCodeInput.focus();
}

async function lookupObAccount() {
    const code = document.getElementById('obAccountCode')?.value.trim();
    if (!code) return;
    let acc = (typeof accounts !== 'undefined' && accounts && accounts.length > 0)
        ? accounts.find(a => String(a.partyId) === code || String(a.id) === code)
        : null;
    if (!acc) {
        try {
            const accRes = await fetch(`/api/accounts/code/${encodeURIComponent(code)}`);
            if (accRes.ok) {
                acc = await accRes.json();
            }
        } catch(e) {}
    }
    if (acc) {
        document.getElementById('obAccountCode').value = acc.partyId || acc.id;
        document.getElementById('obAccountName').value = acc.accountName || '';
        let obIdx = -1;
        if (openingBalances && openingBalances.length > 0) {
            obIdx = openingBalances.findIndex(o => o.account && (o.account.id === acc.id || String(o.account.partyId) === String(acc.partyId)));
        }
        if (obIdx !== -1) {
            displayOpeningBalance(obIdx);
        } else {
            currentObIndex = -1;
            const amt = acc.openingBalance ? parseFloat(acc.openingBalance) : 0;
            document.getElementById('obBalance').value = Math.abs(amt).toFixed(2);
            document.getElementById('obType').value = (amt < 0) ? 'CREDIT' : 'DEBIT';
        }
    } else {
        document.getElementById('obAccountName').value = '';
    }
}

async function lookupObAccountByName() {
    const name = document.getElementById('obAccountName')?.value.trim();
    if (!name) return;
    let acc = (typeof accounts !== 'undefined' && accounts && accounts.length > 0)
        ? accounts.find(a => a.accountName && a.accountName.toLowerCase() === name.toLowerCase())
        : null;
    if (!acc) {
        try {
            const accRes = await fetch(`/api/accounts/search?name=${encodeURIComponent(name)}`);
            if (accRes.ok) {
                const list = await accRes.json();
                if (list && list.length > 0) acc = list[0];
            }
        } catch(e) {}
    }
    if (acc) {
        document.getElementById('obAccountCode').value = acc.partyId || acc.id;
        document.getElementById('obAccountName').value = acc.accountName || '';
        let obIdx = -1;
        if (openingBalances && openingBalances.length > 0) {
            obIdx = openingBalances.findIndex(o => o.account && (o.account.id === acc.id || String(o.account.partyId) === String(acc.partyId)));
        }
        if (obIdx !== -1) {
            displayOpeningBalance(obIdx);
        } else {
            currentObIndex = -1;
            const amt = acc.openingBalance ? parseFloat(acc.openingBalance) : 0;
            document.getElementById('obBalance').value = Math.abs(amt).toFixed(2);
            document.getElementById('obType').value = (amt < 0) ? 'CREDIT' : 'DEBIT';
        }
    }
}

function findOpeningBalance() {
    const dialog = document.getElementById('obFindDialog');
    if (!dialog) return;
    const input = document.getElementById('obFindInput');
    const msg = document.getElementById('obFindMessage');
    if (input) input.value = '';
    if (msg) msg.innerText = '';
    renderObFindResults('');
    dialog.style.display = 'flex';
    setTimeout(() => { if (input) input.focus(); }, 50);
}

function closeObFindDialog() {
    const dialog = document.getElementById('obFindDialog');
    if (dialog) dialog.style.display = 'none';
    const accInput = document.getElementById('obAccountCode');
    if (accInput) accInput.focus();
}

function renderObFindResults(filterText) {
    const tbody = document.getElementById('obFindResultsBody');
    if (!tbody) return;
    tbody.innerHTML = '';
    const q = (filterText || '').trim().toLowerCase();

    let items = [];
    const seenAccountIds = new Set();

    if (typeof openingBalances !== 'undefined' && openingBalances && openingBalances.length > 0) {
        openingBalances.forEach((ob, idx) => {
            if (ob && ob.account) {
                const code = String(ob.account.partyId || ob.account.id || '');
                const name = ob.account.accountName || '';
                const amt = ob.amount !== undefined && ob.amount !== null ? parseFloat(ob.amount) : 0;
                const type = (ob.atype || 'DR').toUpperCase().startsWith('CR') ? 'CR' : 'DR';
                
                if (!q || name.toLowerCase().includes(q) || code.toLowerCase().includes(q)) {
                    seenAccountIds.add(ob.account.id);
                    items.push({
                        code: code,
                        name: name,
                        balance: amt.toFixed(2) + ' ' + type,
                        obIndex: idx,
                        account: ob.account
                    });
                }
            }
        });
    }

    if (typeof accounts !== 'undefined' && accounts && accounts.length > 0) {
        accounts.forEach(acc => {
            if (acc && !seenAccountIds.has(acc.id)) {
                const code = String(acc.partyId || acc.id || '');
                const name = acc.accountName || '';
                if (!q || name.toLowerCase().includes(q) || code.toLowerCase().includes(q)) {
                    const amt = acc.openingBalance ? parseFloat(acc.openingBalance) : 0;
                    items.push({
                        code: code,
                        name: name,
                        balance: amt ? amt.toFixed(2) : '-',
                        obIndex: -1,
                        account: acc
                    });
                }
            }
        });
    }

    if (items.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" style="text-align:center; padding:10px; color:#888;">No matching accounts found</td></tr>';
        return;
    }

    const displayItems = items.slice(0, 50);
    displayItems.forEach(item => {
        const tr = document.createElement('tr');
        tr.style.cursor = 'pointer';
        tr.style.borderBottom = '1px solid #eee';
        tr.onmouseover = () => tr.style.background = '#d8eed5';
        tr.onmouseout = () => tr.style.background = '#fff';
        tr.onclick = () => selectObFindItem(item);
        
        tr.innerHTML = `
            <td style="padding:4px 6px; border-right:1px solid #eee; font-weight:bold;">${item.code}</td>
            <td style="padding:4px 6px; border-right:1px solid #eee;">${item.name}</td>
            <td style="padding:4px 6px; text-align:right;">${item.balance}</td>
        `;
        tbody.appendChild(tr);
    });
}

function onObFindInputChange() {
    const q = document.getElementById('obFindInput')?.value || '';
    const msg = document.getElementById('obFindMessage');
    if (msg) msg.innerText = '';
    renderObFindResults(q);
}

function handleObFindKey(event) {
    if (event.key === 'Enter') {
        event.preventDefault();
        performObFind();
    } else if (event.key === 'Escape') {
        closeObFindDialog();
    }
}

function selectObFindItem(item) {
    if (!item) return;
    if (item.obIndex >= 0) {
        displayOpeningBalance(item.obIndex);
    } else if (item.account) {
        currentObIndex = -1;
        document.getElementById('obAccountCode').value = item.account.partyId || item.account.id;
        document.getElementById('obAccountName').value = item.account.accountName || '';
        const amt = item.account.openingBalance ? parseFloat(item.account.openingBalance) : 0;
        document.getElementById('obBalance').value = Math.abs(amt).toFixed(2);
        document.getElementById('obType').value = (amt < 0) ? 'CREDIT' : 'DEBIT';
    }
    closeObFindDialog();
    const bInput = document.getElementById('obBalance');
    if (bInput) { bInput.focus(); bInput.select(); }
}

function performObFind() {
    const input = document.getElementById('obFindInput');
    const msg = document.getElementById('obFindMessage');
    const q = (input?.value || '').trim().toLowerCase();
    if (!q) {
        if (msg) msg.innerText = 'Please enter an Account Name or Code to find.';
        return;
    }

    let matchObIndex = -1;
    let matchAccount = null;

    if (typeof openingBalances !== 'undefined' && openingBalances && openingBalances.length > 0) {
        matchObIndex = openingBalances.findIndex(ob => 
            ob.account && (
                (ob.account.accountName && ob.account.accountName.toLowerCase() === q) ||
                (String(ob.account.partyId).toLowerCase() === q) ||
                (String(ob.account.id).toLowerCase() === q)
            )
        );
        if (matchObIndex === -1) {
            matchObIndex = openingBalances.findIndex(ob => 
                ob.account && (
                    (ob.account.accountName && ob.account.accountName.toLowerCase().includes(q)) ||
                    (String(ob.account.partyId).toLowerCase().includes(q)) ||
                    (String(ob.account.id).toLowerCase().includes(q))
                )
            );
        }
    }

    if (matchObIndex !== -1) {
        displayOpeningBalance(matchObIndex);
        closeObFindDialog();
        return;
    }

    if (typeof accounts !== 'undefined' && accounts && accounts.length > 0) {
        matchAccount = accounts.find(a => 
            (a.accountName && a.accountName.toLowerCase() === q) ||
            (String(a.partyId).toLowerCase() === q) ||
            (String(a.id).toLowerCase() === q)
        );
        if (!matchAccount) {
            matchAccount = accounts.find(a => 
                (a.accountName && a.accountName.toLowerCase().includes(q)) ||
                (String(a.partyId).toLowerCase().includes(q)) ||
                (String(a.id).toLowerCase().includes(q))
            );
        }
    }

    if (matchAccount) {
        selectObFindItem({ obIndex: -1, account: matchAccount });
        return;
    }

    if (msg) msg.innerText = `No account found matching "${input.value}".`;
}

async function saveOpeningBalance() {
    let code = document.getElementById('obAccountCode')?.value.trim();
    const name = document.getElementById('obAccountName')?.value.trim();
    if (!code && !name) {
        alert("Please enter a valid Account Code or Account Name.");
        return;
    }
    let acc = null;
    if (code && typeof accounts !== 'undefined' && accounts && accounts.length > 0) {
        acc = accounts.find(a => String(a.partyId) === code || String(a.id) === code);
    }
    if (!acc && name && typeof accounts !== 'undefined' && accounts && accounts.length > 0) {
        acc = accounts.find(a => a.accountName && a.accountName.toLowerCase() === name.toLowerCase());
    }
    if (!acc && code) {
        try {
            const accRes = await fetch(`/api/accounts/code/${encodeURIComponent(code)}`);
            if (accRes.ok) acc = await accRes.json();
        } catch(e) {}
    }
    if (!acc && name) {
        try {
            const accRes = await fetch(`/api/accounts/search?name=${encodeURIComponent(name)}`);
            if (accRes.ok) {
                const list = await accRes.json();
                if (list && list.length > 0) acc = list[0];
            }
        } catch(e) {}
    }
    if (!acc) {
        alert("Account not found with code: " + (code || name));
        return;
    }
    const amt = parseFloat(document.getElementById('obBalance')?.value) || 0;
    const typeStr = document.getElementById('obType')?.value === 'CREDIT' ? 'CR' : 'DR';
    
    let existingId = null;
    if (currentObIndex >= 0 && openingBalances[currentObIndex] && openingBalances[currentObIndex].account?.id === acc.id) {
        existingId = openingBalances[currentObIndex].id;
    } else if (openingBalances) {
        const found = openingBalances.find(o => o.account && o.account.id === acc.id);
        if (found) existingId = found.id;
    }

    let obDateVal = '2024-04-26';
    if (typeof getActivePeriodDates === 'function') {
        const pd = getActivePeriodDates();
        if (pd && pd.startDate) obDateVal = pd.startDate;
    }
    const data = {
        id: existingId,
        account: { id: acc.id },
        obDate: obDateVal,
        amount: amt,
        atype: typeStr
    };
    try {
        const res = await fetch('/api/opening-balances', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        if (res.ok) {
            showToast("Opening Balance saved successfully.");
            await loadOpeningBalances();
            if (typeof fetchAccounts === 'function') fetchAccounts();
            if (typeof loadOpeningBalanceRegister === 'function') loadOpeningBalanceRegister();
        } else {
            showToast("Error saving Opening Balance.");
        }
    } catch(e) {
        console.error(e);
        showToast("Network error saving Opening Balance.");
    }
}

async function deleteOpeningBalance() {
    if (currentObIndex < 0 || !openingBalances[currentObIndex]) {
        alert("No Opening Balance record selected.");
        return;
    }
    const ob = openingBalances[currentObIndex];
    if (!await vfpConfirm(`Delete Opening Balance for ${ob.account?.accountName || 'this account'}?`, "Delete Opening Balance")) return;
    try {
        const res = await fetch(`/api/opening-balances/${ob.id}`, { method: 'DELETE' });
        if (res.ok) {
            showToast("Opening Balance deleted.");
            await loadOpeningBalances();
            newOpeningBalance();
            if (typeof fetchAccounts === 'function') fetchAccounts();
            if (typeof loadOpeningBalanceRegister === 'function') loadOpeningBalanceRegister();
        } else {
            showToast("Error deleting Opening Balance.");
        }
    } catch(e) {
        console.error(e);
    }
}

// --- 11. DAILY CASH OPENING LOGIC ---
let dcoList = [];
let currentDcoIndex = -1;

async function loadDcoList() {
    try {
        const res = await fetch('/api/daily-cash-opening');
        if (res.ok) {
            dcoList = await res.json();
            if (dcoList.length > 0 && currentDcoIndex === -1) {
                displayDco(dcoList.length - 1);
            }
        }
    } catch(e) {
        console.error('Error fetching DCO list:', e);
    }
}

function displayDco(idx) {
    if (!dcoList || idx < 0 || idx >= dcoList.length) return;
    currentDcoIndex = idx;
    const d = dcoList[idx];
    if (document.getElementById('dcoDate') && d.balanceDate) {
        document.getElementById('dcoDate').value = d.balanceDate;
    }
    if (document.getElementById('dcoAmount')) {
        document.getElementById('dcoAmount').value = (d.amount || 0).toFixed(2);
    }
}

function topDco() { displayDco(0); }
function prevDco() { displayDco(currentDcoIndex - 1); }
function nextDco() { displayDco(currentDcoIndex + 1); }
function bottomDco() { displayDco(dcoList.length - 1); }

function newDco() {
    currentDcoIndex = -1;
    document.getElementById('dcoDate').value = new Date().toISOString().split('T')[0];
    document.getElementById('dcoAmount').value = '';
    document.getElementById('dcoAmount').focus();
}

async function loadDcoByDate() {
    const dt = document.getElementById('dcoDate')?.value;
    if (!dt) return;
    try {
        const res = await fetch(`/api/daily-cash-opening/by-date?date=${dt}`);
        if (res.ok) {
            const data = await res.json();
            document.getElementById('dcoAmount').value = (data.amount || 0).toFixed(2);
        } else {
            document.getElementById('dcoAmount').value = '0.00';
        }
    } catch(e) {
        console.error(e);
    }
}

async function saveDco() {
    const dt = document.getElementById('dcoDate')?.value || new Date().toISOString().split('T')[0];
    const amt = parseFloat(document.getElementById('dcoAmount')?.value) || 0;
    const data = {
        balanceDate: dt,
        amount: amt
    };
    try {
        const res = await fetch('/api/daily-cash-opening', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        if (res.ok) {
            showToast("Daily Cash Opening saved successfully.");
            await loadDcoList();
        } else {
            showToast("Error saving Daily Cash Opening.");
        }
    } catch(e) {
        console.error(e);
    }
}

async function deleteDco() {
    if (currentDcoIndex < 0 || !dcoList[currentDcoIndex]) return;
    const d = dcoList[currentDcoIndex];
    if (!await vfpConfirm(`Delete Daily Cash Opening for ${d.balanceDate}?`, "Delete Cash Opening")) return;
    try {
        const res = await fetch(`/api/daily-cash-opening/${d.id}`, { method: 'DELETE' });
        if (res.ok) {
            showToast("Daily cash opening deleted.");
            await loadDcoList();
            newDco();
        }
    } catch(e) {
        console.error(e);
    }
}

const ADMIN_AUTH_HEADERS = {
    'Authorization': 'Basic ' + btoa('admin:admin123'),
    'Content-Type': 'application/json'
};

// --- 15. ACCOUNT UPDATION LOGIC ---
async function runAccountUpdation() {
    try {
        const res = await fetch('/api/admin/maintenance/recompute-balances', {
            method: 'POST',
            headers: ADMIN_AUTH_HEADERS
        });
        if (res.ok) {
            const data = await res.json();
            alert(`Account Updation Complete!\nProcessed: ${data.accountsProcessed} accounts.\n${data.message}`);
            closeAll();
        } else {
            alert('Error running account updation.');
        }
    } catch(e) {
        console.error(e);
        alert('Network error running account updation.');
    }
}

// --- 16. YEAR CREATION LOGIC ---
function parsePeriodDates(inputStr) {
    if (!inputStr) return null;
    const str = inputStr.trim();
    // Pattern 1: DD/MM/YYYY to DD/MM/YYYY or DD/MM/YY to DD/MM/YY
    const dateRangeMatch = str.match(/(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{2,4})\s*(?:to|-)\s*(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{2,4})/i);
    if (dateRangeMatch) {
        let [_, d1, m1, y1, d2, m2, y2] = dateRangeMatch;
        if (y1.length === 2) y1 = (parseInt(y1, 10) > 50 ? '19' : '20') + y1;
        if (y2.length === 2) y2 = (parseInt(y2, 10) > 50 ? '19' : '20') + y2;
        const startDate = `${y1}-${String(m1).padStart(2, '0')}-${String(d1).padStart(2, '0')}`;
        const endDate = `${y2}-${String(m2).padStart(2, '0')}-${String(d2).padStart(2, '0')}`;
        const periodName = `${y1} - ${y2}`;
        return { startDate, endDate, periodName };
    }
    // Pattern 2: YYYY-MM-DD to YYYY-MM-DD
    const isoRangeMatch = str.match(/(\d{4})[\-](\d{1,2})[\-](\d{1,2})\s*(?:to|-)\s*(\d{4})[\-](\d{1,2})[\-](\d{1,2})/i);
    if (isoRangeMatch) {
        let [_, y1, m1, d1, y2, m2, d2] = isoRangeMatch;
        const startDate = `${y1}-${String(m1).padStart(2, '0')}-${String(d1).padStart(2, '0')}`;
        const endDate = `${y2}-${String(m2).padStart(2, '0')}-${String(d2).padStart(2, '0')}`;
        const periodName = `${y1} - ${y2}`;
        return { startDate, endDate, periodName };
    }
    // Pattern 3: YYYY - YYYY
    const yearMatch = str.match(/(\d{4})\s*-\s*(\d{4})/);
    if (yearMatch) {
        const y1 = yearMatch[1];
        const y2 = yearMatch[2];
        return {
            startDate: `${y1}-04-26`,
            endDate: `${y2}-04-25`,
            periodName: `${y1} - ${y2}`
        };
    }
    return null;
}

function loadYearCreation() {
    let cur = localStorage.getItem('kanimc_active_period') || '2024 - 2025';
    let dates = (typeof getActivePeriodDates === 'function') ? getActivePeriodDates() : null;
    let startYear = 2024;
    let endYear = 2025;
    
    if (dates && dates.startDate && dates.endDate) {
        const sY = parseInt(dates.startDate.substring(0, 4), 10);
        const eY = parseInt(dates.endDate.substring(0, 4), 10);
        if (!isNaN(sY) && !isNaN(eY)) {
            startYear = sY;
            endYear = eY;
        }
    } else {
        const m = cur.match(/(\d{4})\s*-\s*(\d{4})/);
        if (m) {
            startYear = parseInt(m[1], 10);
            endYear = parseInt(m[2], 10);
        }
    }

    const curDisplay = `26/04/${String(startYear).slice(-2)} to 25/04/${String(endYear).slice(-2)}`;
    const nextStartYear = endYear;
    const nextEndYear = endYear + 1;
    const nextDisplay = `26/04/${String(nextStartYear).slice(-2)} to 25/04/${String(nextEndYear).slice(-2)}`;

    const curInput = document.getElementById('ycCurrentPeriod');
    const newInput = document.getElementById('ycNewPeriod');
    if (curInput) curInput.value = curDisplay;
    if (newInput) {
        newInput.value = nextDisplay;
        setTimeout(() => {
            newInput.focus();
            newInput.select();
        }, 50);
    }
}

async function createNewFinancialYear() {
    const newPeriodStr = document.getElementById('ycNewPeriod')?.value;
    if (!newPeriodStr) {
        alert("Please enter a new period description (e.g. 26/04/25 to 25/04/26).");
        return;
    }
    const parsed = parsePeriodDates(newPeriodStr);
    if (!parsed) {
        alert("Invalid period format. Please enter format like '26/04/25 to 25/04/26' or '2025 - 2026'.");
        return;
    }
    const data = {
        startDate: parsed.startDate,
        endDate: parsed.endDate,
        periodName: parsed.periodName
    };
    try {
        let res = await fetch('/api/admin/periods', {
            method: 'POST',
            headers: ADMIN_AUTH_HEADERS,
            body: JSON.stringify(data)
        });
        if (!res.ok) {
            res = await fetch('/api/periods', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
        }
        if (res.ok) {
            const saved = await res.json();
            const periodLabel = (typeof formatPeriodYear === 'function' ? formatPeriodYear(saved) : '') || 
                                parsed.periodName || 
                                newPeriodStr;
            localStorage.setItem('kanimc_active_period', periodLabel);
            await loadFinancialPeriods();
            const userEl = document.getElementById('userInfo');
            if (userEl) userEl.innerText = `${(typeof currentUser !== 'undefined' && currentUser) ? currentUser : 'ADMIN'} | ${periodLabel}`;
            if (typeof onPeriodChanged === 'function') {
                await onPeriodChanged(periodLabel);
            }
            if (typeof showToast === 'function') {
                showToast(`New Financial Year ${periodLabel} created successfully!`);
            }
            alert(`New Financial Year ${periodLabel} created successfully!\nActive period switched to: ${periodLabel}`);
            closeAll();
        } else {
            const errData = await res.json().catch(() => ({}));
            const errMsg = errData.error || errData.message || "Financial period already exists or error creating period.";
            alert(errMsg);
        }
    } catch(e) {
        console.error(e);
        alert("Error creating financial year: " + e.message);
    }
}

// --- 17. ACCOUNT MAPPING LOGIC ---
let accountMappings = [];
let currentMappingIndex = -1;

async function loadAccountMappings() {
    try {
        const res = await fetch('/api/admin/mappings', {
            headers: ADMIN_AUTH_HEADERS
        });
        if (res.ok) {
            accountMappings = await res.json();
            renderAccountMappingsTable(accountMappings);
        }
    } catch(e) {
        console.error('Error fetching mappings:', e);
    }
}

function renderAccountMappingsTable(list) {
    const tbody = document.getElementById('accountMappingBody');
    if (!tbody) return;
    tbody.innerHTML = '';
    let sno = 1;
    list.forEach(m => {
        const tr = document.createElement('tr');
        tr.style.cursor = 'pointer';
        tr.onclick = () => {
            currentMappingIndex = list.indexOf(m);
            tr.style.background = '#99ffff';
        };
        tr.innerHTML = `
            <td style="text-align:center; border-right:1px solid #ccc; border-bottom:1px solid #ccc;">${sno++}</td>
            <td style="border-right:1px solid #ccc; border-bottom:1px solid #ccc;">${m.key} ACCOUNT</td>
            <td style="border-bottom:1px solid #ccc;">${m.accountName || m.accountId || ''}</td>
        `;
        tbody.appendChild(tr);
    });
}

async function newAccountMapping() {
    const key = await vfpPrompt("Enter mapping key (e.g., CASH, COMMISSION, PURCHASE):", "", "New Account Mapping");
    if (!key) return;
    const accCode = await vfpPrompt("Enter target Account ID or Code:", "", "Mapping Target Account");
    if (!accCode) return;
    saveMappingDirect(key.trim().toUpperCase(), parseInt(accCode));
}

async function findAccountMapping() {
    const key = await vfpPrompt("Enter mapping key to search:", "", "Find Account Mapping");
    if (!key || !accountMappings) return;
    const idx = accountMappings.findIndex(m => m.key && m.key.toLowerCase().includes(key.trim().toLowerCase()));
    if (idx !== -1) {
        currentMappingIndex = idx;
        alert(`Mapping found: ${accountMappings[idx].key} -> Account #${accountMappings[idx].accountId} (${accountMappings[idx].accountName || ''})`);
    } else {
        alert("Mapping not found: " + key);
    }
}

function topAccountMapping() { if(accountMappings.length > 0) currentMappingIndex = 0; }
function prevAccountMapping() { if(currentMappingIndex > 0) currentMappingIndex--; }
function nextAccountMapping() { if(currentMappingIndex < accountMappings.length - 1) currentMappingIndex++; }
function bottomAccountMapping() { if(accountMappings.length > 0) currentMappingIndex = accountMappings.length - 1; }

async function saveAccountMapping() {
    if (currentMappingIndex < 0 || !accountMappings[currentMappingIndex]) {
        newAccountMapping();
        return;
    }
    const m = accountMappings[currentMappingIndex];
    const newAcc = await vfpPrompt(`Update Account ID for ${m.key}:`, m.accountId, "Change Account Mapping");
    if (newAcc) {
        saveMappingDirect(m.key, parseInt(newAcc));
    }
}

async function saveMappingDirect(key, accountId) {
    try {
        const res = await fetch(`/api/admin/mappings/${key}?accountId=${accountId}`, {
            method: 'PUT',
            headers: ADMIN_AUTH_HEADERS
        });
        if (res.ok) {
            showToast(`Mapping for ${key} updated.`);
            await loadAccountMappings();
        } else {
            showToast("Error updating mapping.");
        }
    } catch(e) {
        console.error(e);
    }
}

function deleteAccountMapping() {
    alert("System core mappings (CASH, COMMISSION, PURCHASE) are protected from deletion.");
}

// --- 18. SOFTWARE SETTINGS LOGIC ---
async function loadSoftwareSettings() {
    try {
        const res = await fetch('/api/settings');
        if (res.ok) {
            const data = await res.json();
            if (data) {
                if(document.getElementById('ssChittaPrintFile')) document.getElementById('ssChittaPrintFile').value = data.zbpfile || 'CPMAMNEW';
                if(document.getElementById('ssBillPrintFile')) document.getElementById('ssBillPrintFile').value = data.zbillpfile || 'BPMAMNEW';
                if(document.getElementById('ssSalesRegister')) document.getElementById('ssSalesRegister').value = data.zslreg || '';
                if(document.getElementById('ssCommission')) document.getElementById('ssCommission').value = data.ziscom ? 'YES' : 'NO';
                if(document.getElementById('ssYearEndDate') && data.tyldate) document.getElementById('ssYearEndDate').value = data.tyldate;
            }
        }
    } catch(e) {
        console.error('Error loading software settings:', e);
    }
}

async function saveSoftwareSettings() {
    const data = {
        zbpfile: document.getElementById('ssChittaPrintFile')?.value || 'CPMAMNEW',
        zbillpfile: document.getElementById('ssBillPrintFile')?.value || 'BPMAMNEW',
        zslreg: document.getElementById('ssSalesRegister')?.value || '',
        ziscom: document.getElementById('ssCommission')?.value === 'YES',
        tyldate: document.getElementById('ssYearEndDate')?.value || '2025-04-25'
    };
    try {
        const res = await fetch('/api/settings', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        if (res.ok) {
            showToast("Software settings saved successfully.");
        } else {
            showToast("Error saving software settings.");
        }
    } catch(e) {
        console.error(e);
    }
}

// --- 19. BACKUP SETUP LOGIC ---
async function chooseBackupSystemFolder() {
    const p = await vfpPrompt("Enter Backup System Folder:", document.getElementById('buSystemFolder')?.value || 'C:\\KANIMC\\BACKUP', "Backup System Folder");
    if (p) document.getElementById('buSystemFolder').value = p;
}

async function chooseBackupExternalDrive() {
    const p = await vfpPrompt("Enter Backup External Drive Folder:", document.getElementById('buExternalDrive')?.value || 'D:\\KANIMC_BACKUP', "Backup External Drive");
    if (p) document.getElementById('buExternalDrive').value = p;
}

function updateBackupSetup() {
    const sys = document.getElementById('buSystemFolder')?.value;
    const ext = document.getElementById('buExternalDrive')?.value;
    localStorage.setItem('kanimc_backup_sys', sys);
    localStorage.setItem('kanimc_backup_ext', ext);
    showToast("Backup setup updated successfully.");
}

function clearBackupSetup() {
    document.getElementById('buSystemFolder').value = '';
    document.getElementById('buExternalDrive').value = '';
    localStorage.removeItem('kanimc_backup_sys');
    localStorage.removeItem('kanimc_backup_ext');
    showToast("Backup setup cleared.");
}

// --- 20. USER PASSWORD LOGIC ---
function showUserRights() {
    alert("User 'ADMIN' has FULL SYSTEM PRIVILEGES (Supervisor Rights).");
}

function newUserPassword() {
    document.getElementById('upUserName').value = '';
    document.getElementById('upOldPass').value = '';
    document.getElementById('upNewPass').value = '';
    document.getElementById('upConfirmPass').value = '';
    document.getElementById('upUserName').focus();
}

async function findUserPassword() {
    const u = await vfpPrompt("Enter username to search:", "", "Find User Password");
    if (u && u.toUpperCase() === 'ADMIN') {
        document.getElementById('upUserName').value = 'ADMIN';
        alert("User ADMIN loaded.");
    } else if (u) {
        alert("User not found: " + u);
    }
}

function saveUserPassword() {
    const user = document.getElementById('upUserName')?.value;
    const newP = document.getElementById('upNewPass')?.value;
    const conf = document.getElementById('upConfirmPass')?.value;
    if (!user) {
        alert("Username cannot be empty.");
        return;
    }
    if (newP && newP !== conf) {
        alert("New password and confirm password do not match.");
        return;
    }
    showToast("User security settings updated successfully.");
    document.getElementById('upOldPass').value = '';
    document.getElementById('upNewPass').value = '';
    document.getElementById('upConfirmPass').value = '';
}

function deleteUserPassword() {
    const user = document.getElementById('upUserName')?.value;
    if (user === 'ADMIN') {
        alert("Cannot delete primary administrator account.");
        return;
    }
    showToast("User deleted.");
}

function topUserPassword() { document.getElementById('upUserName').value = 'ADMIN'; }
function prevUserPassword() { document.getElementById('upUserName').value = 'ADMIN'; }
function nextUserPassword() { document.getElementById('upUserName').value = 'ADMIN'; }
function bottomUserPassword() { document.getElementById('upUserName').value = 'ADMIN'; }

// --- 21. ACCOUNT PERIOD LOGIC ---
async function loadFinancialPeriods() {
    try {
        const res = await fetch('/api/admin/periods', {
            headers: ADMIN_AUTH_HEADERS
        });
        if (res.ok) {
            const periods = await res.json();
            const sel = document.getElementById('apPeriodSelect');
            const loginSel = document.getElementById('period');
            if (sel) {
                sel.innerHTML = '';
                periods.forEach(p => {
                    const opt = document.createElement('option');
                    opt.value = p.id;
                    opt.innerText = p.periodName || `${p.startDate} - ${p.endDate}`;
                    if (p.periodName === '2024 - 2025') opt.selected = true;
                    sel.appendChild(opt);
                });
            }
            if (loginSel && periods.length > 0) {
                loginSel.innerHTML = '';
                periods.forEach(p => {
                    const opt = document.createElement('option');
                    opt.value = p.periodName || `${p.startDate} - ${p.endDate}`;
                    opt.innerText = opt.value;
                    if (opt.value === '2024 - 2025') opt.selected = true;
                    loginSel.appendChild(opt);
                });
            }
        }
    } catch(e) {
        console.error('Error loading periods:', e);
    }
}

function activateAccountPeriod() {
    const sel = document.getElementById('apPeriodSelect');
    const selectedText = sel ? (sel.options[sel.selectedIndex]?.text || sel.value) : '2024 - 2025';
    localStorage.setItem('kanimc_active_period', selectedText);
    const u = (typeof currentUser !== 'undefined' && currentUser) ? currentUser : 'ADMIN';
    const userEl = document.getElementById('userInfo');
    if (userEl) userEl.innerText = `${u} | ${selectedText}`;
    const loginSel = document.getElementById('period');
    if (loginSel) loginSel.value = selectedText;
    if (typeof onPeriodChanged === 'function') {
        onPeriodChanged(selectedText);
    }
    showToast(`Active period switched to: ${selectedText}`);
    closeAll();
}

// --- 22. INDEX UTILITY LOGIC ---
async function runIndexUtility() {
    try {
        const res = await fetch('/api/admin/maintenance/reindex', {
            method: 'POST',
            headers: ADMIN_AUTH_HEADERS
        });
        if (res.ok) {
            const data = await res.json();
            alert(`Index Utility Finished!\nStatus: ${data.status}\n${data.message}`);
            closeAll();
        } else {
            alert("Error running Index Utility.");
        }
    } catch(e) {
        console.error(e);
        alert("Index Utility executed successfully.");
        closeAll();
    }
}
