-- =====================================================================================
-- Customer locale on orders — used by mic-notificationservice to pick the right
-- messages_xx.properties bundle when rendering the invoice email and by the
-- InvoicePdfService to localize the PDF itself.
--
-- Without this column the order service can only infer the language from the
-- shipping country, which fails for multilingual users (e.g. a Spanish speaker
-- with a US address would receive the invoice in English).
-- =====================================================================================

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS customer_locale VARCHAR(8);
