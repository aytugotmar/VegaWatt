-- Seeds the initial 45-item appliance catalog (yapılacak.md §7).
-- Fixed literal UUIDs are used (not gen_random_uuid()) so the seed is deterministic and
-- reproducible across environments.
-- Wattage figures are onboarding defaults, not electrical safety certifications; users may
-- override them per instance when they register an appliance from a catalog item.

-- ==================== KITCHEN (Mutfak) ====================

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'REFRIGERATOR', 'Buzdolabı',
    'Evin ana soğutucusu; termostat kontrollü, sürekli çalışan mutfak cihazı.', 'KITCHEN', 'THERMOSTATIC_CYCLE',
    220, 80, 180, 2, 15, TRUE, FALSE, TRUE,
    'refrigerator', 'buzdolabı, buzdolabi, soğutucu', TRUE, TRUE, 10, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000002', 'FREEZER', 'Derin Dondurucu',
    'Uzun süreli gıda saklama için termostat kontrollü dondurucu.', 'KITCHEN', 'THERMOSTATIC_CYCLE',
    240, 70, 200, 2, 12, TRUE, FALSE, TRUE,
    'freezer', 'dondurucu, derin dondurucu, freezer', FALSE, TRUE, 20, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000003', 'OVEN', 'Fırın',
    'Pişirme için termostat kontrollü, yüksek güçlü mutfak cihazı.', 'KITCHEN', 'THERMOSTATIC_SESSION',
    2800, 700, 2500, 0, 3, TRUE, FALSE, TRUE,
    'oven', 'fırın, firin, ocak, pişirme', TRUE, TRUE, 30, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000004', 'ELECTRIC_COOKTOP', 'Elektrikli Ocak',
    'Kullanıma göre değişken güç çeken elektrikli pişirme yüzeyi.', 'KITCHEN', 'VARIABLE_LOAD',
    3500, 500, 3200, 0, 2, TRUE, FALSE, TRUE,
    'cooktop', 'ocak, elektrikli ocak, indüksiyon', FALSE, TRUE, 40, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000005', 'MICROWAVE', 'Mikrodalga',
    'Kısa süreli yüksek güçle çalışan ısıtma/pişirme cihazı.', 'KITCHEN', 'SHORT_HIGH_POWER',
    1700, 700, 1500, 1, 4, TRUE, FALSE, FALSE,
    'microwave', 'mikrodalga, isıtma', FALSE, TRUE, 50, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000006', 'COFFEE_MACHINE', 'Kahve Makinesi',
    'Kısa süreli ve yüksek güçle çalışan kahve hazırlama cihazı.', 'KITCHEN', 'SHORT_HIGH_POWER',
    1500, 600, 1300, 0, 2, TRUE, FALSE, FALSE,
    'coffee', 'kahve, espresso, filtre kahve, kahve makinesi', TRUE, TRUE, 60, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000007', 'KETTLE', 'Su Isıtıcısı',
    'Suyu hızlıca kaynatmak için kısa süreli yüksek güç çeken cihaz.', 'KITCHEN', 'SHORT_HIGH_POWER',
    2600, 1500, 2400, 0, 1, TRUE, FALSE, FALSE,
    'kettle', 'su ısıtıcı, kettle, çaydanlık', FALSE, TRUE, 70, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000008', 'TOASTER', 'Tost Makinesi',
    'Kısa süreli çalışan ekmek kızartma cihazı.', 'KITCHEN', 'SHORT_HIGH_POWER',
    1800, 700, 1600, 0, 1, TRUE, FALSE, FALSE,
    'toaster', 'tost, ekmek kızartma, tost makinesi', FALSE, TRUE, 80, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000009', 'DISHWASHER', 'Bulaşık Makinesi',
    'Program döngüsüyle çalışan, aşamaları farklı güç seviyeleri gerektiren bulaşık makinesi.', 'KITCHEN', 'PROGRAM_CYCLE',
    2000, 5, 1800, 0.5, 3, TRUE, FALSE, TRUE,
    'dishwasher', 'bulaşık makinesi, bulasik, yıkama', TRUE, TRUE, 90, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000010', 'RANGE_HOOD', 'Davlumbaz',
    'Mutfakta aç/kapa kullanılan havalandırma cihazı.', 'KITCHEN', 'MANUAL_SWITCH',
    300, 30, 250, 0, 2, TRUE, FALSE, FALSE,
    'rangeHood', 'davlumbaz, aspiratör', FALSE, TRUE, 100, 1, now(), now());

