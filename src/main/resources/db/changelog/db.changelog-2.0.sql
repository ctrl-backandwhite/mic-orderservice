-- ============================================================
-- Saga Pattern: Add saga_status column to orders table
-- ChangeSet id: 10
-- ============================================================

ALTER TABLE orders ADD COLUMN IF NOT EXISTS saga_status VARCHAR(30) DEFAULT 'CREATED';

CREATE INDEX IF NOT EXISTS idx_orders_saga_status ON orders(saga_status);
