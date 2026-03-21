-- ============================================================================
-- OSRS-MMORP Complete Database Schema for SQL Server 2025
-- ============================================================================
-- Classification: MASTER DATABASE SETUP SCRIPT
-- Purpose: Initialize complete OSRS-MMORP database with all tables, indices,
--          views, stored procedures, and constraints
-- Created: 2026-03-21
-- Usage: Run entire script in SSMS Query Editor (paste all at once)
-- ============================================================================

-- ============================================================================
-- PART 0: DATABASE CREATION & CONFIGURATION
-- ============================================================================

-- Create database if it doesn't exist
USE [master];
GO

IF EXISTS (SELECT 1 FROM sys.databases WHERE name = N'osrsmmorp')
BEGIN
  ALTER DATABASE [osrsmmorp] SET OFFLINE WITH ROLLBACK IMMEDIATE;
  DROP DATABASE [osrsmmorp];
  WAITFOR DELAY '00:00:02';
END
GO

CREATE DATABASE [osrsmmorp];
GO

-- Switch to the new database
USE [osrsmmorp];
GO

-- Enable snapshot isolation for better concurrency
ALTER DATABASE [osrsmmorp] SET ALLOW_SNAPSHOT_ISOLATION ON;
GO

ALTER DATABASE [osrsmmorp] SET READ_COMMITTED_SNAPSHOT ON;
GO

-- ============================================================================
-- PART 1: SCHEMA CREATION
-- ============================================================================

-- Create schema for all OSRS objects
IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'osrs')
BEGIN
  EXEC sp_executesql N'CREATE SCHEMA osrs';
  PRINT 'SCHEMA [osrs] created.';
END
ELSE
BEGIN
  PRINT 'SCHEMA [osrs] already exists.';
END
GO

-- ============================================================================
-- PART 2: CONFIGURATION TABLES
-- ============================================================================

-- Configuration lookup table (for server settings)
IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'config' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.config;
  PRINT 'TABLE osrs.config dropped.';
END
GO

CREATE TABLE osrs.config (
  config_key VARCHAR(100) PRIMARY KEY,
  config_value VARCHAR(255) NOT NULL,
  description VARCHAR(500),
  updated_at DATETIME2 DEFAULT GETDATE(),
  CONSTRAINT pk_config PRIMARY KEY (config_key)
);
GO

INSERT INTO osrs.config (config_key, config_value, description)
VALUES
  ('TICK_RATE_HZ', '256', 'Server tick rate in Hz'),
  ('TICK_INTERVAL_NS', '3906250', 'Nanoseconds per tick (1B / 256)'),
  ('MAX_PLAYERS_MVP', '1', 'Maximum concurrent players for MVP'),
  ('COMBAT_RANGE_TILES', '2', 'Melee combat range in tiles'),
  ('ITEM_DESPAWN_HOURS', '1', 'Hours before ground loot despawns'),
  ('NPC_RESPAWN_TICKS', '600', 'Ticks before NPC respawns'),
  ('MAX_INVENTORY_SLOTS', '28', 'Hard inventory limit per player'),
  ('IDLE_DISCONNECT_MINUTES', '30', 'Minutes of inactivity before disconnect'),
  ('MAX_XP_PER_TICK', '10000', 'Maximum XP gain per single tick'),
  ('MAX_TOTAL_XP', '200000000', 'Maximum XP per skill (Level 99)'),
  ('LEVEL_99_XP', '200000000', 'Exact XP required for level 99'),
  ('PAYMENT_FEE_PERCENT', '2', 'Grand Exchange fee percentage');
GO

PRINT 'TABLE osrs.config seeded with defaults.';

-- ============================================================================
-- PART 3: ITEMS REFERENCE TABLE
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'items' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.items;
  PRINT 'TABLE osrs.items dropped.';
END
GO

CREATE TABLE osrs.items (
  id INT PRIMARY KEY NOT NULL,
  name VARCHAR(255) NOT NULL,
  examine_text VARCHAR(500),
  stackable BIT DEFAULT 0,
  weight_kg FLOAT DEFAULT 0,
  tradeable BIT DEFAULT 1,
  high_alchemy_value INT DEFAULT 0,
  low_alchemy_value INT DEFAULT 0,
  ge_price_low INT,
  ge_price_high INT,
  created_at DATETIME2 DEFAULT GETDATE(),
  updated_at DATETIME2 DEFAULT GETDATE(),
  
  CONSTRAINT pk_items PRIMARY KEY (id),
  CONSTRAINT ck_items_stackable CHECK (stackable IN (0, 1)),
  CONSTRAINT ck_items_tradeable CHECK (tradeable IN (0, 1)),
  CONSTRAINT ck_items_weight CHECK (weight_kg >= 0),
  CONSTRAINT ck_items_alchemy CHECK (high_alchemy_value >= 0 AND low_alchemy_value >= 0)
);
GO

-- Create indices on items table
CREATE INDEX idx_items_name ON osrs.items(name);
CREATE INDEX idx_items_stackable ON osrs.items(stackable) WHERE stackable = 1;
CREATE INDEX idx_items_tradeable ON osrs.items(tradeable) WHERE tradeable = 1;
GO

