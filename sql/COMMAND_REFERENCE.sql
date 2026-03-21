-- ============================================================================
-- OSRS-MMORP SQL Command Reference
-- ============================================================================
-- Quick copy-paste commands for common operations
-- Use these to test the database or perform admin tasks
-- ============================================================================

USE osrsmmorp;
GO

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Check all tables exist (should return 14)
SELECT COUNT(*) AS tables_created FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'osrs';

-- Check all stored procedures exist (should return 4)
SELECT COUNT(*) AS procedures_created FROM INFORMATION_SCHEMA.ROUTINES 
WHERE ROUTINE_SCHEMA = 'osrs' AND ROUTINE_TYPE = 'PROCEDURE';

-- Check all views exist (should return 2)
SELECT COUNT(*) AS views_created FROM INFORMATION_SCHEMA.VIEWS 
WHERE TABLE_SCHEMA = 'osrs';

-- List all tables
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'osrs' ORDER BY TABLE_NAME;

-- ============================================================================
-- PLAYER MANAGEMENT
-- ============================================================================

-- Create a test player
DECLARE @player_id INT;
EXEC osrs.sp_create_player 
  @username = 'Tester',
  @password_hash = 'PLACEHOLDER_HASH',
  @email = 'test@example.com',
  @new_player_id = @player_id OUTPUT;
PRINT 'Created player ID: ' + CAST(@player_id AS VARCHAR(10));

-- Get all players
SELECT id, username, created_at, last_login, x, y, total_gold 
FROM osrs.players 
ORDER BY created_at DESC;

-- Get specific player details
SELECT 
  username,
  created_at,
  x, y,
  attack_xp, strength_xp, defence_xp, magic_xp, prayer_xp, woodcutting_xp, fishing_xp, cooking_xp,
  total_gold,
  is_dead
FROM osrs.players 
WHERE username = 'Tester';

-- Get player position
SELECT id, username, x, y FROM osrs.players WHERE id = 1;

-- Get all online players
SELECT * FROM osrs.active_players;

-- ============================================================================
-- EXPERIENCE & LEVELING
-- ============================================================================

-- Award XP (call stored procedure)
EXEC osrs.sp_add_experience 1, 'ATTACK', 1000;
EXEC osrs.sp_add_experience 1, 'STRENGTH', 500;
EXEC osrs.sp_add_experience 1, 'COOKING', 2000;

-- Check player XP and levels
SELECT 
  username,
  attack_xp,
  CAST(POWER(CASE WHEN attack_xp = 0 THEN 1 ELSE attack_xp / 4.0 END, 1.0/3.0) AS INT) AS attack_level,
  strength_xp,
  CAST(POWER(CASE WHEN strength_xp = 0 THEN 1 ELSE strength_xp / 4.0 END, 1.0/3.0) AS INT) AS strength_level,
  cooking_xp,
  CAST(POWER(CASE WHEN cooking_xp = 0 THEN 1 ELSE cooking_xp / 4.0 END, 1.0/3.0) AS INT) AS cooking_level
FROM osrs.players 
WHERE id = 1;

-- Get hiscores (leaderboard)
SELECT TOP 10 
  overall_rank,
  username,
  overall_level,
  attack_level,
  strength_level,
  defence_level,
  magic_level,
  prayer_level,
  woodcutting_level,
  fishing_level,
  cooking_level
FROM osrs.hiscores 
ORDER BY overall_rank;

-- ============================================================================
-- INVENTORY MANAGEMENT
-- ============================================================================

-- Add item to inventory (for player_id=1, slot=0, item_id=2 [Raw Chicken], qty=1)
INSERT INTO osrs.inventory (player_id, slot_index, item_id, quantity)
VALUES (1, 0, 2, 1);

-- Get player inventory
SELECT 
  i.slot_index,
  i.item_id,
  item.name,
  i.quantity,
  item.weight_kg,
  item.stackable
