-- ============================================================================
-- OSRS-MMORP Complete Database Schema for SQL Server 2025 (CLEAN VERSION)
-- ============================================================================
-- FIXES: Removed ALL subqueries from CHECK constraints (not allowed in SQL Server)
-- Created: 2026-03-21 (CLEAN VERSION)
-- Usage: Run entire script in SSMS Query Editor (paste all at once)
-- ============================================================================

USE [master];
GO

IF EXISTS (SELECT 1 FROM sys.databases WHERE name = N'osrsmmorp')
BEGIN
  ALTER DATABASE [osrsmmorp] SET OFFLINE WITH ROLLBACK IMMEDIATE;
  WAITFOR DELAY '00:00:02';
  DROP DATABASE [osrsmmorp];
  WAITFOR DELAY '00:00:02';
END
GO

CREATE DATABASE [osrsmmorp];
GO

USE [osrsmmorp];
GO

ALTER DATABASE [osrsmmorp] SET ALLOW_SNAPSHOT_ISOLATION ON;
GO

ALTER DATABASE [osrsmmorp] SET READ_COMMITTED_SNAPSHOT ON;
GO

-- ============================================================================
-- SCHEMA CREATION
-- ============================================================================

IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'osrs')
BEGIN
  EXEC sp_executesql N'CREATE SCHEMA osrs';
  PRINT 'SCHEMA [osrs] created.';
END
GO

-- ============================================================================
-- CONFIG TABLE
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'config' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.config;
END
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
  ('MAX_TOTAL_XP', '200000000', 'Max XP per skill'),
  ('LEVEL_99_XP', '200000000', 'XP for level 99'),
  ('PAYMENT_FEE_PERCENT', '2', 'GE fee percent');
GO

PRINT 'TABLE osrs.config created.';

-- ============================================================================
-- ITEMS TABLE
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'items' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.items;
END
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

PRINT 'TABLE osrs.items created (15 items).';

-- ============================================================================
-- NPCS TABLE
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'npcs' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.npcs;
END
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
GO

INSERT INTO osrs.npcs (id, name, combat_level, max_hp, attack_level, strength_level, defence_level, spawn_x, spawn_y, spawn_id, respawn_ticks, max_alive, size, attackable, aggressive, description)
VALUES
  (1, 'Chicken', 1, 3, 1, 1, 1, 3230, 3205, 1, 600, 5, 1, 1, 0, 'Docile poultry.'),
  (2, 'Goblin', 5, 6, 5, 5, 5, 3210, 3215, 2, 600, 3, 1, 1, 0, 'Small green humanoid.'),
  (3, 'Cow', 10, 10, 10, 10, 10, 3245, 3190, 3, 600, 8, 2, 1, 0, 'Bovine creature.');
GO

PRINT 'TABLE osrs.npcs created (3 NPCs).';

-- ============================================================================
-- NPC LOOT TABLES
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'npc_loot_tables' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.npc_loot_tables;
END
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

INSERT INTO osrs.npc_loot_tables (npc_id, item_id, quantity_min, quantity_max, drop_rate, always_drop, sort_order)
VALUES
  (1, 1, 1, 1, 10000, 1, 0),
  (1, 2, 1, 1, 10000, 1, 1),
  (1, 3, 5, 15, 10000, 0, 2),
  (2, 1, 1, 1, 10000, 1, 0),
  (2, 4, 1, 1, 10000, 0, 1),
  (3, 1, 1, 1, 10000, 1, 0),
  (3, 4, 1, 1, 10000, 1, 1),
  (3, 5, 1, 1, 10000, 1, 2);
GO

PRINT 'TABLE osrs.npc_loot_tables created.';

-- ============================================================================
-- PLAYERS TABLE
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'players' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.players;
END
GO

CREATE TABLE osrs.players (
  id INT IDENTITY(1,1) PRIMARY KEY,
  username VARCHAR(12) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  created_at DATETIME2 DEFAULT GETDATE(),
  last_login DATETIME2,
  last_logout DATETIME2,
  
  x INT DEFAULT 3222,
  y INT DEFAULT 3218,
  
  attack_xp BIGINT DEFAULT 0,
  strength_xp BIGINT DEFAULT 0,
  defence_xp BIGINT DEFAULT 0,
  magic_xp BIGINT DEFAULT 0,
  prayer_xp BIGINT DEFAULT 0,
  prayer_points INT DEFAULT 10,
  woodcutting_xp BIGINT DEFAULT 0,
  fishing_xp BIGINT DEFAULT 0,
  cooking_xp BIGINT DEFAULT 0,
  
  total_gold BIGINT DEFAULT 0,
  total_questpoints INT DEFAULT 0,
  
  last_action_tick BIGINT DEFAULT 0,
  pending_actions INT DEFAULT 0,
  
  is_dead BIT DEFAULT 0,
  death_tick BIGINT,
  death_x INT,
  death_y INT,
  
  current_session_id INT
);
GO

