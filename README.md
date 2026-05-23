# 🏆 Sport Hub — Premium Sporting Club Management

Welcome to **Sport Hub**, an ultra-premium, high-fidelity JavaFX desktop application designed to streamline the administration of sporting clubs, membership subscriptions, team organizations, attendance logs, and financial flows.

Sport Hub features a clean, responsive, and visually stunning user interface inspired by modern premium SaaS dashboards (white cards, clean typography, and a crisp, high-contrast dark gradient navigation sidebar).

---

## ✨ Key Features

### 📊 1. Advanced Dashboard & Analytics
* **KPI Metrics**: Real-time summary cards displaying **Active Members**, **Monthly Revenue**, **Monthly Expenses**, and **Subscriptions Expiring Soon (7 Days)**.
* **Interactive Charts**:
  * **Membership Distribution**: Interactive PieChart showing active vs. inactive member ratios.
  * **Weekly Revenue Breakdown**: Dynamic BarChart reflecting income distribution over the current month.
* **Visual Expiry Alerts**: A smart, dynamic warning banner at the top of the dashboard that automatically alerts employees of expiring subscriptions.

### 👥 2. Member & Team Organization
* **Full CRUD Operations**: Create, read, update, and delete members and teams.
* **Smart Member Profiles**: Double-click any member row in the management screen to open their detailed, high-fidelity profile card showing full subscription status and team associations.
* **Flexible Input Validation**: Support for both 10-digit and 11-digit mobile phone numbers and standard email formats.
* **Optional Team Association**: Members can be enrolled either with or without a team assignment, avoiding rigid workflow constraints.

### 💳 3. Subscriptions & Billing
* **Flexible Billing Plans**: Toggle between **Monthly ($50)**, **Quarterly ($150)**, and **Yearly ($500)** subscription packages.
* **Automatic Date Calculations**: The system automatically computes and sets start and end dates based on the chosen plan type.
* **Email Reminder System**: An integrated email dispatcher (using Jakarta Mail) that allows administrators to notify members with expiring subscriptions in a single click.

### 📋 4. Attendance Tracking
* **Daily Attendance Log**: Register member attendance by specific session types (e.g., training, competition, matches) and status (present, absent, excused).
* **Historical Filters**: Search and view past logs dynamically.

### 💰 5. Financial Flow & CSV Exporting
* **Comprehensive Income & Expense Logs**: Categorize financial inflows (membership dues) and outflows (equipment maintenance, coach salaries, utilities).
* **Interactive Filters**: Instantly filter financial logs by specific categories.
* **Instant Export**: Export filtered reports to standard `.csv` files for spreadsheets with one click.

---

## 🔒 Security & Access Control

### 🛡️ Role-Based Access Control (RBAC)
The application has built-in roles managed via a global `SessionManager` class to enforce access restrictions:
* **Admin (`admin`)**: Complete access to all modules, settings, billing controls, and deletion privileges.
* **Employee (`emp`)**: Restricted access to dashboard overview, member management, and attendance logs. Buttons for financial configuration, team management, and deletions are automatically disabled.

### 🔑 Secure Password Hashing
All database credentials are fully secured using high-entropy **SHA-256** hashing. The login system supports a seamless plain-text fallback mechanism to ensure legacy seeded credentials remain functional for local development.

---

## 🛠️ Technology Stack
* **Language**: Java 22
* **Framework**: JavaFX 22.0.1 (Controls, FXML, Web, Swing, Media)
* **Database**: SQLite (via JDBC Driver)
* **Dependency Manager**: Apache Maven
* **Styling**: Vanilla CSS3 (high-contrast premium design system with custom layouts, custom charts, and smooth drop-shadow filters)

---

## 🚀 Getting Started

### Prerequisites
Make sure you have the following installed on your machine:
* **Java Development Kit (JDK)**: Version 21 or 22
* **Maven**: (A built-in Maven Wrapper `./mvnw` is provided)

### Setup & Installation
1. Clone the repository to your local workspace.
2. The SQLite database connection is fully self-initializing. Upon the first launch, the schema and essential test data will be seeded automatically into `sport_hub.db`.

### Execution Commands

To build the project:
```bash
./mvnw clean compile
```

To run the JavaFX application:
```bash
./mvnw javafx:run
```

---

## 👤 Test Credentials
You can log into the application using the following built-in accounts:

| Username | Password | Role | Access Level |
| :--- | :--- | :--- | :--- |
| **admin** | `admin` | Administrator | **Full Access** (all pages) |
| **emp** | `emp` | Employee | **Restricted Access** (Dashboard, Members, Attendance) |

---

## 📁 Architecture Highlights
* **Controller-View Separation**: Uses standard JavaFX FXML files for visual layouts mapped to specific controller files under `com.example.club_sporting_final.admin.Controller`.
* **Central Database Provider**: `DatabaseConnection` manages single-instance connection lifecycles, ensuring thread safety and query optimization.
* **Responsive Layouts**: Designed using nested `AnchorPane`, `BorderPane`, `VBox`, and `HBox` modules for flawless rendering across multiple screen sizes.
