-- ╔══════════════════════════════════════════════════════════════════╗
-- ║  Seed: default Standard Shipping carrier + 1 rule per country    ║
-- ║                                                                  ║
-- ║  Without this, /api/v1/shipping/options falls back to a $5 USD   ║
-- ║  flat rate for every country and the storefront banner ("Envío   ║
-- ║  gratis sobre $120") never actually triggers because there's     ║
-- ║  no rule with a free_above threshold to honour.                  ║
-- ║                                                                  ║
-- ║  Seeds a single "Standard Shipping" carrier (code STD) with one  ║
-- ║  rule per country active in the CMS currency table. Default      ║
-- ║  values: rate $10 USD, free shipping >= $120 USD, weight cap     ║
-- ║  15 kg, 5-day delivery estimate, weight bracket 0–30 kg.         ║
-- ║                                                                  ║
-- ║  Idempotent: re-runs are no-ops thanks to NOT EXISTS guards, so  ║
-- ║  this is safe to ship to dev / staging / prod even when admins   ║
-- ║  have already configured carriers manually. Existing rules are   ║
-- ║  never overwritten — the seed only fills *missing* coverage.     ║
-- ╚══════════════════════════════════════════════════════════════════╝

-- 1. Ensure the carrier exists.
INSERT INTO shipping_carriers (id, name, code, active, created_at, created_by)
SELECT gen_random_uuid()::text, 'Standard Shipping', 'STD', true, now(), 'seed'
WHERE  NOT EXISTS (SELECT 1 FROM shipping_carriers WHERE code = 'STD');

-- 2. Seed one rule per country (only if that exact carrier+zone pair is
--    not already configured).
INSERT INTO shipping_rules (
    id, carrier_id, zone,
    min_weight, max_weight,
    rate, free_above, free_above_max_weight,
    estimated_days, active, created_at, created_by
)
SELECT gen_random_uuid()::text,
       c.id,
       country.code,
       0, 30,
       10, 120, 15,
       5, true, now(), 'seed'
FROM   shipping_carriers c
CROSS JOIN (VALUES
        ('CO'), ('US'), ('MX'), ('AR'),
        ('BR'), ('ES'), ('GB'), ('JP')
    ) AS country(code)
WHERE  c.code = 'STD'
  AND  NOT EXISTS (
        SELECT 1 FROM shipping_rules sr
        WHERE  sr.carrier_id = c.id
          AND  sr.zone       = country.code
       );
