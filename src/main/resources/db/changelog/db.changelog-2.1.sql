-- ============================================================
-- CJ Dropshipping Shopping Integration
-- ChangeSet id: 11
-- ============================================================

-- Token singleton for CJ Shopping API auth
CREATE TABLE IF NOT EXISTS cj_tokens (
    id                      VARCHAR(10)     NOT NULL DEFAULT 'SINGLETON',
    access_token            TEXT,
    refresh_token           TEXT,
    access_token_expiry     TIMESTAMPTZ,
    refresh_token_expiry    TIMESTAMPTZ,
    last_token_request_time TIMESTAMPTZ,
    updated_at              TIMESTAMPTZ,
    CONSTRAINT pk_cj_tokens PRIMARY KEY (id)
);

-- CJ order submission tracking
CREATE TABLE IF NOT EXISTS cj_orders (
    id                  VARCHAR(64)     NOT NULL,
    order_id            VARCHAR(64)     NOT NULL,
    cj_order_id         VARCHAR(200),
    shipment_order_id   VARCHAR(200),
    cj_order_status     VARCHAR(50)     NOT NULL DEFAULT 'CREATED',
    track_number        VARCHAR(200),
    logistic_name       VARCHAR(200),
    product_info_list   JSONB,
    last_synced_at      TIMESTAMPTZ,
    error_count         INTEGER         NOT NULL DEFAULT 0,
    last_error          TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    CONSTRAINT pk_cj_orders PRIMARY KEY (id),
    CONSTRAINT uq_cj_orders_order_id UNIQUE (order_id),
    CONSTRAINT fk_cj_orders_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX IF NOT EXISTS idx_cj_orders_cj_status  ON cj_orders(cj_order_status);
CREATE INDEX IF NOT EXISTS idx_cj_orders_error_count ON cj_orders(error_count);

-- Add CJ fields to orders table
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cj_order_id  VARCHAR(200);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS track_number VARCHAR(200);

-- Add CJ logistic name to shipping_carriers
ALTER TABLE shipping_carriers ADD COLUMN IF NOT EXISTS cj_logistic_name VARCHAR(200);
