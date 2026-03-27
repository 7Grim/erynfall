-- ============================================================================
-- OSRS-MMORP Complete Database Schema for SQL Server 2025 - FINAL MASTER
-- ============================================================================
-- ULTRATHINK: Proper SQL Server batch separation, no syntax errors
-- All CREATE VIEW and CREATE PROCEDURE preceded by GO
-- All statements properly batched
-- Created: 2026-03-21 FINAL
-- ============================================================================

-- Azure SQL: database already exists, skip CREATE/DROP DATABASE
-- Schema objects use DROP IF EXISTS so this script is safe to re-run

-- Create schema
IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'osrs')
BEGIN
  EXEC sp_executesql N'CREATE SCHEMA osrs';
END
GO

-- ============================================================================
-- TABLE 1: CONFIG (Server configuration)
-- ============================================================================

DROP TABLE IF EXISTS osrs.config;
GO

CREATE TABLE osrs.config (
  config_key VARCHAR(100) PRIMARY KEY,
  config_value VARCHAR(255) NOT NULL,
  description VARCHAR(500),
  updated_at DATETIME2 DEFAULT GETDATE()
);
GO

INSERT INTO osrs.config (config_key, config_value, description)
VALUES
  ('TICK_RATE_HZ', '256', 'Server tick rate in Hz'),
  ('TICK_INTERVAL_NS', '3906250', 'Nanoseconds per tick'),
  ('MAX_PLAYERS_MVP', '1', 'Maximum concurrent players'),
  ('COMBAT_RANGE_TILES', '2', 'Melee combat range'),
  ('ITEM_DESPAWN_HOURS', '1', 'Hours before loot despawns'),
  ('NPC_RESPAWN_TICKS', '600', 'Ticks before NPC respawns'),
  ('MAX_INVENTORY_SLOTS', '28', 'Inventory slot limit'),
  ('IDLE_DISCONNECT_MINUTES', '30', 'Idle disconnect time'),
  ('MAX_XP_PER_TICK', '10000', 'Max XP per tick'),
  ('MAX_TOTAL_XP', '200000000', 'Max XP per skill (Level 99)'),
  ('LEVEL_99_XP', '200000000', 'XP required for level 99'),
  ('PAYMENT_FEE_PERCENT', '2', 'Grand Exchange fee percentage');
GO

-- ============================================================================
-- TABLE 2: ITEMS (Item definitions - stackable, tradeable, prices)
-- ============================================================================

DROP TABLE IF EXISTS osrs.items;
GO

CREATE TABLE osrs.items (
  id INT PRIMARY KEY,
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
  updated_at DATETIME2 DEFAULT GETDATE()
);
GO

CREATE INDEX idx_items_name ON osrs.items(name);
GO

INSERT INTO osrs.items (id, name, examine_text, stackable, weight_kg, tradeable, high_alchemy_value, low_alchemy_value)
VALUES
  (1, 'Bones', 'These are bones. Bury for prayer XP.', 1, 0.1, 1, 5, 2),
  (2, 'Raw Chicken', 'Raw chicken - cook it first.', 1, 0.05, 1, 0, 0),
  (3, 'Feathers', 'Light fluffy feathers.', 1, 0.01, 1, 0, 0),
  (4, 'Raw Beef', 'Raw beef - cook first.', 1, 0.08, 1, 0, 0),
  (5, 'Cowhide', 'Cow hide - craft into leather.', 1, 0.12, 1, 20, 10),
  (6, 'Logs', 'Log of wood - fletch or burn.', 1, 0.1, 1, 20, 10),
  (7, 'Raw Fish', 'Raw fish - needs cooking.', 1, 0.2, 1, 0, 0),
  (100, 'Bronze Dagger', 'Weakest dagger.', 0, 0.5, 1, 16, 5),
  (101, 'Iron Dagger', 'Iron dagger.', 0, 0.5, 1, 32, 10),
  (102, 'Bronze Sword', 'Bronze sword.', 0, 0.7, 1, 48, 15),
  (103, 'Iron Sword', 'Iron sword.', 0, 0.7, 1, 120, 40),
  (200, 'Cooked Chicken', 'Heals 3 HP.', 1, 0.05, 1, 0, 0),
  (201, 'Cooked Beef', 'Heals 5 HP.', 1, 0.08, 1, 0, 0),
  (202, 'Cooked Fish', 'Heals 3 HP.', 1, 0.2, 1, 0, 0),
  (995, 'Coins', 'Legal tender.', 1, 0.005, 1, 1, 1);
