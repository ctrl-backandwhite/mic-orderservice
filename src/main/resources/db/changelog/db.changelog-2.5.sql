-- ╔══════════════════════════════════════════════════════════════════╗
-- ║  Persist the invoice's currency code                             ║
-- ║                                                                  ║
-- ║  The Invoice domain has carried a currencyCode for a while but   ║
-- ║  the entity / table never persisted it, so the storefront could  ║
-- ║  only show invoices in whatever currency the buyer happened to   ║
-- ║  have selected when looking at them. Add the column and backfill ║
-- ║  legacy rows from the related order so historic invoices keep    ║
-- ║  rendering with the currency they were charged in.               ║
-- ╚══════════════════════════════════════════════════════════════════╝

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS currency_code VARCHAR(3);

-- Backfill: pull the currency from the parent order. Defaults to USD
-- when the order itself doesn't have one set.
UPDATE invoices i
SET currency_code = COALESCE(o.currency_code, 'USD')
FROM orders o
WHERE i.order_id = o.id
  AND i.currency_code IS NULL;

UPDATE invoices SET currency_code = 'USD' WHERE currency_code IS NULL;
