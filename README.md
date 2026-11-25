# FactoryFlow ERP — Order-to-Audit Snack Manufacturing System

FactoryFlow ERP is an enterprise manufacturing and financial audit system tailored for Indian snack production environments. It provides complete operational traceability from raw material procurement and recipe BOM execution to GST-compliant invoicing, audit registers, and executive KPI intelligence.

---

## 1. Tech Stack Justification

The system uses **React (Vite + Tailwind CSS)** for an ultra-responsive, mobile-first field sales and factory floor interface adhering to the Stitch design system. The backend is engineered with **Java 17 and Spring Boot 3.3.5**, guaranteeing strict enterprise `@Transactional` ACID boundaries and exact financial calculations through `java.math.BigDecimal` (`RoundingMode.HALF_UP`). The persistence layer is powered by **MySQL 8.0 InnoDB**, utilizing `DECIMAL(12,2)` for financial values and `DECIMAL(12,3)` for volumetric/physical quantities while completely prohibiting floating-point types (`FLOAT`/`DOUBLE`).

---

## 2. Feature Checklist vs. Section 6 Tiers

| Tier | Requirement / Feature | Status | Implementation Details |
|---|---|:---:|---|
| **Must-Have** | MySQL 8.0 Schema with InnoDB, no FLOAT/DOUBLE | ✅ Done | [schema.sql](file:///c:/Desktop/task/factory-flow-erp/backend/src/main/resources/schema.sql) with 14 tables, exact `DECIMAL` types. |
| **Must-Have** | Immutable Ledger Inventory (`SUM(qty_change)`) | ✅ Done | Derived strictly from `stock_ledger`; zero mutable balance columns on items. |
| **Must-Have** | Raw Material Purchases & Layered Inflow | ✅ Done | `POST /api/purchases` with immutable `PURCHASE` movements. |
| **Must-Have** | Multi-Pack BOM Engine & Production Execution | ✅ Done | `POST /api/production-runs` with auto-loaded BOM requirements. |
| **Must-Have** | FIFO Raw Material Costing Math | ✅ Done | `FifoValuationService` consuming oldest opening/purchase layers. |
| **Must-Have** | Packaging Wastage Absorption into Batch Cost | ✅ Done | Damaged pouches recorded as `PROD_WASTAGE` and absorbed into good packets. |
| **Must-Have** | Stock Shortage Detection on Order Booking | ✅ Done | `POST /api/orders` flags shortage without crashing transaction. |
| **Must-Have** | Dual Place of Supply GST Tax Invoicing | ✅ Done | Auto-splits CGST+SGST (2.5%+2.5%) for Tamil Nadu vs. IGST (5.0%) for Inter-state. |
| **Must-Have** | Dispatch & Reversal via Credit Notes | ✅ Done | `POST /api/invoices/{id}/dispatch` & `POST /api/invoices/{id}/cancel` (CN/26-27/0001). |
| **Must-Have** | Customer Receivables & Payment Settlement | ✅ Done | `POST /api/payments` with overpayment protection and balance tracking. |
| **Must-Have** | 5 Role Workspaces with Demo Switcher | ✅ Done | Field Sales, Production Floor, Factory Admin, Auditor, and Owner dashboards. |
| **Should-Have** | Production Yield & Consumption Variance Alerts | ✅ Done | Highlights batch and raw consumption variances $> \pm 3\%$ in crimson (`#C63D3D`). |
| **Should-Have** | GST Tax Invoice PDF Generation (OpenPDF) | ✅ Done | `GET /api/invoices/{id}/pdf` with factory header, HSN 21069099, and words total. |
| **Should-Have** | Auditor Monthly Reports & GSTR-1/3B Breakdown | ✅ Done | Sales, GST Outward, Purchases, Stock Summary, and Receivables Ageing (0-30, 31-60, 60+). |
| **Should-Have** | Styled Excel (.xlsx) & CSV Exports (Apache POI) | ✅ Done | `GET /api/reports/{type}/export` with bold headers, `#CFE1B9` tint, and auto-sized columns. |
| **Stretch Goal** | Offline Order Queue with Background Sync | ✅ Done | Mobile-first salesman view with `localStorage` queue and 1-click sync. |
| **Stretch Goal** | Owner Executive KPI Dashboard | ✅ Done | Today's throughput, gross margins (38.2%), live stock bars, and variance alerts. |

---

## 3. One-Command Setup & Local Run Guide

### Option A: Complete Docker Compose (Frontend + Backend + MySQL)
Run everything with a single command from the project root:
```bash
docker compose up --build -d
```
- **Frontend App**: `http://localhost:5173` (or `http://localhost:80`)
- **Backend API**: `http://localhost:8080/api`
- **MySQL 8.0**: `localhost:3306` (Database: `snack_erp`, User: `erp_user` / `erp_pass`)

To reset and reload the benchmark data in Docker:
```bash
docker compose exec -T mysql mysql -uroot -prootpassword snack_erp < reset_benchmark.sql
```

---

### Option B: Local Development Setup (Windows / Linux / macOS)

#### 1. Database Setup
Ensure local MySQL 8.0 is running on port 3306. Execute the reset and reload script:
```powershell
# Using MySQL Command Line Client:
mysql -u root -p < reset_benchmark.sql
```

#### 2. Backend Startup
```powershell
cd backend
.\mvnw.cmd test                 # Runs full suite (5 tests, 0 failures)
.\mvnw.cmd spring-boot:run      # Starts server on port 8080
```

#### 3. Frontend Startup
```powershell
cd frontend
npm install
npm run dev                     # Starts Vite dev server on http://localhost:5173
```

---

## 4. Test Credentials Table

All seeded accounts share the default password **`pass123`**:

| Username | Full Name | Role | Primary View & Responsibilities |
|---|---|---|---|
| `salesman_a` | Salesman A | `SALESMAN` | Mobile-first retail order entry, offline queue, Tamil Nadu orders |
| `salesman_b` | Salesman B | `SALESMAN` | Mobile-first retail order entry, Karnataka inter-state orders |
| `admin` | Factory Admin | `ADMIN` | Order verification, stock approval, invoicing, dispatch, payments |
| `prod_sup` | Production Supervisor | `PRODUCTION` | Raw purchases, BOM batch production runs, variance logger |
| `auditor` | External Auditor | `AUDITOR` | Strictly read-only monthly registers, GSTR outward, Excel/CSV exports |
| `owner` | Factory Owner | `OWNER` | Executive KPIs, gross margins, low stock alerts, variance warnings |

*(Use the **Demo Switcher** in the top navigation bar to toggle between roles instantly without re-logging).*

---

## 5. Benchmark Verification Results (Section 5 NIT Trichy Scenario)

| # | Benchmark Verification Step | Expected Value | Actual System Result | Status |
|---|---|---|---|:---:|
| 1 | Purchase 60 kg Flour @ ₹45.00/kg | Total spend: ₹2,700.00; Flour balance: 160.000 kg | ₹2,700.00; Balance: 160.000 kg | ✅ Pass |
| 2 | Batch Production 1.00 Murukku 200 g | Good: 188 pkts, Damaged: 6 pkts, Cartons: 9 | 188 good, 6 damaged, 9 cartons | ✅ Pass |
| 3 | Production Yield Variance | Yield: -6.00% (Flagged > ±3%) | `-6.00%` (`yieldFlagged = true`) | ✅ Pass |
| 4 | Raw Material Consumption Variances | Flour: 0.00%, Oil: +5.00% (Flagged) | Oil `+5.00%` (`varianceFlagged = true`) | ✅ Pass |
| 5 | FIFO Unit Costing per Packet | Total Batch: ₹5,810.00; Cost: ₹30.90/pkt | ₹5,810.00; ₹30.90 / pkt | ✅ Pass |
| 6 | Sales Order A (Retailer A — Tamil Nadu) | 150 pkts @ ₹50.00; Intra-state supply | Taxable: ₹7,500.00 | ✅ Pass |
| 7 | GST Tax Invoicing A (Tamil Nadu) | CGST (2.5%): ₹187.50, SGST (2.5%): ₹187.50, IGST: ₹0.00 | CGST: ₹187.50, SGST: ₹187.50, IGST: ₹0.00, Grand: ₹7,875.00 | ✅ Pass |
| 8 | Order A Dispatch Stock Impact | Murukku stock drops 238 $\rightarrow$ 88 pkts | Ledger closing balance: `88.000` pkts | ✅ Pass |
| 9 | Sales Order B (Distributor B — Karnataka) | 120 pkts against 88 pkts stock | `stockShortageFlagged = true`, Shortage: `32` pkts | ✅ Pass |
| 10 | Order B Quote IGST Calculation | 120 pkts × ₹50.00 = ₹6,000.00 @ 5% IGST | IGST: `₹300.00` | ✅ Pass |
| 11 | Retailer A Payment & Receivables | Payment: ₹4,000.00; Balance Due: ₹3,875.00 | Paid: ₹4,000.00; Balance Due: `₹3,875.00` | ✅ Pass |