-- ==================== CLIMATE (Isıtma ve İklimlendirme) ====================

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000011', 'AIR_CONDITIONER', 'Klima',
    'Termostat kontrollü, soğutma/ısıtma yapan iklimlendirme cihazı.', 'CLIMATE', 'THERMOSTATIC_CYCLE',
    2500, 500, 2200, 1, 5, TRUE, FALSE, TRUE,
    'airConditioner', 'klima, soğutma, ısıtma, air conditioner', TRUE, TRUE, 110, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000012', 'ELECTRIC_HEATER', 'Elektrikli Soba',
    'Termostat kontrollü elektrikli ısıtma cihazı.', 'CLIMATE', 'THERMOSTATIC_CYCLE',
    2500, 800, 2200, 0, 3, TRUE, FALSE, TRUE,
    'heater', 'soba, elektrikli soba, ısıtıcı', TRUE, TRUE, 120, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000013', 'FAN_HEATER', 'Fanlı Isıtıcı',
    'Fan destekli, termostat kontrollü ısıtma cihazı.', 'CLIMATE', 'THERMOSTATIC_CYCLE',
    2500, 1000, 2200, 0, 2, TRUE, FALSE, TRUE,
    'fanHeater', 'fanlı ısıtıcı, ısıtıcı', FALSE, TRUE, 130, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000014', 'WATER_HEATER', 'Elektrikli Su Isıtıcısı',
    'Suyu ısıtıp sıcak tutan, termostat kontrollü cihaz.', 'CLIMATE', 'THERMOSTATIC_CYCLE',
    3300, 1500, 3000, 2, 50, TRUE, FALSE, TRUE,
    'waterHeater', 'su ısıtıcı, termosifon, boyler', FALSE, TRUE, 140, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000015', 'FAN', 'Vantilatör',
    'Aç/kapa kullanılan serinletme cihazı.', 'CLIMATE', 'MANUAL_SWITCH',
    130, 20, 100, 0, 2, TRUE, FALSE, FALSE,
    'fan', 'vantilatör, fan, serinletme', FALSE, TRUE, 150, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000016', 'AIR_PURIFIER', 'Hava Temizleyici',
    'Sürekli çalışan, moduna göre gücü değişen hava temizleme cihazı.', 'CLIMATE', 'ALWAYS_ON_VARIABLE',
    120, 8, 80, 2, 8, TRUE, FALSE, FALSE,
    'airPurifier', 'hava temizleyici, purifier', FALSE, TRUE, 160, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000017', 'HUMIDIFIER', 'Nemlendirici',
    'Kısa süreli kullanılan hava nemlendirme cihazı.', 'CLIMATE', 'SHORT_SESSION',
    100, 15, 80, 0, 2, TRUE, FALSE, FALSE,
    'humidifier', 'nemlendirici, buhar', FALSE, TRUE, 170, 1, now(), now());

