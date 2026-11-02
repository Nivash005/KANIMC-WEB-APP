# KANIMC Logic Restoration — Phase 1

This build keeps the existing UI and Spring Boot architecture and restores the Chitta workflow.

## Restored
- Server-side Chitta validation.
- Farmer and item reference hydration.
- Gross = sum(quantity × rate).
- Commission rate → calculated commission amount.
- Net = gross − commission − advance − freight.
- Duplicate Chitta number protection for create/update.
- Chitta list filtering by date/account.
- Chitta create/update/delete API wiring.
- Existing UI uses relative `/api/...` URLs instead of hard-coded localhost URLs for Chitta.
- Chitta Find searches Chitta number or farmer.
- Register totals now include advance and freight.

## VFP evidence used
The recovered `chitr.DBF` schema contains `TOTAL`, `COMA`, `ADV`, `NET`, and `FRA`, matching the deduction structure above. The recovered application is DBF/cursor based and uses document code 4 in the accounting area.

## Important
The exact VFP account-code mapping for cash/commission/advance/freight is not fully recoverable from printable strings alone. Therefore the web API does not hard-code account IDs; accounting accounts can be supplied explicitly. This avoids corrupting the ledger while the remaining VFP mapping is reconstructed.