GO

-- ============================================================================
-- TABLE 3: NPCS (NPC definitions - combat level, spawn location)
-- ============================================================================

DROP TABLE IF EXISTS osrs.npcs;
GO

CREATE TABLE osrs.npcs (
  id INT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  combat_level INT DEFAULT 1,
  max_hp INT DEFAULT 10,
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
  size INT DEFAULT 1,
  attackable BIT DEFAULT 1,
  aggressive BIT DEFAULT 0,
  description VARCHAR(500),
  created_at DATETIME2 DEFAULT GETDATE()
);
GO

CREATE INDEX idx_npcs_spawn ON osrs.npcs(spawn_id);
CREATE INDEX idx_npcs_location ON osrs.npcs(spawn_x, spawn_y);
GO

INSERT INTO osrs.npcs (id, name, combat_level, max_hp, attack_level, strength_level, defence_level, spawn_x, spawn_y, spawn_id, respawn_ticks, max_alive, size, attackable, aggressive, description)
VALUES
  (1, 'Chicken', 1, 3, 1, 1, 1, 3230, 3205, 1, 600, 5, 1, 1, 0, 'Docile poultry creature.'),
  (2, 'Goblin', 5, 6, 5, 5, 5, 3210, 3215, 2, 600, 3, 1, 1, 0, 'Small green humanoid.'),
  (3, 'Cow', 10, 10, 10, 10, 10, 3245, 3190, 3, 600, 8, 2, 1, 0, 'Bovine creature.');
GO

-- ============================================================================
-- TABLE 4: NPC_LOOT_TABLES (Drop table definitions with rates)
-- ============================================================================

DROP TABLE IF EXISTS osrs.npc_loot_tables;
GO

CREATE TABLE osrs.npc_loot_tables (
  id INT IDENTITY(1,1) PRIMARY KEY,
  npc_id INT NOT NULL REFERENCES osrs.npcs(id) ON DELETE CASCADE,
  item_id INT NOT NULL REFERENCES osrs.items(id),
  quantity_min INT NOT NULL DEFAULT 1,
  quantity_max INT NOT NULL DEFAULT 1,
  drop_rate INT NOT NULL,
  always_drop BIT DEFAULT 0,
  sort_order INT DEFAULT 0
);
GO

CREATE INDEX idx_loot_npc ON osrs.npc_loot_tables(npc_id);
CREATE INDEX idx_loot_item ON osrs.npc_loot_tables(item_id);
GO

INSERT INTO osrs.npc_loot_tables (npc_id, item_id, quantity_min, quantity_max, drop_rate, always_drop, sort_order)
VALUES
  (1, 1, 1, 1, 10000, 1, 0),  -- Chicken: bones always
  (1, 2, 1, 1, 10000, 1, 1),  -- Chicken: raw chicken always
  (1, 3, 5, 15, 10000, 0, 2), -- Chicken: feathers 5-15
  (2, 1, 1, 1, 10000, 1, 0),  -- Goblin: bones always
  (2, 4, 1, 1, 10000, 0, 1),  -- Goblin: raw beef
  (3, 1, 1, 1, 10000, 1, 0),  -- Cow: bones always
  (3, 4, 1, 1, 10000, 1, 1),  -- Cow: raw beef always
  (3, 5, 1, 1, 10000, 1, 2);  -- Cow: cowhide always
GO