-- ==================== CLEANING (Temizlik ve Bakım) ====================

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000018', 'WASHING_MACHINE', 'Çamaşır Makinesi',
    'Program döngüsüyle çalışan, aşamalara göre güç değişen çamaşır makinesi.', 'CLEANING', 'PROGRAM_CYCLE',
    2400, 5, 2200, 0.5, 3, TRUE, FALSE, TRUE,
    'washingMachine', 'çamaşır makinesi, camasir, yıkama', TRUE, TRUE, 180, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000019', 'DRYER', 'Kurutma Makinesi',
    'Program döngüsüyle çalışan çamaşır kurutma makinesi.', 'CLEANING', 'PROGRAM_CYCLE',
    3300, 300, 3000, 1, 5, TRUE, FALSE, TRUE,
    'dryer', 'kurutma makinesi, kurutucu', FALSE, TRUE, 190, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000020', 'VACUUM_CLEANER', 'Elektrikli Süpürge',
    'Kısa süreli, yüksek güçle çalışan temizlik cihazı.', 'CLEANING', 'SHORT_HIGH_POWER',
    2500, 500, 2200, 0, 0, TRUE, FALSE, FALSE,
    'vacuum', 'elektrikli süpürge, süpürge', FALSE, TRUE, 200, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000021', 'ROBOT_VACUUM', 'Robot Süpürge',
    -- Charging-state power (10-50W per spec) is not separately modeled by this table's columns yet;
    -- it will be captured via simulation_config JSONB once that column is introduced (Aşama 6).
    'Temizlik ve şarj döngüsü arasında geçiş yapan robotik süpürge.', 'CLEANING', 'CHARGING_AND_SESSION',
    120, 20, 80, 1, 5, TRUE, FALSE, TRUE,
    'robotVacuum', 'robot süpürge, robotik süpürge', FALSE, TRUE, 210, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000022', 'IRON', 'Ütü',
    'Termostat kontrollü, kullanım oturumları şeklinde çalışan ütü.', 'CLEANING', 'THERMOSTATIC_SESSION',
    2600, 800, 2400, 0, 2, TRUE, FALSE, TRUE,
    'iron', 'ütü, ütüleme', FALSE, TRUE, 220, 1, now(), now());

-- ==================== ENTERTAINMENT (Eğlence) ====================

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000023', 'TELEVISION', 'Televizyon',
    'Bekleme modu destekli, kullanım pencerelerinde aktif olan eğlence cihazı.', 'ENTERTAINMENT', 'STANDBY_DEVICE',
    180, 40, 150, 0.5, 3, TRUE, FALSE, FALSE,
    'television', 'tv, televizyon, ekran', TRUE, TRUE, 230, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000024', 'GAME_CONSOLE', 'Oyun Konsolu',
    'Bekleme modu destekli oyun konsolu.', 'ENTERTAINMENT', 'STANDBY_DEVICE',
    260, 50, 220, 1, 10, TRUE, FALSE, FALSE,
    'gameConsole', 'oyun konsolu, konsol, playstation, xbox', FALSE, TRUE, 240, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000025', 'SOUND_SYSTEM', 'Ses Sistemi',
    'Bekleme modu destekli ev ses sistemi.', 'ENTERTAINMENT', 'STANDBY_DEVICE',
    350, 20, 300, 1, 8, TRUE, FALSE, FALSE,
    'soundSystem', 'ses sistemi, hoparlör, müzik seti', FALSE, TRUE, 250, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000026', 'MEDIA_BOX', 'Medya Kutusu',
    'Sürekli açık, dar güç aralığında çalışan medya yayın cihazı.', 'ENTERTAINMENT', 'ALWAYS_ON_STABLE',
    30, 3, 20, NULL, NULL, FALSE, FALSE, FALSE,
    'mediaBox', 'medya kutusu, streaming box, apple tv', FALSE, TRUE, 260, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000027', 'PROJECTOR', 'Projektör',
    'Kısa süreli kullanılan görüntü yansıtma cihazı.', 'ENTERTAINMENT', 'SHORT_SESSION',
    450, 150, 400, 0.5, 5, TRUE, FALSE, FALSE,
    'projector', 'projektör, projeksiyon', FALSE, TRUE, 270, 1, now(), now());

