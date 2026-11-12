
// FoxPro Grid Navigation & Calculation Logic
const ROWS = 5;

document.addEventListener("DOMContentLoaded", () => {
    const tbody = document.getElementById("gridBody");
    
    for(let i=1; i<=ROWS; i++) {
        let tr = document.createElement("tr");
        tr.innerHTML = `
            <td style="text-align:center">${i}</td>
            <td><input type="text" class="cell" data-row="${i}" data-col="1" id="item_${i}"></td>
            <td><input type="number" class="cell qty" data-row="${i}" data-col="2" id="qty_${i}" oninput="calcRow(${i})"></td>
            <td><input type="number" class="cell rate" data-row="${i}" data-col="3" id="rate_${i}" oninput="calcRow(${i})"></td>
            <td><input type="number" class="cell amt" data-row="${i}" data-col="4" id="amt_${i}" readonly style="background:#e9e9e9;text-align:right;"></td>
        `;
        tbody.appendChild(tr);
    }

    setupKeyboardNav();
});

function calcRow(row) {
    const qty = parseFloat(document.getElementById(`qty_${row}`).value) || 0;
    const rate = parseFloat(document.getElementById(`rate_${row}`).value) || 0;
    const amt = qty * rate;
    
    document.getElementById(`amt_${row}`).value = amt.toFixed(2);
    calculateFooter();
}

function calculateFooter() {
    let gross = 0;
    for(let i=1; i<=ROWS; i++) {
        gross += parseFloat(document.getElementById(`amt_${i}`).value) || 0;
    }
    
    const deductions = parseFloat(document.getElementById("deductions").value) || 0;
    const net = gross - deductions;

    document.getElementById("lblGross").innerText = gross.toFixed(2);
    document.getElementById("lblNet").innerText = net.toFixed(2);
}

function setupKeyboardNav() {
    // Arrow keys and Enter navigation mimicking FoxPro Grid behavior
    document.addEventListener("keydown", function(e) {
        if(e.key === "F5") { e.preventDefault(); saveChitta(); }
        
        if(!e.target.classList.contains("cell")) return;
        
        let row = parseInt(e.target.dataset.row);
        let col = parseInt(e.target.dataset.col);

        if(e.key === "ArrowDown" || e.key === "Enter") {
            e.preventDefault();
            let next = document.querySelector(`.cell[data-row="${row+1}"][data-col="${col}"]`);
            if(!next && e.key === "Enter") next = document.querySelector(`.cell[data-row="${row}"][data-col="${col+1}"]`); // Move right on enter if no row below
            if(next) next.focus();
        } 
        else if (e.key === "ArrowUp") {
            e.preventDefault();
            let prev = document.querySelector(`.cell[data-row="${row-1}"][data-col="${col}"]`);
            if(prev) prev.focus();
        }
        else if (e.key === "ArrowRight") {
            e.preventDefault();
            let next = document.querySelector(`.cell[data-row="${row}"][data-col="${col+1}"]`);
            if(next) next.focus();
        }
        else if (e.key === "ArrowLeft") {
            e.preventDefault();
            let prev = document.querySelector(`.cell[data-row="${row}"][data-col="${col-1}"]`);
            if(prev) prev.focus();
        }
    });
}

function saveChitta() {
    alert("Saving Chitta with Net Payable: ₹" + document.getElementById("lblNet").innerText + "\n(In production, this fires an AJAX POST to /api/chittas)");
}
