-- ============================================================
-- SEED: 15 return requests across various statuses
-- References the 5 existing orders in the system
-- ============================================================

INSERT INTO return_requests (id, order_id, user_id, status, reason, items, refund_amount, created_at, updated_at)
VALUES

-- Order 1: fe78e32c... (NX-20260404-50074) — 3 returns
('RET-001', 'fe78e32c-fca1-4ee4-a3e4-031e9836dfd3', 'jfinol02@gmail.com',
 'REQUESTED', 'El producto llegó con la pantalla rayada desde fábrica',
 '[{"productId":"PRD-001","name":"Samsung Galaxy S24 Ultra","quantity":1}]',
 1349.00, '2026-04-01 10:15:00', NULL),

('RET-002', 'fe78e32c-fca1-4ee4-a3e4-031e9836dfd3', 'jfinol02@gmail.com',
 'REVIEWING', 'Los auriculares no sincronizan con Bluetooth después de 2 días',
 '[{"productId":"PRD-002","name":"Sony WH-1000XM5","quantity":1}]',
 379.00, '2026-04-01 11:30:00', '2026-04-02 09:00:00'),

('RET-003', 'fe78e32c-fca1-4ee4-a3e4-031e9836dfd3', 'jfinol02@gmail.com',
 'APPROVED', 'Recibí el color equivocado, pedí negro y vino gris',
 '[{"productId":"PRD-003","name":"Funda iPhone 15 Pro","quantity":2}]',
 49.98, '2026-03-28 14:00:00', '2026-03-30 10:45:00'),

-- Order 2: ef2a46a7... (NX-20260404-32217) — 3 returns
('RET-004', 'ef2a46a7-1a36-4fe7-a261-d1be32a611d6', 'jfinol02@gmail.com',
 'REJECTED', 'Quiero devolver porque encontré un precio más bajo en otra tienda',
 '[{"productId":"PRD-004","name":"Canon EOS R10 Kit","quantity":1}]',
 899.00, '2026-03-25 08:20:00', '2026-03-27 16:30:00'),

('RET-005', 'ef2a46a7-1a36-4fe7-a261-d1be32a611d6', 'jfinol02@gmail.com',
 'REFUNDED', 'El altavoz tiene distorsión a volumen medio, defecto de fábrica',
 '[{"productId":"PRD-005","name":"JBL Charge 5","quantity":1}]',
 149.00, '2026-03-20 09:45:00', '2026-03-25 11:00:00'),

('RET-006', 'ef2a46a7-1a36-4fe7-a261-d1be32a611d6', 'jfinol02@gmail.com',
 'REQUESTED', 'La talla no corresponde con la guía de tallas del sitio',
 '[{"productId":"PRD-006","name":"Nike Air Max 270","quantity":1}]',
 159.00, '2026-04-03 17:10:00', NULL),

-- Order 3: 57edb277... (NX-20260404-64530) — 3 returns
('RET-007', '57edb277-d4a9-4cb7-ad81-8b20f22748e0', 'jfinol02@gmail.com',
 'REVIEWING', 'El mouse deja de responder intermitentemente por USB',
 '[{"productId":"PRD-007","name":"Logitech MX Master 3S","quantity":1}]',
 109.00, '2026-04-02 13:25:00', '2026-04-03 08:15:00'),

('RET-008', '57edb277-d4a9-4cb7-ad81-8b20f22748e0', 'jfinol02@gmail.com',
 'RETURNED', 'La mochila tiene una costura descosida en el bolsillo frontal',
 '[{"productId":"PRD-008","name":"Mochila Laptop Premium","quantity":1}]',
 79.99, '2026-03-22 10:00:00', '2026-03-28 14:30:00'),

('RET-009', '57edb277-d4a9-4cb7-ad81-8b20f22748e0', 'jfinol02@gmail.com',
 'REFUNDED', 'La batería externa no carga al voltaje indicado en la descripción',
 '[{"productId":"PRD-009","name":"Power Bank 20000mAh","quantity":2}]',
 59.98, '2026-03-15 16:40:00', '2026-03-22 09:20:00'),

-- Order 4: a779d814... (NX-20260404-81979) — 3 returns
('RET-010', 'a779d814-d7d3-496b-8fe3-304d8bfcd358', 'jfinol02@gmail.com',
 'APPROVED', 'Laptop no enciende después de la primera carga completa',
 '[{"productId":"PRD-010","name":"Dell XPS 15 2024","quantity":1}]',
 1899.00, '2026-03-30 07:50:00', '2026-04-01 15:00:00'),

('RET-011', 'a779d814-d7d3-496b-8fe3-304d8bfcd358', 'jfinol02@gmail.com',
 'REQUESTED', 'El reloj muestra datos de salud incorrectos comparado con otro dispositivo',
 '[{"productId":"PRD-011","name":"Apple Watch Series 9","quantity":1}]',
 449.00, '2026-04-04 09:30:00', NULL),

('RET-012', 'a779d814-d7d3-496b-8fe3-304d8bfcd358', 'jfinol02@gmail.com',
 'REVIEWING', 'Llegó el producto sin el cargador que se muestra en las fotos',
 '[{"productId":"PRD-012","name":"GoPro HERO12 Black","quantity":1}]',
 399.00, '2026-04-03 11:05:00', '2026-04-04 10:00:00'),

-- Order 5: 54bf3e7c... (NX-20260405-37833) — 3 returns
('RET-013', '54bf3e7c-3160-4da9-a487-430bdcf80664', 'jfinol02@gmail.com',
 'REFUNDED', 'El producto se dañó durante el envío, caja aplastada',
 '[{"productId":"PRD-013","name":"Monitor LG UltraWide 34","quantity":1}]',
 549.00, '2026-03-18 08:00:00', '2026-03-24 12:30:00'),

('RET-014', '54bf3e7c-3160-4da9-a487-430bdcf80664', 'jfinol02@gmail.com',
 'REJECTED', 'No me gustó el diseño después de verlo en persona',
 '[{"productId":"PRD-014","name":"Teclado Mecánico Keychron K2","quantity":1}]',
 89.00, '2026-03-28 19:15:00', '2026-03-30 11:40:00'),

('RET-015', '54bf3e7c-3160-4da9-a487-430bdcf80664', 'jfinol02@gmail.com',
 'APPROVED', 'Cable USB-C defectuoso, no transfiere datos solo carga',
 '[{"productId":"PRD-015","name":"Cable USB-C 2m Anker","quantity":3}]',
 35.97, '2026-04-02 20:00:00', '2026-04-04 08:45:00')

ON CONFLICT (id) DO NOTHING;