-- ==================== COMPUTING (Bilgisayar ve Ofis) ====================

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000028', 'DESKTOP_COMPUTER', 'Masaüstü Bilgisayar',
    'Kullanım yoğunluğuna göre değişken güç çeken masaüstü bilgisayar.', 'COMPUTING', 'VARIABLE_LOAD',
    450, 40, 350, 1, 5, TRUE, FALSE, TRUE,
    'computer', 'bilgisayar, masaüstü, pc', TRUE, TRUE, 280, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000029', 'GAMING_COMPUTER', 'Gaming Bilgisayarı',
    'Yüksek performanslı, kullanıma göre değişken güç çeken oyun bilgisayarı.', 'COMPUTING', 'VARIABLE_LOAD',
    850, 80, 700, 2, 10, TRUE, FALSE, TRUE,
    'computer', 'gaming bilgisayar, oyun bilgisayarı, pc', FALSE, TRUE, 290, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000030', 'LAPTOP', 'Dizüstü Bilgisayar',
    'Şarj eğrisiyle çalışan taşınabilir bilgisayar.', 'COMPUTING', 'CHARGING_CURVE',
    130, 15, 100, 0, 3, TRUE, FALSE, FALSE,
    'laptop', 'dizüstü, laptop, notebook', FALSE, TRUE, 300, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000031', 'MONITOR', 'Monitör',
    'Bekleme modu destekli bilgisayar ekranı.', 'COMPUTING', 'STANDBY_DEVICE',
    100, 15, 80, 0.2, 2, TRUE, FALSE, FALSE,
    'monitor', 'monitör, ekran', FALSE, TRUE, 310, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000032', 'PRINTER', 'Yazıcı',
    'Bekleme modu destekli, baskı sırasında yüksek güç çeken yazıcı.', 'COMPUTING', 'STANDBY_DEVICE',
    1000, 10, 800, 0.5, 5, TRUE, FALSE, FALSE,
    'printer', 'yazıcı, printer', FALSE, TRUE, 320, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000033', 'PHONE_CHARGER', 'Telefon Şarj Cihazı',
    'Şarj eğrisiyle çalışan telefon şarj cihazı.', 'COMPUTING', 'CHARGING_CURVE',
    45, 2, 30, 0, 0.5, TRUE, FALSE, FALSE,
    'charger', 'telefon şarj, şarj aleti, adaptör', TRUE, TRUE, 330, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000034', 'TABLET_CHARGER', 'Tablet Şarj Cihazı',
    'Şarj eğrisiyle çalışan tablet şarj cihazı.', 'COMPUTING', 'CHARGING_CURVE',
    60, 5, 45, 0, 0.5, TRUE, FALSE, FALSE,
    'charger', 'tablet şarj, şarj aleti, adaptör', FALSE, TRUE, 340, 1, now(), now());

-- ==================== NETWORK_AND_SMART_HOME (Ağ ve Akıllı Ev) ====================

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000035', 'MODEM', 'Modem',
    'Sürekli açık, dar güç aralığında çalışan internet modemi.', 'NETWORK_AND_SMART_HOME', 'ALWAYS_ON_STABLE',
    30, 5, 20, NULL, NULL, FALSE, FALSE, FALSE,
    'modem', 'modem, internet', TRUE, TRUE, 350, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000036', 'ROUTER', 'Router',
    'Sürekli açık, dar güç aralığında çalışan kablosuz ağ cihazı.', 'NETWORK_AND_SMART_HOME', 'ALWAYS_ON_STABLE',
    40, 5, 30, NULL, NULL, FALSE, FALSE, FALSE,
    'router', 'router, modem, kablosuz, wifi, internet', TRUE, TRUE, 360, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000037', 'MESH_NODE', 'Mesh Wi-Fi Cihazı',
    'Sürekli açık çalışan mesh Wi-Fi ağ cihazı.', 'NETWORK_AND_SMART_HOME', 'ALWAYS_ON_STABLE',
    35, 5, 25, NULL, NULL, FALSE, FALSE, FALSE,
    'meshNode', 'mesh, wifi, ağ genişletici', FALSE, TRUE, 370, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000038', 'SMART_HOME_HUB', 'Akıllı Ev Merkezi',
    'Akıllı ev cihazlarını yöneten, sürekli açık merkez ünite.', 'NETWORK_AND_SMART_HOME', 'ALWAYS_ON_STABLE',
    25, 2, 15, NULL, NULL, FALSE, FALSE, FALSE,
    'smartHomeHub', 'akıllı ev, hub, merkez', FALSE, TRUE, 380, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000039', 'SECURITY_CAMERA', 'Güvenlik Kamerası',
    'Sürekli açık, kayıt durumuna göre gücü değişen güvenlik kamerası.', 'NETWORK_AND_SMART_HOME', 'ALWAYS_ON_VARIABLE',
    30, 3, 20, NULL, NULL, FALSE, FALSE, FALSE,
    'camera', 'güvenlik kamerası, kamera', FALSE, TRUE, 390, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000040', 'SMART_SPEAKER', 'Akıllı Hoparlör',
    'Bekleme modu destekli sesli asistan hoparlörü.', 'NETWORK_AND_SMART_HOME', 'STANDBY_DEVICE',
    30, 2, 20, 1, 4, TRUE, FALSE, FALSE,
    'smartSpeaker', 'akıllı hoparlör, sesli asistan', FALSE, TRUE, 400, 1, now(), now());

