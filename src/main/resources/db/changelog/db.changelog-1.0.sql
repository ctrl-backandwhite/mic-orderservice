-- =============================================
-- mic-orderservice: All tables
-- =============================================

-- C.1 Cart
CREATE TABLE IF NOT EXISTS carts (
    id              VARCHAR(64) PRIMARY KEY,
    user_id         VARCHAR(64),
    session_id      VARCHAR(128),
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    expires_at      TIMESTAMP,
    created_at      TIMESTAMP     DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(120),
    updated_by      VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_carts_user_id    ON carts(user_id);
CREATE INDEX IF NOT EXISTS idx_carts_session_id ON carts(session_id);
CREATE INDEX IF NOT EXISTS idx_carts_status     ON carts(status);

CREATE TABLE IF NOT EXISTS cart_items (
    id              VARCHAR(64) PRIMARY KEY,
    cart_id         VARCHAR(64)   NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id      VARCHAR(64)   NOT NULL,
    variant_id      VARCHAR(64),
    quantity        INT           NOT NULL CHECK (quantity > 0),
    unit_price      DECIMAL(12,2) NOT NULL,
    product_name    VARCHAR(255)  NOT NULL,
    product_image   VARCHAR(500),
    created_at      TIMESTAMP     DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(120),
    updated_by      VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id ON cart_items(cart_id);

-- C.2 Shipping & Tax
CREATE TABLE IF NOT EXISTS shipping_carriers (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50)  NOT NULL UNIQUE,
    logo_url        VARCHAR(500),
    active          BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMP    DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(120),
    updated_by      VARCHAR(120)
);

CREATE TABLE IF NOT EXISTS shipping_rules (
    id              VARCHAR(64) PRIMARY KEY,
    carrier_id      VARCHAR(64)   NOT NULL REFERENCES shipping_carriers(id) ON DELETE CASCADE,
    zone            VARCHAR(100)  NOT NULL,
    min_weight      DECIMAL(10,2),
    max_weight      DECIMAL(10,2),
    min_price       DECIMAL(12,2),
    max_price       DECIMAL(12,2),
    rate            DECIMAL(12,2) NOT NULL,
    free_above      DECIMAL(12,2),
    estimated_days  INT           NOT NULL DEFAULT 3,
    active          BOOLEAN       NOT NULL DEFAULT true,
    created_at      TIMESTAMP     DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(120),
    updated_by      VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_shipping_rules_carrier ON shipping_rules(carrier_id);
CREATE INDEX IF NOT EXISTS idx_shipping_rules_zone    ON shipping_rules(zone);

CREATE TABLE IF NOT EXISTS tax_rules (
    id                      VARCHAR(64) PRIMARY KEY,
    country                 VARCHAR(10)   NOT NULL,
    region                  VARCHAR(100),
    rate                    DECIMAL(6,4)  NOT NULL,
    type                    VARCHAR(20)   NOT NULL,
    applies_to_categories   JSONB,
    active                  BOOLEAN       NOT NULL DEFAULT true,
    created_at              TIMESTAMP     DEFAULT NOW(),
    updated_at              TIMESTAMP,
    created_by              VARCHAR(120),
    updated_by              VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_tax_rules_country ON tax_rules(country);

-- C.3 Coupons
CREATE TABLE IF NOT EXISTS coupons (
    id                      VARCHAR(64) PRIMARY KEY,
    code                    VARCHAR(50)   NOT NULL UNIQUE,
    type                    VARCHAR(20)   NOT NULL,
    value                   DECIMAL(12,2) NOT NULL,
    min_order_amount        DECIMAL(12,2),
    max_uses                INT,
    used_count              INT           NOT NULL DEFAULT 0,
    max_uses_per_user       INT,
    valid_from              TIMESTAMP     NOT NULL,
    valid_until             TIMESTAMP     NOT NULL,
    applies_to_categories   JSONB,
    applies_to_products     JSONB,
    active                  BOOLEAN       NOT NULL DEFAULT true,
    created_at              TIMESTAMP     DEFAULT NOW(),
    updated_at              TIMESTAMP,
    created_by              VARCHAR(120),
    updated_by              VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_coupons_code ON coupons(code);

CREATE TABLE IF NOT EXISTS coupon_usages (
    id              VARCHAR(64) PRIMARY KEY,
    coupon_id       VARCHAR(64) NOT NULL REFERENCES coupons(id),
    user_id         VARCHAR(64) NOT NULL,
    order_id        VARCHAR(64),
    used_at         TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_coupon_usages_coupon ON coupon_usages(coupon_id);
CREATE INDEX IF NOT EXISTS idx_coupon_usages_user   ON coupon_usages(user_id);

-- C.4 Orders
CREATE TABLE IF NOT EXISTS orders (
    id                  VARCHAR(64) PRIMARY KEY,
    order_number        VARCHAR(30)   NOT NULL UNIQUE,
    user_id             VARCHAR(64)   NOT NULL,
    status              VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    subtotal            DECIMAL(12,2) NOT NULL,
    shipping_cost       DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax_amount          DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount     DECIMAL(12,2) NOT NULL DEFAULT 0,
    total               DECIMAL(12,2) NOT NULL,
    coupon_id           VARCHAR(64),
    shipping_address    JSONB         NOT NULL,
    billing_address     JSONB,
    payment_method      VARCHAR(50),
    payment_ref         VARCHAR(255),
    notes               TEXT,
    created_at          TIMESTAMP     DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(120),
    updated_by          VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id      ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status        ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_order_number  ON orders(order_number);

CREATE TABLE IF NOT EXISTS order_items (
    id              VARCHAR(64) PRIMARY KEY,
    order_id        VARCHAR(64)   NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id      VARCHAR(64)   NOT NULL,
    variant_id      VARCHAR(64),
    product_name    VARCHAR(255)  NOT NULL,
    product_image   VARCHAR(500),
    sku             VARCHAR(100),
    quantity        INT           NOT NULL CHECK (quantity > 0),
    unit_price      DECIMAL(12,2) NOT NULL,
    total_price     DECIMAL(12,2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);

CREATE TABLE IF NOT EXISTS order_status_history (
    id              VARCHAR(64) PRIMARY KEY,
    order_id        VARCHAR(64) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30) NOT NULL,
    changed_by      VARCHAR(120),
    reason          TEXT,
    changed_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_status_history_order ON order_status_history(order_id);

-- C.5 Tracking
CREATE TABLE IF NOT EXISTS tracking_events (
    id              VARCHAR(64) PRIMARY KEY,
    order_id        VARCHAR(64) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status          VARCHAR(50) NOT NULL,
    description     TEXT,
    location        VARCHAR(200),
    carrier         VARCHAR(100),
    event_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP   DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(120),
    updated_by      VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_tracking_events_order ON tracking_events(order_id);

-- C.6 Returns
CREATE TABLE IF NOT EXISTS return_requests (
    id              VARCHAR(64) PRIMARY KEY,
    order_id        VARCHAR(64)   NOT NULL REFERENCES orders(id),
    user_id         VARCHAR(64)   NOT NULL,
    status          VARCHAR(30)   NOT NULL DEFAULT 'REQUESTED',
    reason          TEXT          NOT NULL,
    items           JSONB         NOT NULL,
    refund_amount   DECIMAL(12,2),
    created_at      TIMESTAMP     DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(120),
    updated_by      VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_return_requests_order  ON return_requests(order_id);
CREATE INDEX IF NOT EXISTS idx_return_requests_user   ON return_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_return_requests_status ON return_requests(status);

-- C.7 Invoices
CREATE TABLE IF NOT EXISTS invoices (
    id                  VARCHAR(64) PRIMARY KEY,
    invoice_number      VARCHAR(30)   NOT NULL UNIQUE,
    order_id            VARCHAR(64)   NOT NULL REFERENCES orders(id),
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    issue_date          DATE          NOT NULL,
    due_date            DATE          NOT NULL,
    subtotal            DECIMAL(12,2) NOT NULL,
    shipping            DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax                 DECIMAL(12,2) NOT NULL DEFAULT 0,
    total               DECIMAL(12,2) NOT NULL,
    payment_method      VARCHAR(50),
    customer_snapshot   JSONB         NOT NULL,
    lines               JSONB         NOT NULL,
    notes               TEXT,
    created_at          TIMESTAMP     DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(120),
    updated_by          VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_invoices_order  ON invoices(order_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);
