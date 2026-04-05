-- Migration 011: Add ammo_quantity to players table
-- Required for Phase 1+ ranged combat: persists the exact arrow stack count
-- so players don't lose their ammo on reconnect.

ALTER TABLE osrs.players
    ADD ammo_quantity INT NOT NULL DEFAULT 0;
GO