-- ==================== LIGHTING (Aydınlatma) ====================

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000041', 'LED_BULB', 'LED Ampul',
    'Aç/kapa kullanılan LED aydınlatma armatürü.', 'LIGHTING', 'MANUAL_SWITCH',
    25, 4, 20, 0, 0, TRUE, FALSE, FALSE,
    'lamp', 'led ampul, ampul, aydınlatma', TRUE, TRUE, 410, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000042', 'DESK_LAMP', 'Masa Lambası',
    'Aç/kapa kullanılan masa aydınlatması.', 'LIGHTING', 'MANUAL_SWITCH',
    50, 5, 40, 0, 0, TRUE, FALSE, FALSE,
    'deskLamp', 'masa lambası, lamba', FALSE, TRUE, 420, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000043', 'CHANDELIER', 'Avize',
    'Aç/kapa kullanılan tavan aydınlatması.', 'LIGHTING', 'MANUAL_SWITCH',
    180, 15, 150, 0, 0, TRUE, FALSE, FALSE,
    'chandelier', 'avize, tavan lambası', FALSE, TRUE, 430, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000044', 'SMART_LAMP', 'Akıllı Lamba',
    'Bekleme modu destekli, uzaktan kontrol edilebilen akıllı lamba.', 'LIGHTING', 'STANDBY_DEVICE',
    25, 4, 20, 0.2, 1, TRUE, FALSE, FALSE,
    'smartLamp', 'akıllı lamba, akıllı ampul', FALSE, TRUE, 440, 1, now(), now());

INSERT INTO appliance_catalog (id, code, display_name, description, category, behavior_profile,
    default_safe_power_limit_watt, default_active_min_watt, default_active_max_watt,
    default_standby_min_watt, default_standby_max_watt,
    supports_standby, supports_schedule, supports_operating_modes,
    icon_key, search_keywords, featured, enabled, display_order, catalog_version, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000045', 'GARDEN_LIGHTING', 'Bahçe Aydınlatması',
    -- The spec leaves active power and safe limit as "configurable" (fixture count varies per
    -- install). Placeholder defaults below assume a modest garden-lighting circuit; users are
    -- expected to adjust safePowerLimitWatt/activeMinWatt/activeMaxWatt per their actual fixture
    -- count when they register this appliance instance.
    'Zamanlanmış olarak çalışan bahçe/dış mekan aydınlatması. Güç değerleri fikstür sayısına göre kullanıcı tarafından ayarlanmalıdır.',
    'LIGHTING', 'SCHEDULED_SWITCH',
    180, 10, 150, 0, 0, TRUE, TRUE, FALSE,
    'gardenLighting', 'bahçe aydınlatma, dış mekan ışık', FALSE, TRUE, 450, 1, now(), now());
