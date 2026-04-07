-- =============================================
-- Payment breakdown: gift card, loyalty, coupon
-- on orders and invoices
-- =============================================

-- Orders: gift card + loyalty fields
ALTER TABLE orders ADD COLUMN IF NOT EXISTS gift_card_code      VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS gift_card_amount    DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS loyalty_points_used INT           NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS loyalty_discount    DECIMAL(12,2) NOT NULL DEFAULT 0;

-- Invoices: coupon discount + gift card + loyalty
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS discount_amount   DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS gift_card_amount  DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS loyalty_discount  DECIMAL(12,2) NOT NULL DEFAULT 0;
