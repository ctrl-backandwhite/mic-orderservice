-- Add multi-currency support to orders table
ALTER TABLE orders ADD COLUMN currency_code VARCHAR(3) NOT NULL DEFAULT 'USD';
ALTER TABLE orders ADD COLUMN exchange_rate_to_usd DECIMAL(18,8) NOT NULL DEFAULT 1.00000000;
