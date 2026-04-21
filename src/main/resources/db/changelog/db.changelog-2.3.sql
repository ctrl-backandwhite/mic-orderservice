-- =====================================================================================
-- CJ Integration Fases 2–18 — persistence layer
-- ChangeSet id: 13
--
-- Tables added here back the following phases of docs/cj-dropshipping-integration-plan.md:
--   Fase 2  → app_settings (webhook registration hash)
--   Fase 3  → cj_allowed_countries (whitelist of countries CJ ships to)
--   Fase 5  → order_financial_ledger (inbound/outbound/refund)
--   Fase 8  → cj_api_audit_log, order_state_history, cj_order_state_history,
--             order_item_snapshot
--   Fase 13 → cj_disputes
--   Fase 15 → admin_pii_access_log
-- =====================================================================================

-- ── Fase 2: generic key-value settings (webhook registration idempotency, kill switches…)
CREATE TABLE IF NOT EXISTS app_settings (
    setting_key   VARCHAR(120) NOT NULL,
    setting_value TEXT,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_app_settings PRIMARY KEY (setting_key)
);

-- ── Fase 3: countries allowed for CJ shipping (seed in app startup)
CREATE TABLE IF NOT EXISTS cj_allowed_countries (
    country_code VARCHAR(2)  NOT NULL,
    country_name VARCHAR(80) NOT NULL,
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_cj_allowed_countries PRIMARY KEY (country_code)
);

INSERT INTO cj_allowed_countries (country_code, country_name, active) VALUES
    ('MX', 'Mexico',          TRUE),
    ('US', 'United States',   TRUE),
    ('BR', 'Brazil',          TRUE),
    ('CO', 'Colombia',        TRUE),
    ('CL', 'Chile',           TRUE),
    ('AR', 'Argentina',       TRUE),
    ('ES', 'Spain',           TRUE),
    ('DE', 'Germany',         TRUE),
    ('FR', 'France',          TRUE),
    ('IT', 'Italy',           TRUE),
    ('GB', 'United Kingdom',  TRUE),
    ('PT', 'Portugal',        TRUE),
    ('PE', 'Peru',            TRUE),
    ('UY', 'Uruguay',         TRUE),
    ('CA', 'Canada',          TRUE)
ON CONFLICT (country_code) DO NOTHING;