-- Seed items table with MVP items
INSERT INTO osrs.items (id, name, examine_text, stackable, weight_kg, tradeable, high_alchemy_value, low_alchemy_value)
VALUES
  -- Combat drops (bones, meat, hides)
  (1, 'Bones', 'These are bones. You may bury them for prayer experience.', 1, 0.1, 1, 5, 2),
  (2, 'Raw Chicken', 'Some chicken - completely raw. You need to cook it.', 1, 0.05, 1, 0, 0),
  (3, 'Feathers', 'Light, fluffy feathers. Useful for fletching.', 1, 0.01, 1, 0, 0),
  (4, 'Raw Beef', 'Raw beef - cook it first before eating!', 1, 0.08, 1, 0, 0),
  (5, 'Cowhide', 'The hide from a cow. Can be crafted into leather.', 1, 0.12, 1, 20, 10),
  
  -- Gathering
  (6, 'Logs', 'A log of wood. Can be fletched or burned.', 1, 0.1, 1, 20, 10),
  (7, 'Raw Fish', 'Raw fish - needs cooking before eating.', 1, 0.2, 1, 0, 0),
  
  -- Weapons (non-stackable)
  (100, 'Bronze Dagger', 'A bronze dagger. One of the weakest weapons.', 0, 0.5, 1, 16, 5),
  (101, 'Iron Dagger', 'An iron dagger. Better than bronze.', 0, 0.5, 1, 32, 10),
  (102, 'Bronze Sword', 'A bronze sword. Requires 4 attack to wield.', 0, 0.7, 1, 48, 15),
  (103, 'Iron Sword', 'An iron sword. Requires 10 attack to wield.', 0, 0.7, 1, 120, 40),
  
  -- Cooked food
  (200, 'Cooked Chicken', 'A cooked chicken. Heals 3 hp when eaten.', 1, 0.05, 1, 0, 0),
  (201, 'Cooked Beef', 'A cooked beef. Heals 5 hp when eaten.', 1, 0.08, 1, 0, 0),
  (202, 'Cooked Fish', 'A cooked fish. Heals 3 hp when eaten.', 1, 0.2, 1, 0, 0),
  
  -- Currency
  (995, 'Coins', 'Legal tender. Use at shops to buy goods.', 1, 0.005, 1, 1, 1);
GO

PRINT 'TABLE osrs.items seeded with 15 items.';

-- ============================================================================
-- PART 4: NPC DEFINITIONS
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'npcs' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.npcs;
  PRINT 'TABLE osrs.npcs dropped.';
END
GO

CREATE TABLE osrs.npcs (
  id INT PRIMARY KEY NOT NULL,
  name VARCHAR(255) NOT NULL,
  combat_level INT DEFAULT 1 CHECK (combat_level >= 1 AND combat_level <= 138),
  max_hp INT DEFAULT 10 CHECK (max_hp > 0),
  attack_level INT DEFAULT 1,
  strength_level INT DEFAULT 1,
  defence_level INT DEFAULT 1,
  magic_level INT DEFAULT 1,
  ranged_level INT DEFAULT 1,
  spawn_x INT NOT NULL,
  spawn_y INT NOT NULL,
  spawn_id INT NOT NULL,
  respawn_ticks INT DEFAULT 600,
  max_alive INT DEFAULT 5,
  size INT DEFAULT 1 CHECK (size IN (1, 2, 3, 4)),  -- Hitbox size in tiles
  attackable BIT DEFAULT 1,
  aggressive BIT DEFAULT 0,  -- Attacks unprovoked
  description VARCHAR(500),
  created_at DATETIME2 DEFAULT GETDATE(),
  
  CONSTRAINT pk_npcs PRIMARY KEY (id),
  CONSTRAINT ck_npc_levels CHECK (attack_level >= 1 AND strength_level >= 1 AND defence_level >= 1)
);
GO

CREATE INDEX idx_npcs_spawn ON osrs.npcs(spawn_id);
CREATE INDEX idx_npcs_location ON osrs.npcs(spawn_x, spawn_y);
CREATE INDEX idx_npcs_combat_level ON osrs.npcs(combat_level);
GO

-- Seed NPC table
INSERT INTO osrs.npcs (id, name, combat_level, max_hp, attack_level, strength_level, defence_level, spawn_x, spawn_y, spawn_id, respawn_ticks, max_alive, size, attackable, aggressive, description)
VALUES
  (1, 'Chicken', 1, 3, 1, 1, 1, 3230, 3205, 1, 600, 5, 1, 1, 0, 'A docile poultry creature. Drops raw chicken, bones, and feathers.'),
  (2, 'Goblin', 5, 6, 5, 5, 5, 3210, 3215, 2, 600, 3, 1, 1, 0, 'A small green humanoid. Drops bones and goblin mail.'),
  (3, 'Cow', 10, 10, 10, 10, 10, 3245, 3190, 3, 600, 8, 2, 1, 0, 'A bovine creature. Drops raw beef, bones, and cowhide.');
GO

