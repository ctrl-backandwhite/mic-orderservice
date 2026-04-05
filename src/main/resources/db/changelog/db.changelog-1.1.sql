-- =============================================
-- Seed data: coupons (all three types)
-- Idempotent — ON CONFLICT DO NOTHING
-- =============================================

-- 1) PERCENTAGE coupons
INSERT INTO coupons (id, code, type, value, min_order_amount, max_uses, used_count,
                     max_uses_per_user, valid_from, valid_until, active, created_at)
VALUES
    ('seed-coup-welcome10',  'WELCOME10',  'PERCENTAGE', 10.00, NULL,    NULL, 0, 1,
     '2026-01-01T00:00:00Z', '2027-12-31T23:59:59Z', true,  NOW()),
    ('seed-coup-summer25',   'SUMMER25',   'PERCENTAGE', 25.00, 100.00,  200, 0, 2,
     '2026-06-01T00:00:00Z', '2026-08-31T23:59:59Z', true,  NOW()),
    ('seed-coup-flash15',    'FLASH15',    'PERCENTAGE', 15.00, 50.00,   100, 0, 1,
     '2026-04-01T00:00:00Z', '2026-04-30T23:59:59Z', true,  NOW()),
    ('seed-coup-vip20',      'VIP20',      'PERCENTAGE', 20.00, 200.00,   50, 0, 1,
     '2026-01-01T00:00:00Z', '2026-12-31T23:59:59Z', false, NOW())
ON CONFLICT (id) DO NOTHING;

-- 2) FIXED coupons
INSERT INTO coupons (id, code, type, value, min_order_amount, max_uses, used_count,
                     max_uses_per_user, valid_from, valid_until, active, created_at)
VALUES
    ('seed-coup-save50',     'SAVE50',     'FIXED',  50.00, 200.00, 100, 0, 1,
     '2026-01-01T00:00:00Z', '2026-12-31T23:59:59Z', true,  NOW()),
    ('seed-coup-flat25',     'FLAT25',     'FIXED',  25.00, 100.00, NULL, 0, 2,
     '2026-03-01T00:00:00Z', '2026-09-30T23:59:59Z', true,  NOW()),
    ('seed-coup-mega100',    'MEGA100',    'FIXED', 100.00, 500.00,  20, 0, 1,
     '2026-01-01T00:00:00Z', '2027-06-30T23:59:59Z', true,  NOW())
ON CONFLICT (id) DO NOTHING;

-- 3) FREE_SHIPPING coupons
INSERT INTO coupons (id, code, type, value, min_order_amount, max_uses, used_count,
                     max_uses_per_user, valid_from, valid_until, active, created_at)
VALUES
    ('seed-coup-freeship',   'FREESHIP',   'FREE_SHIPPING', 0.00,  75.00, NULL, 0, NULL,
     '2026-01-01T00:00:00Z', '2027-12-31T23:59:59Z', true,  NOW()),
    ('seed-coup-shipfree50', 'SHIPFREE50', 'FREE_SHIPPING', 0.00,  50.00, 500,  0, 2,
     '2026-04-01T00:00:00Z', '2026-06-30T23:59:59Z', true,  NOW())
ON CONFLICT (id) DO NOTHING;
