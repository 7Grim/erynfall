-- Migration 001: Add hitpoints_xp and ranged_xp columns missing from initial schema
-- Run this on Azure SQL: erynfall.database.windows.net / erynfall database

ALTER TABLE osrs.players ADD hitpoints_xp BIGINT DEFAULT 1154;  -- level 10 start
ALTER TABLE osrs.players ADD ranged_xp    BIGINT DEFAULT 0;
GO
