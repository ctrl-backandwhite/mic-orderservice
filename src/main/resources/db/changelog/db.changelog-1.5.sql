-- ═══════════════════════════════════════════════════════════════════════
-- Fix: clear max_price for rules with freeAbove
-- The max_price column was capping which orders the rule applied to, so
-- once the subtotal exceeded freeAbove the rule disappeared instead of
-- showing as free shipping.  Rules with freeAbove should have no upper
-- price cap — the freeAbove logic in the application layer handles the
-- rate → 0 transition.
-- ═══════════════════════════════════════════════════════════════════════
UPDATE shipping_rules SET max_price = NULL WHERE free_above IS NOT NULL;