-- ── Fase 5: financial ledger (customer inbound, CJ outbound, refunds)
CREATE TABLE IF NOT EXISTS order_financial_ledger (
    id              BIGSERIAL     NOT NULL,
    order_id        VARCHAR(64)   NOT NULL,
    entry_type      VARCHAR(20)   NOT NULL,          -- INBOUND | OUTBOUND | REFUND
    amount          NUMERIC(18,4) NOT NULL,
    currency        VARCHAR(3)    NOT NULL,
    fx_rate         NUMERIC(18,6),
    provider        VARCHAR(40),                     -- stripe | paypal | cj | internal
    external_tx_id  VARCHAR(120),
    metadata        JSONB,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_order_financial_ledger PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_order_financial_ledger_order ON order_financial_ledger(order_id);
CREATE INDEX IF NOT EXISTS idx_order_financial_ledger_type  ON order_financial_ledger(entry_type);

-- ── Fase 8.1: audit log of every CJ API call (for forensics)
CREATE TABLE IF NOT EXISTS cj_api_audit_log (
    id              BIGSERIAL     NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    order_id        VARCHAR(64),
    endpoint        VARCHAR(200)  NOT NULL,
    http_status     INT,
    cj_code         VARCHAR(20),
    cj_request_id   VARCHAR(120),
    latency_ms      INT,
    request_body    JSONB,
    response_body   JSONB,
    error_message   TEXT,
    retry_attempt   SMALLINT      DEFAULT 0,
    CONSTRAINT pk_cj_api_audit_log PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_cj_api_audit_created ON cj_api_audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_cj_api_audit_order   ON cj_api_audit_log(order_id);
CREATE INDEX IF NOT EXISTS idx_cj_api_audit_code    ON cj_api_audit_log(cj_code);

-- ── Fase 8.2: event sourcing of every status transition
CREATE TABLE IF NOT EXISTS order_state_history (
    id           BIGSERIAL     NOT NULL,
    order_id     VARCHAR(64)   NOT NULL,
    at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    from_status  VARCHAR(40),
    to_status    VARCHAR(40)   NOT NULL,
    actor        VARCHAR(20)   NOT NULL,             -- SYSTEM | WEBHOOK | ADMIN | CUSTOMER | SCHEDULER
    actor_id     VARCHAR(120),
    reason       TEXT,
    metadata     JSONB,
    CONSTRAINT pk_order_state_history PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_order_state_history_order ON order_state_history(order_id);

CREATE TABLE IF NOT EXISTS cj_order_state_history (
    id            BIGSERIAL     NOT NULL,
    cj_order_id   VARCHAR(200)  NOT NULL,
    order_id      VARCHAR(64),
    at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    from_status   VARCHAR(40),
    to_status     VARCHAR(40)   NOT NULL,
    actor         VARCHAR(20)   NOT NULL,
    reason        TEXT,
    metadata      JSONB,
    CONSTRAINT pk_cj_order_state_history PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_cj_order_state_history_cj_order ON cj_order_state_history(cj_order_id);
CREATE INDEX IF NOT EXISTS idx_cj_order_state_history_order    ON cj_order_state_history(order_id);

-- ── Fase 8.3: immutable snapshot of the product as the customer bought it
CREATE TABLE IF NOT EXISTS order_item_snapshot (
    id             BIGSERIAL     NOT NULL,
    order_id       VARCHAR(64)   NOT NULL,
    order_item_id  VARCHAR(64),
    pid            VARCHAR(64),
    vid            VARCHAR(64),
    name           VARCHAR(500),
    language       VARCHAR(8),
    image_url      VARCHAR(1000),                    -- CJ-hosted URL, no binary stored
    category       VARCHAR(200),
    brand          VARCHAR(200),
    price_usd      NUMERIC(18,4),
    price_customer NUMERIC(18,4),
    currency       VARCHAR(3),
    fx_rate        NUMERIC(18,6),
    stock_at_purchase INT,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_order_item_snapshot PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_order_item_snapshot_order ON order_item_snapshot(order_id);

-- ── Fase 13: disputes / returns / refunds
CREATE TABLE IF NOT EXISTS cj_disputes (
    id             VARCHAR(64)   NOT NULL,
    order_id       VARCHAR(64)   NOT NULL,
    cj_order_id    VARCHAR(200),
    cj_dispute_id  VARCHAR(200),
    reason         VARCHAR(80),
    description    TEXT,
    evidence_urls  JSONB,
    status         VARCHAR(30)   NOT NULL,            -- OPEN | UNDER_REVIEW | APPROVED | REJECTED | CLOSED
    resolution     VARCHAR(30),                       -- FULL_REFUND | PARTIAL_REFUND | REPLACEMENT | REJECTED
    refund_amount  NUMERIC(18,4),
    refund_currency VARCHAR(3),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_cj_disputes PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_cj_disputes_order ON cj_disputes(order_id);
CREATE INDEX IF NOT EXISTS idx_cj_disputes_status ON cj_disputes(status);

-- ── Fase 15: admin access log to customer PII (compliance)
CREATE TABLE IF NOT EXISTS admin_pii_access_log (
    id            BIGSERIAL     NOT NULL,
    at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    admin_user_id VARCHAR(120)  NOT NULL,
    customer_id   VARCHAR(120),
    order_id      VARCHAR(64),
    action        VARCHAR(60)   NOT NULL,           -- VIEW_ADDRESS | VIEW_PHONE | EXPORT | DELETE_REQUEST …
    reason        TEXT,
    source_ip     VARCHAR(45),
    CONSTRAINT pk_admin_pii_access_log PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_admin_pii_access_admin    ON admin_pii_access_log(admin_user_id);
CREATE INDEX IF NOT EXISTS idx_admin_pii_access_customer ON admin_pii_access_log(customer_id);
CREATE INDEX IF NOT EXISTS idx_admin_pii_access_order    ON admin_pii_access_log(order_id);

-- ── Columns on orders for Fase 5 reconciliation & Fase 4 freight snapshot
ALTER TABLE orders ADD COLUMN IF NOT EXISTS paid_by_customer_usd  NUMERIC(18,4);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS expected_cj_cost_usd  NUMERIC(18,4);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS gross_margin_usd      NUMERIC(18,4);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fx_rate_at_payment    NUMERIC(18,6);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS selected_logistic     VARCHAR(120);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS expected_shipping_usd NUMERIC(18,4);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS financial_status      VARCHAR(30);  -- PENDING | RECONCILED | NEEDS_REVIEW | REFUNDED
