-- ==============================================================================
-- Order-to-Audit Snack Factory ERP — Database Reset & Benchmark Reload Script
-- Drops all existing tables, executes schema.sql, and re-seeds seed_benchmark.sql
-- ==============================================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS stock_ledger;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS invoices;
DROP TABLE IF EXISTS sales_order_lines;
DROP TABLE IF EXISTS sales_orders;
DROP TABLE IF EXISTS production_consumptions;
DROP TABLE IF EXISTS production_runs;
DROP TABLE IF EXISTS purchases;
DROP TABLE IF EXISTS bom_lines;
DROP TABLE IF EXISTS bom_headers;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. Load Schema
SOURCE backend/src/main/resources/schema.sql;

-- 2. Load Seed Benchmark Data
SOURCE backend/src/main/resources/seed_benchmark.sql;