PRINT 'TABLE osrs.npcs seeded with 3 NPC types.';

-- ============================================================================
-- PART 5: LOOT TABLES (NPC Drop Configuration)
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'npc_loot_tables' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.npc_loot_tables;
  PRINT 'TABLE osrs.npc_loot_tables dropped.';
END
GO

CREATE TABLE osrs.npc_loot_tables (
  id INT IDENTITY(1,1) PRIMARY KEY,
  npc_id INT NOT NULL REFERENCES osrs.npcs(id) ON DELETE CASCADE,
  item_id INT NOT NULL REFERENCES osrs.items(id),
  quantity_min INT NOT NULL DEFAULT 1 CHECK (quantity_min > 0),
  quantity_max INT NOT NULL DEFAULT 1 CHECK (quantity_max >= quantity_min),
  drop_rate INT NOT NULL CHECK (drop_rate > 0 AND drop_rate <= 10000),  -- Out of 10000 (0.01% to 100%)
  always_drop BIT DEFAULT 0,  -- If 1, this item always drops
  sort_order INT DEFAULT 0,  -- For multiple guaranteed drops
  
  CONSTRAINT pk_loot_tables PRIMARY KEY (id),
  INDEX idx_loot_npc (npc_id),
  INDEX idx_loot_item (item_id)
);
GO

-- Chicken loot table (always: bones, raw chicken; rare: feathers)
INSERT INTO osrs.npc_loot_tables (npc_id, item_id, quantity_min, quantity_max, drop_rate, always_drop, sort_order)
VALUES
  (1, 1, 1, 1, 10000, 1, 0),     -- Bones always
  (1, 2, 1, 1, 10000, 1, 1),     -- Raw Chicken always
  (1, 3, 5, 15, 10000, 0, 2);    -- Feathers 5-15 (always)
GO

-- Goblin loot table
INSERT INTO osrs.npc_loot_tables (npc_id, item_id, quantity_min, quantity_max, drop_rate, always_drop, sort_order)
VALUES
  (2, 1, 1, 1, 10000, 1, 0),     -- Bones always
  (2, 4, 1, 1, 10000, 0, 1);     -- Raw Beef sometimes
GO

-- Cow loot table
INSERT INTO osrs.npc_loot_tables (npc_id, item_id, quantity_min, quantity_max, drop_rate, always_drop, sort_order)
VALUES
  (3, 1, 1, 1, 10000, 1, 0),     -- Bones always
  (3, 4, 1, 1, 10000, 1, 1),     -- Raw Beef always
  (3, 5, 1, 1, 10000, 1, 2);     -- Cowhide always
GO

PRINT 'TABLE osrs.npc_loot_tables seeded.';

-- ============================================================================
-- PART 6: PLAYERS TABLE (Core Player Data)
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'players' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.players;
  PRINT 'TABLE osrs.players dropped.';
END
GO

CREATE TABLE osrs.players (
  id INT IDENTITY(1,1) PRIMARY KEY NOT NULL,
  username VARCHAR(12) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  created_at DATETIME2 DEFAULT GETDATE(),
  last_login DATETIME2,
  last_logout DATETIME2,
  
  -- Position in world (start at Lumbridge)
  x INT DEFAULT 3222,
  y INT DEFAULT 3218,
  
  -- ===== SKILLS (8 MVP skills) =====
  -- XP only; levels derived via cube root formula
  attack_xp BIGINT DEFAULT 0 CHECK (attack_xp >= 0),
  strength_xp BIGINT DEFAULT 0 CHECK (strength_xp >= 0),
  defence_xp BIGINT DEFAULT 0 CHECK (defence_xp >= 0),
  magic_xp BIGINT DEFAULT 0 CHECK (magic_xp >= 0),
  prayer_xp BIGINT DEFAULT 0 CHECK (prayer_xp >= 0),
  prayer_points INT DEFAULT 10 CHECK (prayer_points >= 0),
  woodcutting_xp BIGINT DEFAULT 0 CHECK (woodcutting_xp >= 0),
  fishing_xp BIGINT DEFAULT 0 CHECK (fishing_xp >= 0),
  cooking_xp BIGINT DEFAULT 0 CHECK (cooking_xp >= 0),
  
  -- Economy
  total_gold BIGINT DEFAULT 0 CHECK (total_gold >= 0),
  total_questpoints INT DEFAULT 0 CHECK (total_questpoints >= 0 AND total_questpoints <= 1000),
  
  -- Authority-Server Validation
  last_action_tick BIGINT DEFAULT 0,
  pending_actions INT DEFAULT 0 CHECK (pending_actions >= 0 AND pending_actions <= 10),
  
  -- Death State Management
  is_dead BIT DEFAULT 0,
  death_tick BIGINT,
  death_x INT,
  death_y INT,
  
  -- Session tracking
  current_session_id INT,
  
  CONSTRAINT pk_players PRIMARY KEY (id),
  CONSTRAINT ck_players_username_length CHECK (LEN(username) >= 3 AND LEN(username) <= 12),
  CONSTRAINT ck_players_xp_sane CHECK (
    attack_xp <= 200000000 AND
    strength_xp <= 200000000 AND
    defence_xp <= 200000000 AND
    magic_xp <= 200000000 AND
    prayer_xp <= 200000000 AND
    woodcutting_xp <= 200000000 AND
    fishing_xp <= 200000000 AND
    cooking_xp <= 200000000
  )
);
GO

