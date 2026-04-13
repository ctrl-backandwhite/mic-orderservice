-- Campaign tracking: add campaign fields to order_items and orders
ALTER TABLE order_items ADD COLUMN campaign_id VARCHAR(64);
ALTER TABLE order_items ADD COLUMN campaign_discount DECIMAL(12,2);

ALTER TABLE orders ADD COLUMN campaign_discount_total DECIMAL(12,2) DEFAULT 0;