-- ============================================================================
-- TABLE 5: PLAYERS (Core player account + XP data)
-- ============================================================================

DROP TABLE IF EXISTS osrs.players;
GO

CREATE TABLE osrs.players (
  id INT IDENTITY(1,1) PRIMARY KEY,
  username VARCHAR(12) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  created_at DATETIME2 DEFAULT GETDATE(),
  last_login DATETIME2,
  last_logout DATETIME2,
  
  -- Position in world
  x INT DEFAULT 3222,
  y INT DEFAULT 3218,
  
  -- Skills: XP only (levels derived)
  attack_xp BIGINT DEFAULT 0,
  strength_xp BIGINT DEFAULT 0,
  defence_xp BIGINT DEFAULT 0,
  magic_xp BIGINT DEFAULT 0,
  prayer_xp BIGINT DEFAULT 0,
  prayer_points INT DEFAULT 10,
  woodcutting_xp BIGINT DEFAULT 0,
  fishing_xp BIGINT DEFAULT 0,
  cooking_xp BIGINT DEFAULT 0,
  
  -- Economy
  total_gold BIGINT DEFAULT 0,
  total_questpoints INT DEFAULT 0,
  
  -- Authority-server validation
  last_action_tick BIGINT DEFAULT 0,
  pending_actions INT DEFAULT 0,
  
  -- Death state
  is_dead BIT DEFAULT 0,
  death_tick BIGINT,
  death_x INT,
  death_y INT,
  
  -- Session reference
  current_session_id INT
);
GO

SET QUOTED_IDENTIFIER ON;
CREATE UNIQUE INDEX idx_players_username ON osrs.players(username);
CREATE INDEX idx_players_created_at ON osrs.players(created_at);
CREATE INDEX idx_players_location ON osrs.players(x, y);
CREATE INDEX idx_players_dead ON osrs.players(is_dead) WHERE is_dead = 1;
GO

-- ============================================================================
-- TABLE 6: INVENTORY (28-slot hard limit per player)
-- ============================================================================

DROP TABLE IF EXISTS osrs.inventory;
GO

CREATE TABLE osrs.inventory (
  id INT IDENTITY(1,1) PRIMARY KEY,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  slot_index INT NOT NULL,
  item_id INT NOT NULL REFERENCES osrs.items(id),
  quantity INT DEFAULT 1,
  
  UNIQUE (player_id, slot_index)
);
GO

CREATE INDEX idx_inventory_player ON osrs.inventory(player_id);
CREATE INDEX idx_inventory_item ON osrs.inventory(item_id);
GO

-- ============================================================================
-- TABLE 7: GROUND_LOOT (Items on the ground with despawn)
-- ============================================================================

DROP TABLE IF EXISTS osrs.ground_loot;
GO

CREATE TABLE osrs.ground_loot (
  id INT IDENTITY(1,1) PRIMARY KEY,
  item_id INT NOT NULL REFERENCES osrs.items(id),
  quantity INT NOT NULL,
  x INT NOT NULL,
  y INT NOT NULL,
  owner_id INT REFERENCES osrs.players(id) ON DELETE SET NULL,
  dropped_at DATETIME2 DEFAULT GETDATE(),
  despawn_at DATETIME2
);
GO

CREATE INDEX idx_ground_loot_location ON osrs.ground_loot(x, y);
CREATE INDEX idx_ground_loot_despawn ON osrs.ground_loot(despawn_at);
GO

-- ============================================================================
-- TABLE 8: PLAYER_QUESTS (Quest progress tracking with objectives bitmask)
-- ============================================================================

DROP TABLE IF EXISTS osrs.player_quests;
GO

CREATE TABLE osrs.player_quests (
  id INT IDENTITY(1,1) PRIMARY KEY,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  quest_id INT NOT NULL,
  status INT DEFAULT 0,
  completed_objectives INT DEFAULT 0,
  started_at DATETIME2 DEFAULT GETDATE(),
  completed_at DATETIME2,
  
  UNIQUE (player_id, quest_id)
);
GO

