-- ============================================================
-- CJ Fulfillment Pipeline + Webhooks + Tracking
-- ChangeSet id: 12
-- ============================================================

-- ── Fulfillment pipeline columns on cj_orders ────────────────────────────────
ALTER TABLE cj_orders ADD COLUMN IF NOT EXISTS fulfillment_step   VARCHAR(30);
ALTER TABLE cj_orders ADD COLUMN IF NOT EXISTS fulfillment_error  TEXT;
ALTER TABLE cj_orders ADD COLUMN IF NOT EXISTS pay_id             VARCHAR(50);
ALTER TABLE cj_orders ADD COLUMN IF NOT EXISTS shipments_id       VARCHAR(100);
ALTER TABLE cj_orders ADD COLUMN IF NOT EXISTS cj_actual_payment  NUMERIC(18,2);
ALTER TABLE cj_orders ADD COLUMN IF NOT EXISTS cj_postage_amount  NUMERIC(18,2);
ALTER TABLE cj_orders ADD COLUMN IF NOT EXISTS cj_product_amount  NUMERIC(18,2);
ALTER TABLE cj_orders ADD COLUMN IF NOT EXISTS last_webhook_at    TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_cj_orders_fulfillment_step ON cj_orders(fulfillment_step);
CREATE INDEX IF NOT EXISTS idx_cj_orders_last_webhook_at  ON cj_orders(last_webhook_at);

-- ── Webhook idempotency log ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cj_webhook_log (
    message_id      VARCHAR(64)     NOT NULL,
    type            VARCHAR(50),
    message_type    VARCHAR(50),
    raw_payload     TEXT,
    processed_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_cj_webhook_log PRIMARY KEY (message_id)
);

CREATE INDEX IF NOT EXISTS idx_cj_webhook_log_type ON cj_webhook_log(type);

-- ── CJ order splits (when CJ splits one order into multiple suborders) ────────
CREATE TABLE IF NOT EXISTS cj_order_splits (
    id                      VARCHAR(64)     NOT NULL,
    original_cj_order_id    VARCHAR(200)    NOT NULL,
    split_cj_order_id       VARCHAR(200)    NOT NULL,
    order_id                VARCHAR(64),
    order_status            VARCHAR(50),
    product_list            JSONB,
    split_time              TIMESTAMPTZ,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_cj_order_splits PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_cj_order_splits_original ON cj_order_splits(original_cj_order_id);
CREATE INDEX IF NOT EXISTS idx_cj_order_splits_split    ON cj_order_splits(split_cj_order_id);

-- ── CJ tracking events (from logistics webhooks) ─────────────────────────────
CREATE TABLE IF NOT EXISTS cj_tracking_events (
    id                  VARCHAR(64)     NOT NULL,
    order_id            VARCHAR(64),
    cj_order_id         VARCHAR(200),
    tracking_number     VARCHAR(200),
    tracking_status     VARCHAR(50),
    status_description  VARCHAR(500),
    event_activity      VARCHAR(500),
    event_location      VARCHAR(200),
    event_time          TIMESTAMPTZ,
    logistic_name       VARCHAR(200),
    tracking_url        VARCHAR(500),
    raw_payload         JSONB,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_cj_tracking_events PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_cj_tracking_events_order_id     ON cj_tracking_events(order_id);
CREATE INDEX IF NOT EXISTS idx_cj_tracking_events_track_number ON cj_tracking_events(tracking_number);

-- ── Tracking/last-mile columns on orders ─────────────────────────────────────
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tracking_url               VARCHAR(500);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS last_mile_carrier          VARCHAR(200);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS last_mile_track_number     VARCHAR(200);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS estimated_delivery_days    VARCHAR(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_time              VARCHAR(100);