FROM osrs.inventory i
INNER JOIN osrs.items item ON i.item_id = item.id
WHERE i.player_id = 1
ORDER BY i.slot_index;

-- Check if inventory is full (28 slots)
SELECT 
  player_id,
  COUNT(*) AS filled_slots,
  (28 - COUNT(*)) AS empty_slots
FROM osrs.inventory
WHERE player_id = 1
GROUP BY player_id;

-- Drop item from inventory (delete from slot)
DELETE FROM osrs.inventory WHERE player_id = 1 AND slot_index = 0;

-- ============================================================================
-- ITEMS & DEFINITIONS
-- ============================================================================

-- List all items
SELECT id, name, stackable, weight_kg, tradeable, high_alchemy_value
FROM osrs.items
ORDER BY id;

-- Find item by name
SELECT * FROM osrs.items WHERE name LIKE '%Chicken%';

-- Find stackable items
SELECT id, name, weight_kg FROM osrs.items WHERE stackable = 1;

-- ============================================================================
-- NPC MANAGEMENT
-- ============================================================================

-- List all NPCs
SELECT 
  id, name, combat_level, max_hp, spawn_x, spawn_y, respawn_ticks, max_alive
FROM osrs.npcs
ORDER BY combat_level;

-- Get NPC spawn locations
SELECT id, name, spawn_x, spawn_y, spawn_id FROM osrs.npcs;

-- Get loot table for an NPC
SELECT 
  nl.npc_id,
  n.name,
  i.name AS item_name,
  nl.quantity_min,
  nl.quantity_max,
  nl.drop_rate,
  nl.always_drop
FROM osrs.npc_loot_tables nl
INNER JOIN osrs.npcs n ON nl.npc_id = n.id
INNER JOIN osrs.items i ON nl.item_id = i.id
WHERE nl.npc_id = 1
ORDER BY nl.sort_order;

-- ============================================================================
-- GROUND LOOT
-- ============================================================================

-- Add loot to ground (item_id=1 [Bones], qty=5, at x=3230, y=3205, owner=1 [60 min])
INSERT INTO osrs.ground_loot (item_id, quantity, x, y, owner_id, dropped_at, despawn_at)
VALUES (1, 5, 3230, 3205, 1, GETDATE(), DATEADD(HOUR, 1, GETDATE()));

-- Get loot at location
SELECT 
  g.id,
  i.name,
  g.quantity,
  g.x,
  g.y,
  p.username AS owner,
  g.dropped_at
FROM osrs.ground_loot g
INNER JOIN osrs.items i ON g.item_id = i.id
LEFT JOIN osrs.players p ON g.owner_id = p.id
WHERE g.x = 3230 AND g.y = 3205;

-- Get all despawned loot
SELECT COUNT(*) AS despawned FROM osrs.ground_loot WHERE despawn_at < GETDATE();

-- ============================================================================
-- QUESTS
-- ============================================================================

-- Start a quest for player
INSERT INTO osrs.player_quests (player_id, quest_id, status, started_at)
VALUES (1, 1, 1, GETDATE());

-- Complete quest objective (atomic bitmask operation)
EXEC osrs.sp_complete_quest_objective @player_id = 1, @quest_id = 1, @objective_index = 0;
EXEC osrs.sp_complete_quest_objective @player_id = 1, @quest_id = 1, @objective_index = 1;

-- Check quest progress
SELECT 
  pq.quest_id,
  pq.status,
  pq.completed_objectives,
  pq.started_at,
  pq.completed_at
FROM osrs.player_quests pq
WHERE pq.player_id = 1
ORDER BY pq.quest_id;

-- Mark quest as complete
UPDATE osrs.player_quests 
SET status = 2, completed_at = GETDATE() 
WHERE player_id = 1 AND quest_id = 1;

-- ============================================================================
-- GRAND EXCHANGE (GE)
-- ============================================================================

