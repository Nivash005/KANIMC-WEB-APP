-- KANIMC Modern Web Architecture Schema (PostgreSQL/MySQL Compatible)

CREATE TABLE companies (
    id SERIAL PRIMARY KEY,
    firm_name VARCHAR(255) NOT NULL,
    regional_name VARCHAR(255),
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    city VARCHAR(100),
    pin_code VARCHAR(20),
    phone VARCHAR(50),
    gstin VARCHAR(50),
    mandi_license VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE financial_years (
    id SERIAL PRIMARY KEY,
    year_code VARCHAR(10) NOT NULL UNIQUE, -- e.g. 2425
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE account_groups (
    id SERIAL PRIMARY KEY,
    group_code INT NOT NULL UNIQUE,
    group_name VARCHAR(100) NOT NULL,
    parent_group_id INT REFERENCES account_groups(id)
);

CREATE TABLE places (
    id SERIAL PRIMARY KEY,
    place_code VARCHAR(20) UNIQUE NOT NULL,
    english_name VARCHAR(100) NOT NULL,
    regional_name VARCHAR(100)
);

CREATE TABLE accounts (
    id SERIAL PRIMARY KEY,
    party_id VARCHAR(50) UNIQUE NOT NULL, -- custid
    account_name VARCHAR(255) NOT NULL, -- an
    regional_name VARCHAR(255), -- ant
    group_code INT NOT NULL REFERENCES account_groups(group_code),
    place_id INT REFERENCES places(id),
    phone1 VARCHAR(20),
    phone2 VARCHAR(20),
    phone3 VARCHAR(20),
    is_trader BOOLEAN DEFAULT false, -- slyes
    is_farmer BOOLEAN DEFAULT false, -- puryes
    opening_balance DECIMAL(14,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false
);

CREATE TABLE items (
    id SERIAL PRIMARY KEY,
    commodity_code VARCHAR(20) UNIQUE NOT NULL,
    description VARCHAR(100) NOT NULL,
    regional_description VARCHAR(100),
    sms_code VARCHAR(20)
);

CREATE TABLE commission_rates (
    id SERIAL PRIMARY KEY,
    item_id INT REFERENCES items(id),
    wef_date DATE NOT NULL,
    commission_percentage DECIMAL(5,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chittas (
    id SERIAL PRIMARY KEY,
    chitta_no VARCHAR(50) NOT NULL,
    chitta_date DATE NOT NULL,
    farmer_account_id INT REFERENCES accounts(id),
    financial_year_id INT REFERENCES financial_years(id),
    advance_deduction DECIMAL(14,2) DEFAULT 0.00,
    freight_deduction DECIMAL(14,2) DEFAULT 0.00,
    commission_deduction DECIMAL(14,2) DEFAULT 0.00,
    other_deductions DECIMAL(14,2) DEFAULT 0.00,
    gross_amount DECIMAL(14,2) NOT NULL,
    net_payable DECIMAL(14,2) NOT NULL,
    pay_date DATE,
    paid_to VARCHAR(100),
    pay_tno VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INT REFERENCES users(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    UNIQUE (chitta_no, financial_year_id)
);

CREATE TABLE chitta_items (
    id SERIAL PRIMARY KEY,
    chitta_id INT REFERENCES chittas(id) ON DELETE CASCADE,
    item_id INT REFERENCES items(id),
    quantity DECIMAL(14,2) NOT NULL,
    rate DECIMAL(14,2) NOT NULL,
    amount DECIMAL(14,2) NOT NULL
);

CREATE TABLE sales_bills (
    id SERIAL PRIMARY KEY,
    bill_no VARCHAR(50) NOT NULL,
    bill_date DATE NOT NULL,
    buyer_account_id INT REFERENCES accounts(id),
    financial_year_id INT REFERENCES financial_years(id),
    commission_charges DECIMAL(14,2) DEFAULT 0.00,
    packing_charges DECIMAL(14,2) DEFAULT 0.00,
    freight_charges DECIMAL(14,2) DEFAULT 0.00,
    other_charges DECIMAL(14,2) DEFAULT 0.00,
    discount DECIMAL(14,2) DEFAULT 0.00,
    gross_amount DECIMAL(14,2) NOT NULL,
    net_receivable DECIMAL(14,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INT REFERENCES users(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    UNIQUE(bill_no, financial_year_id)
);

CREATE TABLE sales_bill_items (
    id SERIAL PRIMARY KEY,
    sales_bill_id INT REFERENCES sales_bills(id) ON DELETE CASCADE,
    item_id INT REFERENCES items(id),
    bags INT DEFAULT 0,
    quantity DECIMAL(14,2) NOT NULL,
    rate DECIMAL(14,2) NOT NULL,
    amount DECIMAL(14,2) NOT NULL
);

CREATE TABLE account_transactions (
    id SERIAL PRIMARY KEY,
    transaction_date DATE NOT NULL,
    account_id INT REFERENCES accounts(id),
    doc_code INT NOT NULL, -- 1=Receipt, 2=Payment, 4=Chitta, 6=Sales, O=Opening
    doc_no VARCHAR(50),
    doc_ref_id INT, -- Links to Chitta/SalesBill/Voucher ID
    debit_amount DECIMAL(14,2) DEFAULT 0.00,
    credit_amount DECIMAL(14,2) DEFAULT 0.00,
    narration TEXT,
    financial_year_id INT REFERENCES financial_years(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INT REFERENCES users(id)
);

-- Seed Initial Data for Account Groups
INSERT INTO account_groups (group_code, group_name) VALUES 
(1, 'Cash'), 
(2, 'Bank'), 
(3, 'Expenses'), 
(4, 'Income'), 
(5, 'Purchase'), 
(6, 'Sales'), 
(10, 'Sundry Debtors & Creditors');