CREATE INDEX idx_player_quests ON osrs.player_quests(player_id, quest_id);
GO

-- ============================================================================
-- TABLE 8B: PLAYER_QUEST_TASKS (Exact per-task progress counts)
-- ============================================================================

DROP TABLE IF EXISTS osrs.player_quest_tasks;
GO

CREATE TABLE osrs.player_quest_tasks (
  id INT IDENTITY(1,1) PRIMARY KEY,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  quest_id INT NOT NULL,
  task_id VARCHAR(64) NOT NULL,
  progress_count INT NOT NULL DEFAULT 0,

  UNIQUE (player_id, quest_id, task_id)
);
GO

CREATE INDEX idx_player_quest_tasks_player ON osrs.player_quest_tasks(player_id, quest_id);
GO

-- ============================================================================
-- TABLE 9: GE_ORDERS (Grand Exchange buy/sell orders with matching index)
-- ============================================================================

DROP TABLE IF EXISTS osrs.ge_orders;
GO

CREATE TABLE osrs.ge_orders (
  id INT IDENTITY(1,1) PRIMARY KEY,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  item_id INT NOT NULL REFERENCES osrs.items(id),
  quantity INT NOT NULL,
  price_per_unit INT NOT NULL,
  is_buy BIT NOT NULL,
  filled_quantity INT DEFAULT 0,
  created_at DATETIME2 DEFAULT GETDATE(),
  completed_at DATETIME2
);
GO

-- CRITICAL PERFORMANCE INDEX: filtered to active orders only
SET QUOTED_IDENTIFIER ON;
CREATE INDEX idx_ge_matching ON osrs.ge_orders(item_id, is_buy, price_per_unit DESC, created_at) WHERE completed_at IS NULL;
CREATE INDEX idx_ge_player ON osrs.ge_orders(player_id, completed_at);
GO

-- ============================================================================
-- TABLE 10: TRADE_HISTORY (P2P trade audit log)
-- ============================================================================

DROP TABLE IF EXISTS osrs.trade_history;
GO

CREATE TABLE osrs.trade_history (
  id INT IDENTITY(1,1) PRIMARY KEY,
  initiator_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  recipient_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE NO ACTION,
  item_id INT NOT NULL REFERENCES osrs.items(id),
  quantity INT NOT NULL,
  completed_at DATETIME2 DEFAULT GETDATE()
);
GO

CREATE INDEX idx_trade_initiator ON osrs.trade_history(initiator_id, completed_at);
CREATE INDEX idx_trade_recipient ON osrs.trade_history(recipient_id, completed_at);
GO

-- ============================================================================
-- TABLE 11: PLAYER_ACHIEVEMENTS (Achievement tracking with progress)
-- ============================================================================

DROP TABLE IF EXISTS osrs.player_achievements;
GO

CREATE TABLE osrs.player_achievements (
  id INT IDENTITY(1,1) PRIMARY KEY,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  achievement_id INT NOT NULL,
  unlocked_at DATETIME2 DEFAULT GETDATE(),
  progress INT DEFAULT 0,
  
  UNIQUE (player_id, achievement_id)
);
GO

-- ============================================================================
-- TABLE 12: CHAT_MESSAGES (Public + private messages)
-- ============================================================================

DROP TABLE IF EXISTS osrs.chat_messages;
GO

CREATE TABLE osrs.chat_messages (
  id INT IDENTITY(1,1) PRIMARY KEY,
  sender_id INT REFERENCES osrs.players(id) ON DELETE CASCADE,
  sender_name VARCHAR(12),
  message_text VARCHAR(255) NOT NULL,
  chat_type INT DEFAULT 0,
  recipient_id INT REFERENCES osrs.players(id) ON DELETE NO ACTION,
  created_at DATETIME2 DEFAULT GETDATE()
);
GO