-- Create a buy order
INSERT INTO osrs.ge_orders (player_id, item_id, quantity, price_per_unit, is_buy, created_at)
VALUES (1, 6, 10, 50, 1, GETDATE());  -- Buy 10 logs at 50 gp each

-- Create a sell order
INSERT INTO osrs.ge_orders (player_id, item_id, quantity, price_per_unit, is_buy, created_at)
VALUES (1, 1, 20, 100, 0, GETDATE());  -- Sell 20 bones at 100 gp each

-- Get all active orders
SELECT 
  player_id,
  item_id,
  quantity,
  price_per_unit,
  CASE WHEN is_buy = 1 THEN 'BUY' ELSE 'SELL' END AS order_type,
  filled_quantity,
  quantity - filled_quantity AS remaining
FROM osrs.ge_orders
WHERE completed_at IS NULL
ORDER BY created_at DESC;

-- Get player's GE orders
SELECT 
  id,
  item_id,
  quantity,
  price_per_unit,
  CASE WHEN is_buy = 1 THEN 'BUY' ELSE 'SELL' END AS type,
  filled_quantity,
  completed_at
FROM osrs.ge_orders
WHERE player_id = 1
ORDER BY created_at DESC;

-- Complete a GE order (mark as filled)
UPDATE osrs.ge_orders 
SET filled_quantity = quantity, completed_at = GETDATE() 
WHERE id = 1;

-- ============================================================================
-- TRADING (P2P)
-- ============================================================================

-- Record a player trade
INSERT INTO osrs.trade_history (initiator_id, recipient_id, item_id, quantity, completed_at)
VALUES (1, 2, 1, 5, GETDATE());  -- Player 1 trades 5 bones to Player 2

-- Get trade history for player
SELECT 
  t.id,
  p1.username AS from_player,
  p2.username AS to_player,
  i.name AS item,
  t.quantity,
  t.completed_at
FROM osrs.trade_history t
INNER JOIN osrs.players p1 ON t.initiator_id = p1.id
INNER JOIN osrs.players p2 ON t.recipient_id = p2.id
INNER JOIN osrs.items i ON t.item_id = i.id
WHERE t.initiator_id = 1 OR t.recipient_id = 1
ORDER BY t.completed_at DESC;

-- ============================================================================
-- CHAT MESSAGES
-- ============================================================================

-- Insert a public chat message
INSERT INTO osrs.chat_messages (sender_id, sender_name, message_text, chat_type, created_at)
VALUES (1, 'Tester', 'Hello everyone!', 0, GETDATE());

-- Insert a private message
INSERT INTO osrs.chat_messages (sender_id, sender_name, message_text, chat_type, recipient_id, created_at)
VALUES (1, 'Tester', 'Hey, want to trade?', 1, 2, GETDATE());

-- Get all public chat (recent 20)
SELECT TOP 20
  sender_name,
  message_text,
  created_at
FROM osrs.chat_messages
WHERE chat_type = 0
ORDER BY created_at DESC;

-- Get private messages for player
SELECT 
  sender_name,
  message_text,
  created_at
FROM osrs.chat_messages
WHERE chat_type = 1 AND (recipient_id = 1 OR sender_id = 1)
ORDER BY created_at DESC;

-- ============================================================================
-- ACHIEVEMENTS
-- ============================================================================

-- Award achievement to player
INSERT INTO osrs.player_achievements (player_id, achievement_id, unlocked_at)
VALUES (1, 1, GETDATE());

-- Get player achievements
SELECT 
  player_id,
  achievement_id,
  unlocked_at,
  progress
FROM osrs.player_achievements
WHERE player_id = 1
ORDER BY unlocked_at DESC;

-- ============================================================================
-- SESSIONS & MONITORING
-- ============================================================================

-- Create a session when player logs in
INSERT INTO osrs.player_sessions (player_id, session_start, last_heartbeat, ip_address, status)
VALUES (1, GETDATE(), GETDATE(), '192.168.1.100', 0);

