-- Migration 013: Persist active prayer bitmask.
-- Bit (prayer_id - 1) set = that prayer is active at logout.
-- Prayer IDs 1-6 occupy bits 0-5 of a 32-bit INT.
-- Note: prayer_points column already exists; savePlayer() now writes actual current
-- points instead of always writing the prayer level. No column change required for it.
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('erynfall.players') AND name = 'active_prayer_mask'
)
    ALTER TABLE erynfall.players ADD active_prayer_mask INT NOT NULL DEFAULT 0;
GO