-- Create critical indices
CREATE UNIQUE INDEX idx_players_username ON osrs.players(username);
CREATE INDEX idx_players_created_at ON osrs.players(created_at);
CREATE INDEX idx_players_last_login ON osrs.players(last_login);
CREATE INDEX idx_players_location ON osrs.players(x, y);
CREATE INDEX idx_players_dead ON osrs.players(is_dead) WHERE is_dead = 1;
GO

PRINT 'TABLE osrs.players created.';

-- ============================================================================
-- PART 7: INVENTORY TABLE (28-Slot Hard Limit)
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'inventory' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.inventory;
  PRINT 'TABLE osrs.inventory dropped.';
END
GO

CREATE TABLE osrs.inventory (
  id INT IDENTITY(1,1) PRIMARY KEY NOT NULL,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  slot_index INT NOT NULL CHECK (slot_index >= 0 AND slot_index < 28),
  item_id INT NOT NULL REFERENCES osrs.items(id),
  quantity INT DEFAULT 1 CHECK (quantity > 0),
  
  CONSTRAINT pk_inventory PRIMARY KEY (id),
  CONSTRAINT uq_inventory_slot UNIQUE (player_id, slot_index),
  CONSTRAINT ck_inventory_quantity_stackable CHECK (
    quantity <= CASE 
      WHEN item_id = 995 THEN 2147483647  -- Coins: INT max
      ELSE (SELECT CASE WHEN stackable = 1 THEN 2147483647 ELSE 1 END FROM osrs.items WHERE osrs.items.id = item_id)
    END
  )
);
GO

CREATE INDEX idx_inventory_player ON osrs.inventory(player_id);
CREATE INDEX idx_inventory_item ON osrs.inventory(item_id);
GO

PRINT 'TABLE osrs.inventory created.';

-- ============================================================================
-- PART 8: GROUND LOOT TABLE
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'ground_loot' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.ground_loot;
  PRINT 'TABLE osrs.ground_loot dropped.';
END
GO

CREATE TABLE osrs.ground_loot (
  id INT IDENTITY(1,1) PRIMARY KEY NOT NULL,
  item_id INT NOT NULL REFERENCES osrs.items(id),
  quantity INT NOT NULL CHECK (quantity > 0),
  x INT NOT NULL,
  y INT NOT NULL,
  owner_id INT REFERENCES osrs.players(id) ON DELETE SET NULL,
  dropped_at DATETIME2 DEFAULT GETDATE(),
  despawn_at DATETIME2,
  
  CONSTRAINT pk_ground_loot PRIMARY KEY (id),
  INDEX idx_ground_loot_location (x, y),
  INDEX idx_ground_loot_despawn (despawn_at),
  INDEX idx_ground_loot_item (item_id)
);
GO

PRINT 'TABLE osrs.ground_loot created.';

-- ============================================================================
-- PART 9: QUESTS TABLE
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'player_quests' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.player_quests;
  PRINT 'TABLE osrs.player_quests dropped.';
END
GO

CREATE TABLE osrs.player_quests (
  id INT IDENTITY(1,1) PRIMARY KEY NOT NULL,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  quest_id INT NOT NULL,
  status INT DEFAULT 0 CHECK (status IN (0, 1, 2)),  -- 0=not started, 1=in progress, 2=complete
  completed_objectives INT DEFAULT 0,  -- Bitmask for which objectives done
  started_at DATETIME2 DEFAULT GETDATE(),
  completed_at DATETIME2,
  
  CONSTRAINT pk_player_quests PRIMARY KEY (id),
  CONSTRAINT uq_player_quest UNIQUE (player_id, quest_id),
  INDEX idx_player_quests_player (player_id, quest_id),
  INDEX idx_player_quests_status (player_id, status)
);
GO

PRINT 'TABLE osrs.player_quests created.';

-- ============================================================================
-- PART 10: GRAND EXCHANGE ORDERS
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'ge_orders' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.ge_orders;
  PRINT 'TABLE osrs.ge_orders dropped.';
END
GO

CREATE TABLE osrs.ge_orders (
  id INT IDENTITY(1,1) PRIMARY KEY NOT NULL,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  item_id INT NOT NULL REFERENCES osrs.items(id),
  quantity INT NOT NULL CHECK (quantity > 0),
  price_per_unit INT NOT NULL CHECK (price_per_unit > 0),
  is_buy BIT NOT NULL,
  filled_quantity INT DEFAULT 0 CHECK (filled_quantity >= 0 AND filled_quantity <= quantity),
  created_at DATETIME2 DEFAULT GETDATE(),
  completed_at DATETIME2,
  
  CONSTRAINT pk_ge_orders PRIMARY KEY (id),
  -- FILTERED INDEX for active order matching (performance critical)
  INDEX idx_ge_matching ON osrs.ge_orders (
    item_id, is_buy, price_per_unit DESC, created_at, filled_quantity
  ) WHERE completed_at IS NULL,
  
  INDEX idx_ge_player ON osrs.ge_orders(player_id, completed_at),
  INDEX idx_ge_created ON osrs.ge_orders(created_at),
  INDEX idx_ge_item ON osrs.ge_orders(item_id)
);
GO