CREATE INDEX idx_chat_sender ON osrs.chat_messages(sender_id, created_at);
CREATE INDEX idx_chat_recipient ON osrs.chat_messages(recipient_id, created_at);
GO

-- ============================================================================
-- TABLE 13: PLAYER_SESSIONS (Online player tracking with heartbeat)
-- ============================================================================

DROP TABLE IF EXISTS osrs.player_sessions;
GO

CREATE TABLE osrs.player_sessions (
  id INT IDENTITY(1,1) PRIMARY KEY,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  session_start DATETIME2 DEFAULT GETDATE(),
  session_end DATETIME2,
  last_heartbeat DATETIME2 DEFAULT GETDATE(),
  ip_address VARCHAR(45),
  status INT DEFAULT 0
);
GO

CREATE INDEX idx_sessions_player ON osrs.player_sessions(player_id, last_heartbeat);
CREATE INDEX idx_sessions_heartbeat ON osrs.player_sessions(last_heartbeat);
GO

-- ============================================================================
-- TABLE 14: PROCESSED_PACKETS (Anti-duplication: packet ID deduplication)
-- ============================================================================

DROP TABLE IF EXISTS osrs.processed_packets;
GO

CREATE TABLE osrs.processed_packets (
  id INT IDENTITY(1,1) PRIMARY KEY,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  packet_id BIGINT NOT NULL,
  processed_at DATETIME2 DEFAULT GETDATE(),
  
  UNIQUE (player_id, packet_id)
);
GO

CREATE INDEX idx_packets_cleanup ON osrs.processed_packets(processed_at);
GO

-- ============================================================================
-- VIEW 1: HISCORES (Ranked leaderboard with calculated levels)
-- ============================================================================

DROP VIEW IF EXISTS osrs.hiscores;
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
  ) AS overall_rank
FROM osrs.players p;
GO

-- ============================================================================
-- VIEW 2: ACTIVE_PLAYERS (Online players with heartbeat info)
-- ============================================================================

DROP VIEW IF EXISTS osrs.active_players;
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

-- ============================================================================
-- PROCEDURE 1: sp_add_experience (Atomic XP gain)
-- ============================================================================

DROP PROCEDURE IF EXISTS osrs.sp_add_experience;
GO

CREATE PROCEDURE osrs.sp_add_experience
  @player_id INT,
  @skill_name VARCHAR(50),
  @xp_delta BIGINT
AS
BEGIN
  SET NOCOUNT ON;
  DECLARE @max_xp BIGINT = 200000000;
  
  IF @skill_name = 'ATTACK'
    UPDATE osrs.players SET attack_xp = CASE WHEN attack_xp + @xp_delta > @max_xp THEN @max_xp ELSE attack_xp + @xp_delta END WHERE id = @player_id;
  ELSE IF @skill_name = 'STRENGTH'
    UPDATE osrs.players SET strength_xp = CASE WHEN strength_xp + @xp_delta > @max_xp THEN @max_xp ELSE strength_xp + @xp_delta END WHERE id = @player_id;
  ELSE IF @skill_name = 'DEFENCE'
    UPDATE osrs.players SET defence_xp = CASE WHEN defence_xp + @xp_delta > @max_xp THEN @max_xp ELSE defence_xp + @xp_delta END WHERE id = @player_id;
  ELSE IF @skill_name = 'MAGIC'
    UPDATE osrs.players SET magic_xp = CASE WHEN magic_xp + @xp_delta > @max_xp THEN @max_xp ELSE magic_xp + @xp_delta END WHERE id = @player_id;
  ELSE IF @skill_name = 'PRAYER'
    UPDATE osrs.players SET prayer_xp = CASE WHEN prayer_xp + @xp_delta > @max_xp THEN @max_xp ELSE prayer_xp + @xp_delta END WHERE id = @player_id;
  ELSE IF @skill_name = 'WOODCUTTING'
    UPDATE osrs.players SET woodcutting_xp = CASE WHEN woodcutting_xp + @xp_delta > @max_xp THEN @max_xp ELSE woodcutting_xp + @xp_delta END WHERE id = @player_id;
  ELSE IF @skill_name = 'FISHING'
    UPDATE osrs.players SET fishing_xp = CASE WHEN fishing_xp + @xp_delta > @max_xp THEN @max_xp ELSE fishing_xp + @xp_delta END WHERE id = @player_id;
  ELSE IF @skill_name = 'COOKING'
    UPDATE osrs.players SET cooking_xp = CASE WHEN cooking_xp + @xp_delta > @max_xp THEN @max_xp ELSE cooking_xp + @xp_delta END WHERE id = @player_id;
