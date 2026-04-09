-- ═══════════════════════════════════════════════════════════════════════
-- Changelog 1.8 – Add selected_attrs (JSONB) to cart_items
-- ═══════════════════════════════════════════════════════════════════════

ALTER TABLE cart_items
    ADD COLUMN IF NOT EXISTS selected_attrs JSONB;

COMMENT ON COLUMN cart_items.selected_attrs IS 'Variant attributes selected by the buyer (e.g. {"Color":"Red","Size":"M"})';