PRINT 'TABLE osrs.ge_orders created.';

-- ============================================================================
-- PART 11: TRADE HISTORY (Audit & Analytics)
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'trade_history' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.trade_history;
  PRINT 'TABLE osrs.trade_history dropped.';
END
GO

CREATE TABLE osrs.trade_history (
  id INT IDENTITY(1,1) PRIMARY KEY NOT NULL,
  initiator_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  recipient_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  item_id INT NOT NULL REFERENCES osrs.items(id),
  quantity INT NOT NULL CHECK (quantity > 0),
  completed_at DATETIME2 DEFAULT GETDATE(),
  
  CONSTRAINT pk_trade_history PRIMARY KEY (id),
  INDEX idx_trade_initiator (initiator_id, completed_at),
  INDEX idx_trade_recipient (recipient_id, completed_at),
  INDEX idx_trade_item (item_id)
);
GO

PRINT 'TABLE osrs.trade_history created.';

-- ============================================================================
-- PART 12: ACHIEVEMENTS TABLE
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'player_achievements' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.player_achievements;
  PRINT 'TABLE osrs.player_achievements dropped.';
END
GO

CREATE TABLE osrs.player_achievements (
  id INT IDENTITY(1,1) PRIMARY KEY NOT NULL,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  achievement_id INT NOT NULL,
  unlocked_at DATETIME2 DEFAULT GETDATE(),
  progress INT DEFAULT 0 CHECK (progress >= 0),
  
  CONSTRAINT pk_player_achievements PRIMARY KEY (id),
  CONSTRAINT uq_player_achievement UNIQUE (player_id, achievement_id),
  INDEX idx_achievements_player (player_id)
);
GO

PRINT 'TABLE osrs.player_achievements created.';

-- ============================================================================
-- PART 13: CHAT MESSAGES TABLE
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'chat_messages' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.chat_messages;
  PRINT 'TABLE osrs.chat_messages dropped.';
END
GO

CREATE TABLE osrs.chat_messages (
  id INT IDENTITY(1,1) PRIMARY KEY NOT NULL,
  sender_id INT REFERENCES osrs.players(id) ON DELETE SET NULL,
  sender_name VARCHAR(12),
  message_text VARCHAR(255) NOT NULL,
  chat_type INT DEFAULT 0 CHECK (chat_type IN (0, 1)),  -- 0=public, 1=private
  recipient_id INT REFERENCES osrs.players(id) ON DELETE SET NULL,
  created_at DATETIME2 DEFAULT GETDATE(),
  
  CONSTRAINT pk_chat_messages PRIMARY KEY (id),
  INDEX idx_chat_sender (sender_id, created_at),
  INDEX idx_chat_recipient (recipient_id, created_at),
  INDEX idx_chat_created (created_at)
);
GO

PRINT 'TABLE osrs.chat_messages created.';

-- ============================================================================
-- PART 14: PLAYER SESSIONS (Online Tracking)
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'player_sessions' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.player_sessions;
  PRINT 'TABLE osrs.player_sessions dropped.';
END
GO

CREATE TABLE osrs.player_sessions (
  id INT IDENTITY(1,1) PRIMARY KEY NOT NULL,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  session_start DATETIME2 DEFAULT GETDATE(),
  session_end DATETIME2,
  last_heartbeat DATETIME2 DEFAULT GETDATE(),
  ip_address VARCHAR(45),
  status INT DEFAULT 0 CHECK (status IN (0, 1, 2)),  -- 0=online, 1=offline, 2=idle
  
  CONSTRAINT pk_player_sessions PRIMARY KEY (id),
  INDEX idx_sessions_player (player_id, last_heartbeat),
  INDEX idx_sessions_heartbeat (last_heartbeat),
  INDEX idx_sessions_status (status)
);
GO

PRINT 'TABLE osrs.player_sessions created.';

-- ============================================================================
-- PART 15: PROCESSED PACKETS (Anti-Duplication)
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'processed_packets' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.processed_packets;
  PRINT 'TABLE osrs.processed_packets dropped.';
END
GO

CREATE TABLE osrs.processed_packets (
  id INT IDENTITY(1,1) PRIMARY KEY NOT NULL,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  packet_id BIGINT NOT NULL,
  processed_at DATETIME2 DEFAULT GETDATE(),
  
  CONSTRAINT pk_processed_packets PRIMARY KEY (id),
  CONSTRAINT uq_processed_packet UNIQUE (player_id, packet_id),
  INDEX idx_packets_cleanup (processed_at)
);
GO

PRINT 'TABLE osrs.processed_packets created.';

-- ============================================================================
-- PART 16: VIEWS
-- ============================================================================