-- Update heartbeat (player is still active)
UPDATE osrs.player_sessions
SET last_heartbeat = GETDATE()
WHERE player_id = 1 AND session_end IS NULL;

-- Get active sessions
SELECT 
  ps.id,
  p.username,
  ps.session_start,
  ps.last_heartbeat,
  DATEDIFF(SECOND, ps.last_heartbeat, GETDATE()) AS seconds_since_heartbeat,
  ps.ip_address
FROM osrs.player_sessions ps
INNER JOIN osrs.players p ON ps.player_id = p.id
WHERE ps.session_end IS NULL
ORDER BY ps.session_start DESC;

-- Mark player offline (on logout)
UPDATE osrs.player_sessions
SET session_end = GETDATE(), status = 1
WHERE player_id = 1 AND session_end IS NULL;

-- ============================================================================
-- MAINTENANCE
-- ============================================================================

-- Run cleanup job (idle sessions, old packets, despawned loot)
EXEC osrs.sp_cleanup_idle_sessions;

-- Check config settings
SELECT config_key, config_value FROM osrs.config;

-- Update a config value
UPDATE osrs.config SET config_value = '512' WHERE config_key = 'MAX_PLAYERS_MVP';

-- ============================================================================
-- ADMIN OPERATIONS
-- ============================================================================

-- Count total players
SELECT COUNT(*) AS total_players FROM osrs.players;

-- Count active players (in sessions)
SELECT COUNT(*) AS active_players FROM osrs.player_sessions 
WHERE session_end IS NULL;

-- Total items in circulation
SELECT 
  i.name,
  SUM(inv.quantity) AS total_in_inventory,
  COUNT(DISTINCT inv.player_id) AS owned_by_players
FROM osrs.inventory inv
INNER JOIN osrs.items i ON inv.item_id = i.id
GROUP BY i.name
ORDER BY total_in_inventory DESC;

-- GE order volume
SELECT 
  CASE WHEN is_buy = 1 THEN 'BUY' ELSE 'SELL' END AS order_type,
  COUNT(*) AS order_count,
  SUM(quantity) AS total_quantity
FROM osrs.ge_orders
WHERE completed_at IS NULL
GROUP BY is_buy;

-- ============================================================================
-- DATABASE STATISTICS
-- ============================================================================

-- Table row counts
SELECT 
  OBJECT_NAME(p.object_id) AS table_name,
  SUM(p.rows) AS row_count
FROM sys.partitions p
WHERE p.index_id IN (0, 1) AND OBJECT_SCHEMA_NAME(p.object_id) = 'osrs'
GROUP BY p.object_id
ORDER BY SUM(p.rows) DESC;

-- Index usage statistics
SELECT 
  OBJECT_NAME(ius.object_id) AS table_name,
  i.name AS index_name,
  ius.user_seeks,
  ius.user_scans,
  ius.user_lookups,
  ius.user_updates
FROM sys.dm_db_index_usage_stats ius
INNER JOIN sys.indexes i ON ius.object_id = i.object_id AND ius.index_id = i.index_id
WHERE database_id = DB_ID('osrsmmorp')
ORDER BY ius.user_seeks + ius.user_scans + ius.user_lookups DESC;

-- ============================================================================
-- END OF COMMAND REFERENCE
-- ============================================================================

-- Notes:
-- 1. All player_id references use ID=1 as example (Tester)
-- 2. Item IDs: 1=Bones, 2=Raw Chicken, 6=Logs, etc. (see osrs.items table)
-- 3. NPC IDs: 1=Chicken, 2=Goblin, 3=Cow (see osrs.npcs table)
-- 4. Always use parameterized queries in Java code (PreparedStatement)
-- 5. Use stored procedures for complex operations (sp_add_experience, etc.)
-- 6. Never concatenate SQL strings (SQL injection risk)
-- 7. Validate input in Java BEFORE sending to database
