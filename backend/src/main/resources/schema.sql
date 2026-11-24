-- Order-to-Audit Snack Factory ERP — Phase 1 MySQL 8.0 Schema
-- Strictly InnoDB, DECIMAL(12,2) for currency/rates/tax, DECIMAL(12,3) for weights/volumes/quantities.

SET NAMES utf8mb4;
SET time_zone = '+05:30';

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role ENUM('SALESMAN', 'ADMIN', 'PRODUCTION', 'AUDITOR', 'OWNER') NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    customer_type ENUM('RETAILER', 'DISTRIBUTOR') NOT NULL,
    gstin VARCHAR(20) NULL,
    state_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_code VARCHAR(64) NOT NULL,
    name VARCHAR(150) NOT NULL,
    item_type ENUM('RAW_MATERIAL', 'PACKAGING', 'FINISHED_GOOD') NOT NULL,
    unit ENUM('KG', 'G', 'LITRE', 'PIECE', 'PACKET', 'CARTON') NOT NULL,
    reorder_level DECIMAL(12, 3) NOT NULL DEFAULT 0.000,
    standard_rate DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    selling_price DECIMAL(12, 2) NULL,
    gst_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    hsn_code VARCHAR(20) NULL,
    pack_weight_grams INT NULL,
    packets_per_carton INT NULL,
    CONSTRAINT uk_items_sku_code UNIQUE (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bom_headers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    finished_item_id BIGINT NOT NULL,
    recipe_group VARCHAR(100) NOT NULL,
    expected_yield_packets INT NOT NULL,
    CONSTRAINT uk_bom_headers_finished_item UNIQUE (finished_item_id),
    CONSTRAINT fk_bom_headers_finished_item FOREIGN KEY (finished_item_id) REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bom_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_id BIGINT NOT NULL,
    component_item_id BIGINT NOT NULL,
    standard_qty DECIMAL(12, 3) NOT NULL,
    is_packaging BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_bom_lines_bom FOREIGN KEY (bom_id) REFERENCES bom_headers (id),
    CONSTRAINT fk_bom_lines_component_item FOREIGN KEY (component_item_id) REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS purchases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_name VARCHAR(150) NOT NULL,
    invoice_no VARCHAR(100) NOT NULL,
    purchase_date DATE NOT NULL,
    item_id BIGINT NOT NULL,
    qty DECIMAL(12, 3) NOT NULL,
    unit_rate DECIMAL(12, 2) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_purchases_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT fk_purchases_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS production_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_code VARCHAR(64) NOT NULL,
    finished_item_id BIGINT NOT NULL,
    batches_planned DECIMAL(8, 2) NOT NULL,
    expected_output_packets INT NOT NULL,
    actual_good_packets INT NOT NULL,
    damaged_pouches INT NOT NULL DEFAULT 0,
    cartons_used INT NOT NULL DEFAULT 0,
    labour_cost DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    overhead_cost DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    yield_variance_pct DECIMAL(6, 2) NOT NULL DEFAULT 0.00,
    yield_flagged BOOLEAN NOT NULL DEFAULT FALSE,
    calculated_unit_cost DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    run_date DATE NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_production_runs_run_code UNIQUE (run_code),
    CONSTRAINT fk_production_runs_finished_item FOREIGN KEY (finished_item_id) REFERENCES items (id),
    CONSTRAINT fk_production_runs_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS production_consumptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    production_run_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    planned_qty DECIMAL(12, 3) NOT NULL,
    actual_qty DECIMAL(12, 3) NOT NULL,
    variance_pct DECIMAL(6, 2) NOT NULL DEFAULT 0.00,
    variance_flagged BOOLEAN NOT NULL DEFAULT FALSE,
    unit_cost_applied DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_prod_consumptions_run FOREIGN KEY (production_run_id) REFERENCES production_runs (id),
    CONSTRAINT fk_prod_consumptions_item FOREIGN KEY (item_id) REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(64) NOT NULL,
    customer_id BIGINT NOT NULL,
    salesman_id BIGINT NOT NULL,
    status ENUM('SUBMITTED', 'APPROVED', 'REJECTED', 'INVOICED', 'DISPATCHED') NOT NULL DEFAULT 'SUBMITTED',
    stock_shortage_flagged BOOLEAN NOT NULL DEFAULT FALSE,
    shortage_qty INT NOT NULL DEFAULT 0,
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sales_orders_order_number UNIQUE (order_number),
    CONSTRAINT fk_sales_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_sales_orders_salesman FOREIGN KEY (salesman_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales_order_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    qty INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_sales_order_lines_order FOREIGN KEY (order_id) REFERENCES sales_orders (id),
    CONSTRAINT fk_sales_order_lines_item FOREIGN KEY (item_id) REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(64) NOT NULL,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    taxable_amount DECIMAL(12, 2) NOT NULL,
    cgst_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    sgst_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    igst_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    grand_total DECIMAL(12, 2) NOT NULL,
    status ENUM('ISSUED', 'DISPATCHED', 'CANCELLED') NOT NULL DEFAULT 'ISSUED',
    invoice_date DATE NOT NULL,
    CONSTRAINT uk_invoices_invoice_number UNIQUE (invoice_number),
    CONSTRAINT uk_invoices_order_id UNIQUE (order_id),
    CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES sales_orders (id),
    CONSTRAINT fk_invoices_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    amount_paid DECIMAL(12, 2) NOT NULL,
    payment_mode ENUM('CASH', 'UPI', 'NEFT', 'CHEQUE') NOT NULL,
    payment_date DATE NOT NULL,
    recorded_by BIGINT NOT NULL,
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_payments_recorded_by FOREIGN KEY (recorded_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    movement_type ENUM('OPENING', 'PURCHASE', 'PROD_ISSUE', 'PROD_WASTAGE', 'PROD_OUTPUT', 'SALE_DISPATCH', 'ADJUSTMENT') NOT NULL,
    qty_change DECIMAL(12, 3) NOT NULL,
    unit_rate DECIMAL(12, 2) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT NOT NULL,
    remarks VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_stock_ledger_item_id (item_id),
    INDEX idx_stock_ledger_source (source_type, source_id),
    CONSTRAINT fk_stock_ledger_item FOREIGN KEY (item_id) REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(64) NOT NULL,
    changed_by BIGINT NULL,
    details TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_logs_entity (entity_type, entity_id),
    CONSTRAINT fk_audit_logs_changed_by FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