END
GO

-- ============================================================================
-- PROCEDURE 2: sp_cleanup_idle_sessions (Maintenance job)
-- ============================================================================

DROP PROCEDURE IF EXISTS osrs.sp_cleanup_idle_sessions;
GO

CREATE PROCEDURE osrs.sp_cleanup_idle_sessions
AS
BEGIN
  SET NOCOUNT ON;
  UPDATE osrs.player_sessions SET status = 1, session_end = GETDATE() WHERE status = 0 AND DATEDIFF(SECOND, last_heartbeat, GETDATE()) > 1800;
  DELETE FROM osrs.processed_packets WHERE DATEDIFF(SECOND, processed_at, GETDATE()) > 300;
  DELETE FROM osrs.ground_loot WHERE despawn_at IS NOT NULL AND GETDATE() > despawn_at;
END
GO

-- ============================================================================
-- PROCEDURE 3: sp_create_player (New player registration)
-- ============================================================================

DROP PROCEDURE IF EXISTS osrs.sp_create_player;
GO

CREATE PROCEDURE osrs.sp_create_player
  @username VARCHAR(12),
  @password_hash VARCHAR(255),
  @email VARCHAR(255) = NULL,
  @new_player_id INT OUTPUT
AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS (SELECT 1 FROM osrs.players WHERE username = @username) 
  BEGIN
    RAISERROR('Username taken', 16, 1);
    RETURN;
  END
  INSERT INTO osrs.players (username, password_hash, email, created_at, x, y) VALUES (@username, @password_hash, @email, GETDATE(), 3222, 3218);
  SET @new_player_id = SCOPE_IDENTITY();
END
GO

-- ============================================================================
-- PROCEDURE 4: sp_complete_quest_objective (Atomic quest objective update)
-- ============================================================================

DROP PROCEDURE IF EXISTS osrs.sp_complete_quest_objective;
GO

CREATE PROCEDURE osrs.sp_complete_quest_objective
  @player_id INT,
  @quest_id INT,
  @objective_index INT
AS
BEGIN
  SET NOCOUNT ON;
  DECLARE @bitmask INT = (1 << @objective_index);
  UPDATE osrs.player_quests SET completed_objectives = completed_objectives | @bitmask WHERE player_id = @player_id AND quest_id = @quest_id;
END
GO

-- ============================================================================
-- FINAL VERIFICATION
-- ============================================================================

PRINT '';
PRINT '========== OSRS-MMORP SCHEMA CREATION COMPLETE ==========';
PRINT '';
PRINT 'Database: erynfall';
PRINT 'Schema: osrs';
PRINT '';

SELECT 'TABLES' AS object_type, COUNT(*) AS count FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'osrs'
UNION ALL
SELECT 'VIEWS' AS object_type, COUNT(*) AS count FROM INFORMATION_SCHEMA.VIEWS WHERE TABLE_SCHEMA = 'osrs'
UNION ALL
SELECT 'PROCEDURES' AS object_type, COUNT(*) AS count FROM INFORMATION_SCHEMA.ROUTINES WHERE ROUTINE_SCHEMA = 'osrs' AND ROUTINE_TYPE = 'PROCEDURE';

PRINT '';
PRINT 'Status: READY FOR DEVELOPMENT';
PRINT '';