CREATE UNIQUE INDEX idx_players_username ON osrs.players(username);
CREATE INDEX idx_players_created_at ON osrs.players(created_at);
GO

PRINT 'TABLE osrs.players created.';

-- ============================================================================
-- INVENTORY TABLE
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'inventory' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.inventory;
END
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
GO

PRINT 'TABLE osrs.inventory created.';

-- ============================================================================
-- GROUND LOOT TABLE
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'ground_loot' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.ground_loot;
END
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

PRINT 'TABLE osrs.ground_loot created.';

-- ============================================================================
-- QUESTS TABLE
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'player_quests' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.player_quests;
END
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

PRINT 'TABLE osrs.player_quests created.';

-- ============================================================================
-- GRAND EXCHANGE ORDERS
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'ge_orders' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.ge_orders;
END
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

CREATE INDEX idx_ge_matching ON osrs.ge_orders(item_id, is_buy, price_per_unit DESC, created_at) WHERE completed_at IS NULL;
GO

PRINT 'TABLE osrs.ge_orders created.';

-- ============================================================================
-- TRADE HISTORY
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'trade_history' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.trade_history;
END
GO

CREATE TABLE osrs.trade_history (
  id INT IDENTITY(1,1) PRIMARY KEY,
  initiator_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  recipient_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  item_id INT NOT NULL REFERENCES osrs.items(id),
  quantity INT NOT NULL,
  completed_at DATETIME2 DEFAULT GETDATE()
);
GO

PRINT 'TABLE osrs.trade_history created.';

-- ============================================================================
-- ACHIEVEMENTS
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'player_achievements' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.player_achievements;
END
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

PRINT 'TABLE osrs.player_achievements created.';

-- ============================================================================
-- CHAT MESSAGES
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'chat_messages' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.chat_messages;
END
GO

CREATE TABLE osrs.chat_messages (
  id INT IDENTITY(1,1) PRIMARY KEY,
  sender_id INT REFERENCES osrs.players(id) ON DELETE SET NULL,
  sender_name VARCHAR(12),
  message_text VARCHAR(255) NOT NULL,
  chat_type INT DEFAULT 0,
  recipient_id INT REFERENCES osrs.players(id) ON DELETE SET NULL,
  created_at DATETIME2 DEFAULT GETDATE()
);
GO

PRINT 'TABLE osrs.chat_messages created.';

-- ============================================================================
-- PLAYER SESSIONS
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'player_sessions' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.player_sessions;
END
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
GO

PRINT 'TABLE osrs.player_sessions created.';

-- ============================================================================
-- PROCESSED PACKETS
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.objects WHERE name = N'processed_packets' AND schema_id = SCHEMA_ID('osrs'))
BEGIN
  DROP TABLE osrs.processed_packets;
END
GO

CREATE TABLE osrs.processed_packets (
  id INT IDENTITY(1,1) PRIMARY KEY,
  player_id INT NOT NULL REFERENCES osrs.players(id) ON DELETE CASCADE,
  packet_id BIGINT NOT NULL,
  processed_at DATETIME2 DEFAULT GETDATE(),
  
  UNIQUE (player_id, packet_id)
);
GO

PRINT 'TABLE osrs.processed_packets created.';

-- ============================================================================
-- VIEWS
-- ============================================================================

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

PRINT 'VIEWS created.';

-- ============================================================================
-- STORED PROCEDURES
-- ============================================================================

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

CREATE PROCEDURE osrs.sp_cleanup_idle_sessions
AS
BEGIN
  SET NOCOUNT ON;
  UPDATE osrs.player_sessions SET status = 1, session_end = GETDATE() WHERE status = 0 AND DATEDIFF(SECOND, last_heartbeat, GETDATE()) > 1800;
  DELETE FROM osrs.processed_packets WHERE DATEDIFF(SECOND, processed_at, GETDATE()) > 300;
  DELETE FROM osrs.ground_loot WHERE despawn_at IS NOT NULL AND GETDATE() > despawn_at;
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
  IF EXISTS (SELECT 1 FROM osrs.players WHERE username = @username) 
    RAISERROR('Username taken', 16, 1);
  INSERT INTO osrs.players (username, password_hash, email, created_at, x, y) VALUES (@username, @password_hash, @email, GETDATE(), 3222, 3218);
  SET @new_player_id = SCOPE_IDENTITY();
END
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

PRINT 'STORED PROCEDURES created.';

-- ============================================================================
-- FINAL STATUS
-- ============================================================================

PRINT '';
PRINT '========== SCHEMA CREATION COMPLETE ==========';
PRINT 'Database: osrsmmorp';
PRINT 'Schema: osrs';
PRINT 'Tables: 14';
PRINT 'Views: 2';
PRINT 'Procedures: 4';
PRINT 'Status: READY FOR DEVELOPMENT';
PRINT '';

SELECT COUNT(*) AS table_count FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'osrs';
GO
