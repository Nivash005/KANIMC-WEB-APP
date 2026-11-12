# KANIMC Web Application

A modern Spring Boot & Web-based accounting management application providing full functional parity with the legacy **Visual FoxPro 9** Commission Agent & Accounting System.

---

## 🌟 Key Features

- **Transactions**:
  - **Opening Chitta**: Full purchase entry with farmer code/name autocomplete, rate, qty, gross, freight, advance, commission calculations, and automatic next-numbering per date.
  - **Sales Bill**: Buyer selection, lot tracking, multi-item invoicing with real-time rate and net calculations.
  - **Cash Payment & Receipt Vouchers**: Dual-entry voucher processing (Debit/Credit parity) with live account balance lookups.
  - **Chitta Payment Settlement**: Settle pending farmer balances with automatic ledger posting.
  - **Opening Balance Entry**: Historical opening balance management by financial year and period.

- **Registers & Reports**:
  - Chitta Register, Chitta Pending Register, Chitta Payment Register.
  - Sales Bill Register.
  - Cash Payment Register & Receipt Voucher Register.
  - Account Ledger with running balances (Dr/Cr parity).
  - Outstandings & Summary Reports.
  - In-app printable previews with zero popup windows.

- **Masters & Utilities**:
  - Account Master (over 2,460+ accounts supported with codes, groups, places, and phone numbers).
  - Account Group Master, Place Master, Item Master, Commission Rates.
  - Financial Year & Financial Period Manager.
  - Account Mappings & User Passwords.

- **Authentic Visual FoxPro 9 UI Experience**:
  - Classic grey bevels, Windows 98/2000 titlebars, iconic FoxPro button controls (`Top`, `Prev`, `Next`, `Bottom`, `New`, `Change`, `Find`, `Delete`).
  - Zero browser popup tabs or native alerts: all dialogs are rendered strictly in-app (`#vfpMessageBoxModal`, `#vfpPromptModal`, `#vfpReportPreviewModal`).
  - Full keyboard shortcut support (`F5` New, `F8` Find, `F9` Save, `Enter` navigation, `Esc` cancel/close).
  - High-performance real-time search table in all Find dialogs filtering accounts, chittas, and vouchers as you type.

---

## 🛠️ Technology Stack

- **Backend**: Java 17+, Spring Boot 3.x, Spring Data JPA, Spring Web MVC
- **Database**: Embedded H2 Database (persisted to disk in `./data/kanimc_db`)
- **Frontend**: HTML5, Vanilla JavaScript (ES6+), CSS3 with authentic Visual FoxPro 9 theming
- **Build Tool**: Apache Maven

---

## 🚀 Getting Started

### Prerequisites

- Java Development Kit (JDK) 17 or higher
- Apache Maven 3.8+ (or use the included wrapper/bundled distribution)

### Running the Application

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Nivash005/KANIMC-WEB-APP.git
   cd KANIMC-WEB-APP
   ```

2. **Run with Maven**:
   ```bash
   mvn spring-boot:run
   ```

3. **Access the Application**:
   Open your browser and navigate to:
   ```
   http://localhost:8080/
   ```

---

## 📁 Project Structure

```
KANIMC-WEB-APP/
├── data/                       # Embedded database files (kanimc_db.mv.db)
├── database/                   # SQL schemas and migration scripts
│   └── schema.sql
├── src/
│   └── main/
│       ├── java/com/kanimc/
│       │   ├── config/         # Spring & Web configuration
│       │   ├── controller/     # REST controllers (Chitta, Bill, Account, etc.)
│       │   ├── dto/            # Data transfer objects
│       │   ├── entity/         # JPA database entities
│       │   ├── repository/     # Spring Data JPA repositories
│       │   ├── service/        # Business logic & accounting services
│       │   └── KanimcApplication.java
│       └── resources/
│           ├── static/         # Frontend web assets
│           │   ├── index.html  # Main single-page FoxPro desktop workspace
│           │   ├── modal_logic.js # Window, dialog, and register logic
│           │   ├── styles.css  # FoxPro 9 CSS styling
│           │   └── app.js
│           ├── application.properties
│           └── application.yml
├── pom.xml                     # Maven project configuration
├── .gitignore
└── README.md
```

---

## 📄 License

Proprietary accounting system developed for KANIMC. All rights reserved.
