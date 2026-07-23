-- V21__seed_production_demo_data.sql
-- Wipes obsolete dummy users and seeds production default user & realistic demo homes/appliances.

-- 1. Remove non-admin dummy users & cleanup memberships
DELETE FROM refresh_sessions WHERE user_id IN (SELECT id FROM users WHERE email != 'admin@vegawatt.com');
DELETE FROM home_memberships WHERE user_id IN (SELECT id FROM users WHERE email != 'admin@vegawatt.com');
DELETE FROM users WHERE email != 'admin@vegawatt.com';

-- 2. Insert default admin user if not already present
INSERT INTO users (id, email, password_hash, role, created_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@vegawatt.com',
    '$2a$10$4n930M2G8E9G6v7X9Y.zue9F9F9F9F9F9F9F9F9F9F9F9F9F9F9F9', -- Will be set/refreshed by BootstrapAdminRunner
    'ADMIN',
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- 3. Seed Primary Demo Home 1: "Gül Apartmanı No: 4"
INSERT INTO homes (id, name, contact_email, energy_quota_kwh, budget_quota_try, base_tariff_per_kwh, penalty_tariff_per_kwh, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'Gül Apartmanı No: 4',
    'admin@vegawatt.com',
    350.0000,
    1500.00,
    3.20,
    5.80,
    NOW(),
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- Seed Secondary Demo Home 2: "Yalı Dairesi - Kadıköy"
INSERT INTO homes (id, name, contact_email, energy_quota_kwh, budget_quota_try, base_tariff_per_kwh, penalty_tariff_per_kwh, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000002',
    'Yalı Dairesi - Kadıköy',
    'admin@vegawatt.com',
    500.0000,
    2200.00,
    3.20,
    5.80,
    NOW(),
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- 4. Assign Home Memberships to Admin User
INSERT INTO home_memberships (id, home_id, user_id, role, created_at)
SELECT 'c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', id, 'OWNER', NOW()
FROM users WHERE email = 'admin@vegawatt.com'
ON CONFLICT (home_id, user_id) DO NOTHING;

INSERT INTO home_memberships (id, home_id, user_id, role, created_at)
SELECT 'c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', id, 'OWNER', NOW()
FROM users WHERE email = 'admin@vegawatt.com'
ON CONFLICT (home_id, user_id) DO NOTHING;

-- 5. Seed Appliances for Home 1 (Gül Apartmanı) across various catalog items
INSERT INTO appliances (id, home_id, name, type, safe_power_limit_watt, simulation_min_watt, simulation_max_watt, active, created_at, catalog_item_id, catalog_code_snapshot)
SELECT
    'd0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'Buzdolabı', 'REFRIGERATOR', 250.00, 30.00, 160.00, true, NOW(), id, code
FROM appliance_catalog WHERE code = 'REFRIGERATOR'
ON CONFLICT (home_id, name) DO NOTHING;

INSERT INTO appliances (id, home_id, name, type, safe_power_limit_watt, simulation_min_watt, simulation_max_watt, active, created_at, catalog_item_id, catalog_code_snapshot)
SELECT
    'd0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'Çamaşır Makinesi', 'WASHING_MACHINE', 2200.00, 0.00, 2000.00, true, NOW(), id, code
FROM appliance_catalog WHERE code = 'WASHING_MACHINE'
ON CONFLICT (home_id, name) DO NOTHING;

INSERT INTO appliances (id, home_id, name, type, safe_power_limit_watt, simulation_min_watt, simulation_max_watt, active, created_at, catalog_item_id, catalog_code_snapshot)
SELECT
    'd0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 'Salon Kliması', 'AIR_CONDITIONER', 2500.00, 400.00, 2100.00, true, NOW(), id, code
FROM appliance_catalog WHERE code = 'INVERTER_AIR_CONDITIONER'
ON CONFLICT (home_id, name) DO NOTHING;

INSERT INTO appliances (id, home_id, name, type, safe_power_limit_watt, simulation_min_watt, simulation_max_watt, active, created_at, catalog_item_id, catalog_code_snapshot)
SELECT
    'd0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001', 'Kahve Makinesi', 'COFFEE_MAKER', 1500.00, 0.00, 1400.00, true, NOW(), id, code
FROM appliance_catalog WHERE code = 'ESPRESSO_MACHINE'
ON CONFLICT (home_id, name) DO NOTHING;

INSERT INTO appliances (id, home_id, name, type, safe_power_limit_watt, simulation_min_watt, simulation_max_watt, active, created_at, catalog_item_id, catalog_code_snapshot)
SELECT
    'd0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001', 'Wi-Fi Router', 'ROUTER', 30.00, 8.00, 15.00, true, NOW(), id, code
FROM appliance_catalog WHERE code = 'WIFI_ROUTER'
ON CONFLICT (home_id, name) DO NOTHING;

-- Seed Appliances for Home 2 (Yalı Dairesi)
INSERT INTO appliances (id, home_id, name, type, safe_power_limit_watt, simulation_min_watt, simulation_max_watt, active, created_at, catalog_item_id, catalog_code_snapshot)
SELECT
    'd0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000002', 'Bulaşık Makinesi', 'DISHWASHER', 2000.00, 0.00, 1800.00, true, NOW(), id, code
FROM appliance_catalog WHERE code = 'DISHWASHER'
ON CONFLICT (home_id, name) DO NOTHING;

INSERT INTO appliances (id, home_id, name, type, safe_power_limit_watt, simulation_min_watt, simulation_max_watt, active, created_at, catalog_item_id, catalog_code_snapshot)
SELECT
    'd0000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000002', 'OLED TV', 'TELEVISION', 300.00, 1.00, 220.00, true, NOW(), id, code
FROM appliance_catalog WHERE code = 'OLED_TV'
ON CONFLICT (home_id, name) DO NOTHING;
