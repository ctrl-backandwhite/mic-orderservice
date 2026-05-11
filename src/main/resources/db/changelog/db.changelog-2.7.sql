-- ╔══════════════════════════════════════════════════════════════════╗
-- ║  Add weight cap for free-shipping promo                          ║
-- ║                                                                  ║
-- ║  Business rule: free shipping (subtotal >= free_above) only      ║
-- ║  applies when the shipment weight is <= 15 kg. Heavier orders    ║
-- ║  pay the full rate even if the subtotal qualifies — protects     ║
-- ║  margin on bulky packages.                                       ║
-- ║                                                                  ║
-- ║  Stored per-rule so admins can override (e.g. premium carrier    ║
-- ║  with no weight cap, or stricter 5 kg cap on a partner). NULL    ║
-- ║  means "no weight cap on the promo".                             ║
-- ╚══════════════════════════════════════════════════════════════════╝

ALTER TABLE shipping_rules
    ADD COLUMN IF NOT EXISTS free_above_max_weight DECIMAL(10,2);

ALTER TABLE shipping_rules
    ALTER COLUMN free_above_max_weight SET DEFAULT 15.00;

-- Backfill: every existing rule with a free-shipping threshold gets the
-- 15 kg cap. Rules without a free_above stay NULL (no promo, no cap).
UPDATE shipping_rules
SET    free_above_max_weight = 15.00,
       updated_at = now()
WHERE  free_above IS NOT NULL
  AND  free_above_max_weight IS NULL;
