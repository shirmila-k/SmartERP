-- Order-to-Audit Snack Factory ERP — Phase 1 Benchmark Seed Data
-- All 6 users share BCrypt ($2a$10$) hash for password: pass123

SET NAMES utf8mb4;
SET time_zone = '+05:30';

-- 1. Users (password: pass123)
INSERT INTO users (username, password_hash, full_name, role) VALUES
    ('salesman_a', '$2a$10$S03fW0rDal/AXU3hYBG0GOAzIgegPYnHTkv20xdtjfQGPmxp4MIwa', 'Salesman A', 'SALESMAN'),
    ('salesman_b', '$2a$10$S03fW0rDal/AXU3hYBG0GOAzIgegPYnHTkv20xdtjfQGPmxp4MIwa', 'Salesman B', 'SALESMAN'),
    ('admin',      '$2a$10$S03fW0rDal/AXU3hYBG0GOAzIgegPYnHTkv20xdtjfQGPmxp4MIwa', 'Factory Admin', 'ADMIN'),
    ('prod_sup',   '$2a$10$S03fW0rDal/AXU3hYBG0GOAzIgegPYnHTkv20xdtjfQGPmxp4MIwa', 'Production Supervisor', 'PRODUCTION'),
    ('auditor',    '$2a$10$S03fW0rDal/AXU3hYBG0GOAzIgegPYnHTkv20xdtjfQGPmxp4MIwa', 'External Auditor', 'AUDITOR'),
    ('owner',      '$2a$10$S03fW0rDal/AXU3hYBG0GOAzIgegPYnHTkv20xdtjfQGPmxp4MIwa', 'Factory Owner', 'OWNER');

-- 2. Customers
INSERT INTO customers (name, customer_type, gstin, state_name, phone) VALUES
    ('Retailer A',    'RETAILER',    '33AABCR1234A1Z5', 'Tamil Nadu', '9840011111'),
    ('Distributor B', 'DISTRIBUTOR', '29AABCD5678B1Z9', 'Karnataka',  '9845022222');

-- 3. Items (3 Raw Materials, 2 Packaging, 1 Finished Good)
INSERT INTO items (
    sku_code, name, item_type, unit, reorder_level,
    standard_rate, selling_price, gst_rate, hsn_code, pack_weight_grams, packets_per_carton
) VALUES
    ('RM-FLOUR',       'Flour',                      'RAW_MATERIAL',  'KG',     25.000,  42.00, NULL,  0.00, NULL,       NULL, NULL),
    ('RM-OIL',         'Cooking oil',                'RAW_MATERIAL',  'LITRE',  10.000, 140.00, NULL,  0.00, NULL,       NULL, NULL),
    ('RM-SPICE',       'Salt and spice mix',         'RAW_MATERIAL',  'KG',      2.000, 200.00, NULL,  0.00, NULL,       NULL, NULL),
    ('PK-POUCH-200',   '200 g pouch',                'PACKAGING',     'PIECE', 100.000,   2.50, NULL,  0.00, NULL,       NULL, NULL),
    ('PK-CARTON-20',   'Carton (holds 20 packets)',  'PACKAGING',     'PIECE',  10.000,  15.00, NULL,  0.00, NULL,       NULL, NULL),
    ('FG-MURUKKU-200', 'Murukku 200 g',              'FINISHED_GOOD', 'PACKET', 20.000,  35.00, 50.00, 5.00, '21069099', 200,  20);

-- 4. BOM Header & Lines for FG-MURUKKU-200
INSERT INTO bom_headers (finished_item_id, recipe_group, expected_yield_packets)
VALUES (
    (SELECT id FROM items WHERE sku_code = 'FG-MURUKKU-200'),
    'MURUKKU_BASE',
    200
);

INSERT INTO bom_lines (bom_id, component_item_id, standard_qty, is_packaging)
SELECT
    bh.id,
    i.id,
    v.standard_qty,
    v.is_packaging
FROM bom_headers bh
JOIN items fg ON fg.id = bh.finished_item_id AND fg.sku_code = 'FG-MURUKKU-200'
JOIN (
    SELECT 'RM-FLOUR'     AS sku_code,  40.000 AS standard_qty, FALSE AS is_packaging
    UNION ALL
    SELECT 'RM-OIL'       AS sku_code,  10.000 AS standard_qty, FALSE AS is_packaging
    UNION ALL
    SELECT 'RM-SPICE'     AS sku_code,   1.200 AS standard_qty, FALSE AS is_packaging
    UNION ALL
    SELECT 'PK-POUCH-200' AS sku_code, 200.000 AS standard_qty, TRUE  AS is_packaging
    UNION ALL
    SELECT 'PK-CARTON-20' AS sku_code,  10.000 AS standard_qty, TRUE  AS is_packaging
) v
JOIN items i ON i.sku_code = v.sku_code;

-- 5. Opening Stock Ledger Entries (source_type = 'OPENING', source_id = 0)
INSERT INTO stock_ledger (item_id, movement_type, qty_change, unit_rate, source_type, source_id, remarks)
SELECT
    i.id,
    'OPENING',
    v.qty_change,
    v.unit_rate,
    'OPENING',
    0,
    'Benchmark opening stock balance'
FROM (
    SELECT 'RM-FLOUR'       AS sku_code, 100.000 AS qty_change,  42.00 AS unit_rate
    UNION ALL
    SELECT 'RM-OIL'         AS sku_code,  30.000 AS qty_change, 140.00 AS unit_rate
    UNION ALL
    SELECT 'RM-SPICE'       AS sku_code,   5.000 AS qty_change, 200.00 AS unit_rate
    UNION ALL
    SELECT 'PK-POUCH-200'   AS sku_code, 500.000 AS qty_change,   2.50 AS unit_rate
    UNION ALL
    SELECT 'PK-CARTON-20'   AS sku_code,  30.000 AS qty_change,  15.00 AS unit_rate
    UNION ALL
    SELECT 'FG-MURUKKU-200' AS sku_code,  50.000 AS qty_change,  35.00 AS unit_rate
) v
JOIN items i ON i.sku_code = v.sku_code;
