-- ╔══════════════════════════════════════════════════════════════════╗
-- ║  Set free-shipping threshold to USD 120                          ║
-- ║                                                                  ║
-- ║  Business rule update: free shipping is now offered from         ║
-- ║  USD 120 of order subtotal (was USD 100). Apply at the DB layer  ║
-- ║  so:                                                             ║
-- ║    1. Existing rules with the previous threshold (100) are       ║
-- ║       bumped to 120.                                             ║
-- ║    2. Existing rules without a threshold (NULL) get the new      ║
-- ║       default so admins don't have to backfill each one.         ║
-- ║    3. Future rows created by the admin form inherit 120 as the   ║
-- ║       column DEFAULT — matches the storefront banner copy.       ║
-- ║                                                                  ║
-- ║  Rules whose admins explicitly set a different threshold (e.g.   ║
-- ║  90, 150 for premium carriers) are left untouched.               ║
-- ╚══════════════════════════════════════════════════════════════════╝

ALTER TABLE shipping_rules ALTER COLUMN free_above SET DEFAULT 120;

UPDATE shipping_rules
SET    free_above = 120,
       updated_at = now()
WHERE  free_above IS NULL
   OR  free_above = 100;
