-- appliance_catalog holds SYSTEM-DEFINED device templates users pick from when
-- registering an appliance instance in `appliances`. It is NOT a substitute for
-- `appliances` and does not alter that table.
--
-- NOTE: default_* wattage columns are simulation/onboarding DEFAULTS derived from
-- typical consumer appliance specs. They are NOT an electrical safety certification
-- and must not be presented to users or downstream systems as one.
CREATE TABLE appliance_catalog (
    id                             UUID PRIMARY KEY,
    code                           VARCHAR(64) NOT NULL,
    display_name                   VARCHAR(100) NOT NULL,
    description                    VARCHAR(300) NOT NULL,
    category                       VARCHAR(50) NOT NULL,
    behavior_profile               VARCHAR(50) NOT NULL,
    default_safe_power_limit_watt  NUMERIC(12, 3) NOT NULL,
    default_active_min_watt        NUMERIC(12, 3) NOT NULL,
    default_active_max_watt        NUMERIC(12, 3) NOT NULL,
    default_standby_min_watt       NUMERIC(12, 3),
    default_standby_max_watt       NUMERIC(12, 3),
    supports_standby               BOOLEAN NOT NULL DEFAULT FALSE,
    supports_schedule              BOOLEAN NOT NULL DEFAULT FALSE,
    supports_operating_modes       BOOLEAN NOT NULL DEFAULT FALSE,
    icon_key                       VARCHAR(50) NOT NULL,
    search_keywords                VARCHAR(500),
    featured                       BOOLEAN NOT NULL DEFAULT FALSE,
    enabled                        BOOLEAN NOT NULL DEFAULT TRUE,
    display_order                  INTEGER NOT NULL DEFAULT 0,
    catalog_version                INTEGER NOT NULL DEFAULT 1,
    created_at                     TIMESTAMPTZ NOT NULL,
    updated_at                     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_appliance_catalog_code UNIQUE (code),
    CONSTRAINT chk_appliance_catalog_category CHECK (category IN
        ('KITCHEN','CLIMATE','CLEANING','ENTERTAINMENT','COMPUTING',
         'NETWORK_AND_SMART_HOME','LIGHTING','PERSONAL_CARE','OTHER')),
    CONSTRAINT chk_appliance_catalog_behavior_profile CHECK (behavior_profile IN
        ('ALWAYS_ON_STABLE','ALWAYS_ON_VARIABLE','MANUAL_SWITCH','SCHEDULED_SWITCH',
         'STANDBY_DEVICE','SHORT_SESSION','SHORT_HIGH_POWER','PROGRAM_CYCLE',
         'THERMOSTATIC_CYCLE','THERMOSTATIC_SESSION','VARIABLE_LOAD',
         'CHARGING_CURVE','CHARGING_AND_SESSION','FLOW_TRIGGERED')),
    CONSTRAINT chk_appliance_catalog_code_format CHECK (code ~ '^[A-Z0-9_]{2,64}$'),
    CONSTRAINT chk_appliance_catalog_active_min_nonnegative CHECK (default_active_min_watt >= 0),
    CONSTRAINT chk_appliance_catalog_active_range CHECK (default_active_max_watt >= default_active_min_watt),
    CONSTRAINT chk_appliance_catalog_safe_limit_positive CHECK (default_safe_power_limit_watt > 0),
    CONSTRAINT chk_appliance_catalog_standby_range CHECK (
        default_standby_min_watt IS NULL OR default_standby_max_watt IS NULL
        OR default_standby_max_watt >= default_standby_min_watt)
);

CREATE INDEX idx_appliance_catalog_category ON appliance_catalog (category);
CREATE INDEX idx_appliance_catalog_enabled ON appliance_catalog (enabled);
CREATE INDEX idx_appliance_catalog_display_order ON appliance_catalog (display_order);
