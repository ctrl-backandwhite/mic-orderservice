-- Migrate shipping_rules.zone from descriptive names to ISO 3166-1 alpha-2 country codes
-- to match the active-currency country codes used in the frontend.

UPDATE shipping_rules SET zone = 'ES' WHERE zone IN ('España', 'España Peninsular', 'Baleares', 'Canarias');
UPDATE shipping_rules SET zone = 'PT' WHERE zone = 'Portugal';
UPDATE shipping_rules SET zone = 'US' WHERE zone = 'Estados Unidos';
UPDATE shipping_rules SET zone = 'GB' WHERE zone = 'Reino Unido';
UPDATE shipping_rules SET zone = 'FR' WHERE zone = 'Unión Europea';
UPDATE shipping_rules SET zone = 'MX' WHERE zone = 'México';
UPDATE shipping_rules SET zone = 'BR' WHERE zone = 'Brasil';
UPDATE shipping_rules SET zone = 'CO' WHERE zone = 'Colombia';
UPDATE shipping_rules SET zone = 'AR' WHERE zone = 'Argentina';