-- ===== HISCORES VIEW (with rank calculation) =====
IF EXISTS (SELECT 1 FROM sys.views WHERE name = N'hiscores' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP VIEW osrs.hiscores;
  PRINT 'VIEW osrs.hiscores dropped.';
END
GO

CREATE VIEW osrs.hiscores AS
SELECT 
  p.id,
  p.username,
  CAST(POWER(CASE WHEN p.attack_xp = 0 THEN 1 ELSE p.attack_xp / 4.0 END, 1.0/3.0) AS INT) AS attack_level,
  CAST(POWER(CASE WHEN p.strength_xp = 0 THEN 1 ELSE p.strength_xp / 4.0 END, 1.0/3.0) AS INT) AS strength_level,
  CAST(POWER(CASE WHEN p.defence_xp = 0 THEN 1 ELSE p.defence_xp / 4.0 END, 1.0/3.0) AS INT) AS defence_level,
  CAST(POWER(CASE WHEN p.magic_xp = 0 THEN 1 ELSE p.magic_xp / 4.0 END, 1.0/3.0) AS INT) AS magic_level,
  CAST(POWER(CASE WHEN p.prayer_xp = 0 THEN 1 ELSE p.prayer_xp / 4.0 END, 1.0/3.0) AS INT) AS prayer_level,
  CAST(POWER(CASE WHEN p.woodcutting_xp = 0 THEN 1 ELSE p.woodcutting_xp / 4.0 END, 1.0/3.0) AS INT) AS woodcutting_level,
  CAST(POWER(CASE WHEN p.fishing_xp = 0 THEN 1 ELSE p.fishing_xp / 4.0 END, 1.0/3.0) AS INT) AS fishing_level,
  CAST(POWER(CASE WHEN p.cooking_xp = 0 THEN 1 ELSE p.cooking_xp / 4.0 END, 1.0/3.0) AS INT) AS cooking_level,
  (CAST(POWER(CASE WHEN p.attack_xp = 0 THEN 1 ELSE p.attack_xp / 4.0 END, 1.0/3.0) AS INT) +
   CAST(POWER(CASE WHEN p.strength_xp = 0 THEN 1 ELSE p.strength_xp / 4.0 END, 1.0/3.0) AS INT) +
   CAST(POWER(CASE WHEN p.defence_xp = 0 THEN 1 ELSE p.defence_xp / 4.0 END, 1.0/3.0) AS INT) +
   CAST(POWER(CASE WHEN p.magic_xp = 0 THEN 1 ELSE p.magic_xp / 4.0 END, 1.0/3.0) AS INT) +
   CAST(POWER(CASE WHEN p.prayer_xp = 0 THEN 1 ELSE p.prayer_xp / 4.0 END, 1.0/3.0) AS INT) +
   CAST(POWER(CASE WHEN p.woodcutting_xp = 0 THEN 1 ELSE p.woodcutting_xp / 4.0 END, 1.0/3.0) AS INT) +
   CAST(POWER(CASE WHEN p.fishing_xp = 0 THEN 1 ELSE p.fishing_xp / 4.0 END, 1.0/3.0) AS INT) +
   CAST(POWER(CASE WHEN p.cooking_xp = 0 THEN 1 ELSE p.cooking_xp / 4.0 END, 1.0/3.0) AS INT)) AS overall_level,
  ROW_NUMBER() OVER (ORDER BY 
    (CAST(POWER(CASE WHEN p.attack_xp = 0 THEN 1 ELSE p.attack_xp / 4.0 END, 1.0/3.0) AS INT) +
     CAST(POWER(CASE WHEN p.strength_xp = 0 THEN 1 ELSE p.strength_xp / 4.0 END, 1.0/3.0) AS INT) +
     CAST(POWER(CASE WHEN p.defence_xp = 0 THEN 1 ELSE p.defence_xp / 4.0 END, 1.0/3.0) AS INT) +
     CAST(POWER(CASE WHEN p.magic_xp = 0 THEN 1 ELSE p.magic_xp / 4.0 END, 1.0/3.0) AS INT) +
     CAST(POWER(CASE WHEN p.prayer_xp = 0 THEN 1 ELSE p.prayer_xp / 4.0 END, 1.0/3.0) AS INT) +
     CAST(POWER(CASE WHEN p.woodcutting_xp = 0 THEN 1 ELSE p.woodcutting_xp / 4.0 END, 1.0/3.0) AS INT) +
     CAST(POWER(CASE WHEN p.fishing_xp = 0 THEN 1 ELSE p.fishing_xp / 4.0 END, 1.0/3.0) AS INT) +
     CAST(POWER(CASE WHEN p.cooking_xp = 0 THEN 1 ELSE p.cooking_xp / 4.0 END, 1.0/3.0) AS INT)
    ) DESC
  ) AS overall_rank,
  p.created_at
FROM osrs.players p;
GO

PRINT 'VIEW osrs.hiscores created.';

-- ===== ACTIVE PLAYERS VIEW =====
IF EXISTS (SELECT 1 FROM sys.views WHERE name = N'active_players' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP VIEW osrs.active_players;
  PRINT 'VIEW osrs.active_players dropped.';
END
GO

CREATE VIEW osrs.active_players AS
SELECT 
  p.id,
  p.username,
  p.x,
  p.y,
  ps.session_start,
  ps.last_heartbeat,
  DATEDIFF(SECOND, ps.last_heartbeat, GETDATE()) AS seconds_since_heartbeat,
  ps.status
FROM osrs.players p
INNER JOIN osrs.player_sessions ps ON p.id = ps.player_id
WHERE ps.session_end IS NULL AND p.is_dead = 0;
GO

PRINT 'VIEW osrs.active_players created.';

-- ============================================================================
-- PART 17: STORED PROCEDURES
-- ============================================================================

-- ===== PROCEDURE: Add Experience (Atomic, with validation) =====
IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'sp_add_experience' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP PROCEDURE osrs.sp_add_experience;
  PRINT 'PROCEDURE osrs.sp_add_experience dropped.';
END
GO

CREATE PROCEDURE osrs.sp_add_experience
  @player_id INT,
  @skill_name VARCHAR(50),
  @xp_delta BIGINT
AS
BEGIN
  SET NOCOUNT ON;
  
  DECLARE @max_xp BIGINT = 200000000;  -- Level 99
  DECLARE @max_xp_per_tick BIGINT = 10000;
  
  -- Validate inputs
  IF @player_id IS NULL OR @player_id <= 0
  BEGIN
    RAISERROR('Invalid player_id', 16, 1);
    RETURN;
  END
  
  IF @xp_delta < 0 OR @xp_delta > @max_xp_per_tick
  BEGIN
    RAISERROR('XP delta out of bounds: %I64d (max %I64d)', 16, 1, @xp_delta, @max_xp_per_tick);
    RETURN;
  END
  
  -- Atomic update using addition (prevents overwrite on concurrent updates)
  IF @skill_name = 'ATTACK'
  BEGIN
    UPDATE osrs.players 
    SET attack_xp = CASE 
      WHEN attack_xp + @xp_delta > @max_xp THEN @max_xp
      ELSE attack_xp + @xp_delta
    END
    WHERE id = @player_id;
  END
  ELSE IF @skill_name = 'STRENGTH'
  BEGIN
    UPDATE osrs.players 
    SET strength_xp = CASE 
      WHEN strength_xp + @xp_delta > @max_xp THEN @max_xp
      ELSE strength_xp + @xp_delta
    END
    WHERE id = @player_id;
  END
  ELSE IF @skill_name = 'DEFENCE'
  BEGIN
    UPDATE osrs.players 
    SET defence_xp = CASE 
      WHEN defence_xp + @xp_delta > @max_xp THEN @max_xp
      ELSE defence_xp + @xp_delta
    END
    WHERE id = @player_id;
  END
  ELSE IF @skill_name = 'MAGIC'
  BEGIN
    UPDATE osrs.players 
    SET magic_xp = CASE 
      WHEN magic_xp + @xp_delta > @max_xp THEN @max_xp
      ELSE magic_xp + @xp_delta
    END
    WHERE id = @player_id;
  END
  ELSE IF @skill_name = 'PRAYER'
  BEGIN
    UPDATE osrs.players 
    SET prayer_xp = CASE 
      WHEN prayer_xp + @xp_delta > @max_xp THEN @max_xp
      ELSE prayer_xp + @xp_delta
    END
    WHERE id = @player_id;
  END
  ELSE IF @skill_name = 'WOODCUTTING'
  BEGIN
    UPDATE osrs.players 
    SET woodcutting_xp = CASE 
      WHEN woodcutting_xp + @xp_delta > @max_xp THEN @max_xp
      ELSE woodcutting_xp + @xp_delta
    END
    WHERE id = @player_id;
  END
  ELSE IF @skill_name = 'FISHING'
  BEGIN
    UPDATE osrs.players 
    SET fishing_xp = CASE 
      WHEN fishing_xp + @xp_delta > @max_xp THEN @max_xp
      ELSE fishing_xp + @xp_delta
    END
    WHERE id = @player_id;
  END
  ELSE IF @skill_name = 'COOKING'
  BEGIN
    UPDATE osrs.players 
    SET cooking_xp = CASE 
      WHEN cooking_xp + @xp_delta > @max_xp THEN @max_xp
      ELSE cooking_xp + @xp_delta
    END
    WHERE id = @player_id;
  END
  ELSE
  BEGIN
    RAISERROR('Invalid skill name: %s', 16, 1, @skill_name);
    RETURN;
  END
  
  PRINT 'XP added: ' + @skill_name + ' +' + CAST(@xp_delta AS VARCHAR(20)) + ' for player ' + CAST(@player_id AS VARCHAR(10));
END
GO

PRINT 'PROCEDURE osrs.sp_add_experience created.';

-- ===== PROCEDURE: Cleanup Idle Sessions (for scheduled job) =====
IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'sp_cleanup_idle_sessions' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP PROCEDURE osrs.sp_cleanup_idle_sessions;
  PRINT 'PROCEDURE osrs.sp_cleanup_idle_sessions dropped.';
END
GO

CREATE PROCEDURE osrs.sp_cleanup_idle_sessions
AS
BEGIN
  SET NOCOUNT ON;
  
  DECLARE @idle_seconds INT = 1800;  -- 30 minutes
  DECLARE @old_packet_seconds INT = 300;  -- 5 minutes
  
  -- Mark sessions as offline if no heartbeat in 30 minutes
  UPDATE osrs.player_sessions
  SET status = 1,
      session_end = GETDATE()
  WHERE status = 0 
    AND DATEDIFF(SECOND, last_heartbeat, GETDATE()) > @idle_seconds;
  
  -- Delete old packet records (deduplication cleanup after 5 minutes)
  DELETE FROM osrs.processed_packets
  WHERE DATEDIFF(SECOND, processed_at, GETDATE()) > @old_packet_seconds;
  
  -- Delete despawned loot
  DELETE FROM osrs.ground_loot
  WHERE despawn_at IS NOT NULL 
    AND GETDATE() > despawn_at;
  
  PRINT 'Cleanup job completed: sessions marked idle, old packets deleted, loot despawned.';
END
GO

PRINT 'PROCEDURE osrs.sp_cleanup_idle_sessions created.';

-- ===== PROCEDURE: Create New Player =====
IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'sp_create_player' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP PROCEDURE osrs.sp_create_player;
  PRINT 'PROCEDURE osrs.sp_create_player dropped.';
END
GO

CREATE PROCEDURE osrs.sp_create_player
  @username VARCHAR(12),
  @password_hash VARCHAR(255),
  @email VARCHAR(255) = NULL,
  @new_player_id INT OUTPUT
AS
BEGIN
  SET NOCOUNT ON;
  
  -- Validate username
  IF LEN(@username) < 3 OR LEN(@username) > 12
  BEGIN
    RAISERROR('Username must be 3-12 characters', 16, 1);
    RETURN;
  END
  
  IF EXISTS (SELECT 1 FROM osrs.players WHERE username = @username)
  BEGIN
    RAISERROR('Username already taken', 16, 1);
    RETURN;
  END
  
  -- Insert new player
  BEGIN TRY
    INSERT INTO osrs.players (username, password_hash, email, created_at, x, y)
    VALUES (@username, @password_hash, @email, GETDATE(), 3222, 3218);
    
    SET @new_player_id = SCOPE_IDENTITY();
    
    PRINT 'Player created: ' + @username + ' (ID: ' + CAST(@new_player_id AS VARCHAR(10)) + ')';
  END TRY
  BEGIN CATCH
    RAISERROR('Error creating player', 16, 1);
    RETURN;
  END CATCH
END
GO

PRINT 'PROCEDURE osrs.sp_create_player created.';

-- ===== PROCEDURE: Complete Quest Objective (Atomic bitmask update) =====
IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'sp_complete_quest_objective' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP PROCEDURE osrs.sp_complete_quest_objective;
  PRINT 'PROCEDURE osrs.sp_complete_quest_objective dropped.';
END
GO

CREATE PROCEDURE osrs.sp_complete_quest_objective
  @player_id INT,
  @quest_id INT,
  @objective_index INT
AS
BEGIN
  SET NOCOUNT ON;
  
  DECLARE @objective_bitmask INT = (1 << @objective_index);  -- Shift left: 1, 2, 4, 8, 16...
  
  -- Atomic bitwise OR to prevent race conditions
  UPDATE osrs.player_quests
  SET completed_objectives = completed_objectives | @objective_bitmask
  WHERE player_id = @player_id AND quest_id = @quest_id;
  
  PRINT 'Quest ' + CAST(@quest_id AS VARCHAR(10)) + ' objective ' + CAST(@objective_index AS VARCHAR(2)) + ' completed for player ' + CAST(@player_id AS VARCHAR(10));
END
GO

PRINT 'PROCEDURE osrs.sp_complete_quest_objective created.';

-- ============================================================================
-- PART 18: VERIFY SCHEMA INTEGRITY
-- ============================================================================

PRINT '';
PRINT '========== SCHEMA VERIFICATION ==========';
PRINT '';

-- Count all tables
SELECT 'Table Count' AS Check_Type, COUNT(*) AS Count
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'osrs';

-- Count all indices
SELECT 'Index Count' AS Check_Type, COUNT(*) AS Count
FROM sys.indexes
WHERE object_id IN (SELECT object_id FROM sys.objects WHERE schema_id = SCHEMA_ID('osrs') AND type = 'U');

-- Count all views
SELECT 'View Count' AS Check_Type, COUNT(*) AS Count
FROM INFORMATION_SCHEMA.VIEWS
WHERE TABLE_SCHEMA = 'osrs';

-- Count all stored procedures
SELECT 'Stored Procedure Count' AS Check_Type, COUNT(*) AS Count
FROM INFORMATION_SCHEMA.ROUTINES
WHERE ROUTINE_SCHEMA = 'osrs';

PRINT '';
PRINT '========== SCHEMA CREATION COMPLETE ==========';
PRINT 'Database: osrsmmorp';
PRINT 'Schema: osrs';
PRINT 'Status: READY FOR DEVELOPMENT';
PRINT '';
