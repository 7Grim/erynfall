-- Migration 012: Add selected_spell_id column to persist the player's auto-cast spell selection.
-- -1 = no spell selected (default); valid range: 1-8 matching SpellRegistry spell IDs.
ALTER TABLE osrs.players
    ADD selected_spell_id INT NOT NULL DEFAULT -1;
GO
