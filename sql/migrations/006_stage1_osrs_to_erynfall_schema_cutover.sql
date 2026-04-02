-- ============================================================================
-- Migration 006: Stage 1 — Move all live game objects from osrs to erynfall.
--
-- WHAT THIS DOES
--   1. Ensures erynfall and legacy schemas exist.
--   2. Creates a schema_migrations ledger (idempotent guard).
--   3. Drops osrs views/procs (they hardcode osrs.* so must be recreated).
--   4. Archives stale osrs auth duplicates to legacy (erynfall is the live copy).
--   5. Moves all live game tables from osrs to erynfall via ALTER SCHEMA TRANSFER.
--   6. Recreates views and stored procedures under erynfall.
--   7. Records the migration key so re-running this script is a safe no-op.
--
-- PREREQUISITES
--   - erynfall.accounts, erynfall.characters etc. already exist and are live
--     (created by migration 003).
--   - Game server and auth service are STOPPED.
--   - Backups of all tables listed in the pre-run checklist have been taken.
--
-- SYNTAX NOTE
--   GO is a batch separator and cannot be used inside a TRY/CATCH block.
--   All DDL (CREATE VIEW, CREATE PROCEDURE) is therefore executed via EXEC()
--   which runs in a child scope but commits to the same transaction.
--   ALTER SCHEMA TRANSFER is DML-like and safe inside a transaction.
--
-- SAFE TO RE-RUN: Yes. The migration ledger check returns immediately if
--   migration_key '2026_04_stage1_osrs_to_erynfall_schema_cutover' exists.
-- ============================================================================

SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    -- -------------------------------------------------------------------------
    -- 0. Ensure target and archive schemas exist
    -- -------------------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'erynfall')
        EXEC('CREATE SCHEMA erynfall');

    IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'legacy')
        EXEC('CREATE SCHEMA legacy');

    -- -------------------------------------------------------------------------
    -- 1. Migration ledger (idempotency guard)
    -- -------------------------------------------------------------------------
    IF OBJECT_ID(N'erynfall.schema_migrations', N'U') IS NULL
    BEGIN
        CREATE TABLE erynfall.schema_migrations (
            id            INT IDENTITY(1,1) PRIMARY KEY,
            migration_key VARCHAR(128) NOT NULL UNIQUE,
            applied_at    DATETIME2    NOT NULL DEFAULT GETDATE()
        );
    END;

    -- Short-circuit if already applied
    IF EXISTS (
        SELECT 1 FROM erynfall.schema_migrations
        WHERE migration_key = '2026_04_stage1_osrs_to_erynfall_schema_cutover'
    )
    BEGIN
        COMMIT TRANSACTION;
        RETURN;
    END;

    -- -------------------------------------------------------------------------
    -- 2. Drop live osrs views/procs before transferring tables.
    --    They reference osrs.* and will break after the transfer anyway.
    -- -------------------------------------------------------------------------
    DROP VIEW IF EXISTS osrs.active_players;
    DROP VIEW IF EXISTS osrs.hiscores;
    DROP PROCEDURE IF EXISTS osrs.sp_add_experience;
    DROP PROCEDURE IF EXISTS osrs.sp_cleanup_idle_sessions;
    DROP PROCEDURE IF EXISTS osrs.sp_complete_quest_objective;
    DROP PROCEDURE IF EXISTS osrs.sp_create_player;
    DROP PROCEDURE IF EXISTS osrs.sp_delete_player_safe;

    -- -------------------------------------------------------------------------
    -- 3. Archive stale duplicate auth tables: osrs -> legacy.
    --    Only transfers when the live erynfall copy already exists and the
    --    legacy slot is empty. Skips silently if conditions are not met.
    -- -------------------------------------------------------------------------
    IF OBJECT_ID(N'osrs.accounts', N'U')              IS NOT NULL
    AND OBJECT_ID(N'erynfall.accounts', N'U')          IS NOT NULL
    AND OBJECT_ID(N'legacy.accounts', N'U')             IS NULL
        EXEC('ALTER SCHEMA legacy TRANSFER osrs.accounts');

    IF OBJECT_ID(N'osrs.characters', N'U')             IS NOT NULL
    AND OBJECT_ID(N'erynfall.characters', N'U')         IS NOT NULL
    AND OBJECT_ID(N'legacy.characters', N'U')            IS NULL
        EXEC('ALTER SCHEMA legacy TRANSFER osrs.characters');

    IF OBJECT_ID(N'osrs.auth_refresh_tokens', N'U')    IS NOT NULL
    AND OBJECT_ID(N'erynfall.auth_refresh_tokens', N'U') IS NOT NULL
    AND OBJECT_ID(N'legacy.auth_refresh_tokens', N'U')   IS NULL
        EXEC('ALTER SCHEMA legacy TRANSFER osrs.auth_refresh_tokens');

    IF OBJECT_ID(N'osrs.account_recovery_tokens', N'U')    IS NOT NULL
    AND OBJECT_ID(N'erynfall.account_recovery_tokens', N'U') IS NOT NULL
    AND OBJECT_ID(N'legacy.account_recovery_tokens', N'U')   IS NULL
        EXEC('ALTER SCHEMA legacy TRANSFER osrs.account_recovery_tokens');

    IF OBJECT_ID(N'osrs.auth_audit', N'U')             IS NOT NULL
    AND OBJECT_ID(N'erynfall.auth_audit', N'U')         IS NOT NULL
    AND OBJECT_ID(N'legacy.auth_audit', N'U')            IS NULL
        EXEC('ALTER SCHEMA legacy TRANSFER osrs.auth_audit');

    -- -------------------------------------------------------------------------
    -- 4. Move all live game tables from osrs -> erynfall.
    --    ALTER SCHEMA TRANSFER preserves data, indexes, and FK constraints that
    --    stay within the transferred set. Each transfer is guarded so re-runs
    --    are safe and partial failures leave a clear state.
    -- -------------------------------------------------------------------------

    -- config
    IF OBJECT_ID(N'osrs.config', N'U')             IS NOT NULL
    AND OBJECT_ID(N'erynfall.config', N'U')         IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.config');

    -- items
    IF OBJECT_ID(N'osrs.items', N'U')              IS NOT NULL
    AND OBJECT_ID(N'erynfall.items', N'U')          IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.items');

    -- npcs (must precede npc_loot_tables because of FK)
    IF OBJECT_ID(N'osrs.npcs', N'U')               IS NOT NULL
    AND OBJECT_ID(N'erynfall.npcs', N'U')           IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.npcs');

    -- npc_loot_tables (FK -> npcs)
    IF OBJECT_ID(N'osrs.npc_loot_tables', N'U')    IS NOT NULL
    AND OBJECT_ID(N'erynfall.npc_loot_tables', N'U') IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.npc_loot_tables');

    -- players (root table for most FKs; transfer before child tables)
    IF OBJECT_ID(N'osrs.players', N'U')            IS NOT NULL
    AND OBJECT_ID(N'erynfall.players', N'U')        IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.players');

    -- inventory (FK -> players)
    IF OBJECT_ID(N'osrs.inventory', N'U')          IS NOT NULL
    AND OBJECT_ID(N'erynfall.inventory', N'U')      IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.inventory');

    -- player_friends (FK -> players)
    IF OBJECT_ID(N'osrs.player_friends', N'U')     IS NOT NULL
    AND OBJECT_ID(N'erynfall.player_friends', N'U') IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.player_friends');

    -- ground_loot
    IF OBJECT_ID(N'osrs.ground_loot', N'U')        IS NOT NULL
    AND OBJECT_ID(N'erynfall.ground_loot', N'U')    IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.ground_loot');

    -- player_quests (FK -> players)
    IF OBJECT_ID(N'osrs.player_quests', N'U')      IS NOT NULL
    AND OBJECT_ID(N'erynfall.player_quests', N'U')  IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.player_quests');

    -- player_quest_tasks (FK -> players)
    IF OBJECT_ID(N'osrs.player_quest_tasks', N'U') IS NOT NULL
    AND OBJECT_ID(N'erynfall.player_quest_tasks', N'U') IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.player_quest_tasks');

    -- ge_orders (FK -> players)
    IF OBJECT_ID(N'osrs.ge_orders', N'U')          IS NOT NULL
    AND OBJECT_ID(N'erynfall.ge_orders', N'U')      IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.ge_orders');

    -- trade_history (FK -> players, NO ACTION so safe)
    IF OBJECT_ID(N'osrs.trade_history', N'U')      IS NOT NULL
    AND OBJECT_ID(N'erynfall.trade_history', N'U')  IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.trade_history');

    -- player_achievements (FK -> players)
    IF OBJECT_ID(N'osrs.player_achievements', N'U')IS NOT NULL
    AND OBJECT_ID(N'erynfall.player_achievements', N'U') IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.player_achievements');

    -- chat_messages (FK -> players, NO ACTION)
    IF OBJECT_ID(N'osrs.chat_messages', N'U')      IS NOT NULL
    AND OBJECT_ID(N'erynfall.chat_messages', N'U')  IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.chat_messages');

    -- player_sessions (FK -> players)
    IF OBJECT_ID(N'osrs.player_sessions', N'U')    IS NOT NULL
    AND OBJECT_ID(N'erynfall.player_sessions', N'U') IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.player_sessions');

    -- processed_packets (no FK to players, standalone)
    IF OBJECT_ID(N'osrs.processed_packets', N'U')  IS NOT NULL
    AND OBJECT_ID(N'erynfall.processed_packets', N'U') IS NULL
        EXEC('ALTER SCHEMA erynfall TRANSFER osrs.processed_packets');

    -- -------------------------------------------------------------------------
    -- 5. Recreate views under erynfall.
    --    Current semantics preserved — XP formula fix is Stage 2.
    -- -------------------------------------------------------------------------

    EXEC('
    CREATE OR ALTER VIEW erynfall.active_players AS
    SELECT
        p.id,
        p.username,
        p.x,
        p.y,
        ps.session_start,
        ps.last_heartbeat,
        DATEDIFF(SECOND, ps.last_heartbeat, GETDATE()) AS seconds_since_heartbeat,
        ps.status
    FROM erynfall.players p
    INNER JOIN erynfall.player_sessions ps ON p.id = ps.player_id
    WHERE ps.session_end IS NULL AND p.is_dead = 0;
    ');

    EXEC('
    CREATE OR ALTER VIEW erynfall.hiscores AS
    SELECT
        p.id,
        p.username,
        CAST(POWER(CASE WHEN p.attack_xp      = 0 THEN 1 ELSE p.attack_xp      / 4.0 END, 1.0/3.0) AS INT) AS attack_level,
        CAST(POWER(CASE WHEN p.strength_xp    = 0 THEN 1 ELSE p.strength_xp    / 4.0 END, 1.0/3.0) AS INT) AS strength_level,
        CAST(POWER(CASE WHEN p.defence_xp     = 0 THEN 1 ELSE p.defence_xp     / 4.0 END, 1.0/3.0) AS INT) AS defence_level,
        CAST(POWER(CASE WHEN p.magic_xp       = 0 THEN 1 ELSE p.magic_xp       / 4.0 END, 1.0/3.0) AS INT) AS magic_level,
        CAST(POWER(CASE WHEN p.prayer_xp      = 0 THEN 1 ELSE p.prayer_xp      / 4.0 END, 1.0/3.0) AS INT) AS prayer_level,
        CAST(POWER(CASE WHEN p.woodcutting_xp = 0 THEN 1 ELSE p.woodcutting_xp / 4.0 END, 1.0/3.0) AS INT) AS woodcutting_level,
        CAST(POWER(CASE WHEN p.fishing_xp     = 0 THEN 1 ELSE p.fishing_xp     / 4.0 END, 1.0/3.0) AS INT) AS fishing_level,
        CAST(POWER(CASE WHEN p.cooking_xp     = 0 THEN 1 ELSE p.cooking_xp     / 4.0 END, 1.0/3.0) AS INT) AS cooking_level,
        (
            CAST(POWER(CASE WHEN p.attack_xp      = 0 THEN 1 ELSE p.attack_xp      / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.strength_xp    = 0 THEN 1 ELSE p.strength_xp    / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.defence_xp     = 0 THEN 1 ELSE p.defence_xp     / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.magic_xp       = 0 THEN 1 ELSE p.magic_xp       / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.prayer_xp      = 0 THEN 1 ELSE p.prayer_xp      / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.woodcutting_xp = 0 THEN 1 ELSE p.woodcutting_xp / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.fishing_xp     = 0 THEN 1 ELSE p.fishing_xp     / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.cooking_xp     = 0 THEN 1 ELSE p.cooking_xp     / 4.0 END, 1.0/3.0) AS INT)
        ) AS overall_level,
        ROW_NUMBER() OVER (ORDER BY
            CAST(POWER(CASE WHEN p.attack_xp      = 0 THEN 1 ELSE p.attack_xp      / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.strength_xp    = 0 THEN 1 ELSE p.strength_xp    / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.defence_xp     = 0 THEN 1 ELSE p.defence_xp     / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.magic_xp       = 0 THEN 1 ELSE p.magic_xp       / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.prayer_xp      = 0 THEN 1 ELSE p.prayer_xp      / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.woodcutting_xp = 0 THEN 1 ELSE p.woodcutting_xp / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.fishing_xp     = 0 THEN 1 ELSE p.fishing_xp     / 4.0 END, 1.0/3.0) AS INT) +
            CAST(POWER(CASE WHEN p.cooking_xp     = 0 THEN 1 ELSE p.cooking_xp     / 4.0 END, 1.0/3.0) AS INT)
        DESC) AS overall_rank
    FROM erynfall.players p;
    ');

    -- -------------------------------------------------------------------------
    -- 6. Recreate stored procedures under erynfall.
    --    Semantics preserved from osrs originals — XP correctness is Stage 2.
    -- -------------------------------------------------------------------------

    EXEC('
    CREATE OR ALTER PROCEDURE erynfall.sp_add_experience
        @player_id  INT,
        @skill_name VARCHAR(50),
        @xp_delta   BIGINT
    AS
    BEGIN
        SET NOCOUNT ON;
        DECLARE @max_xp BIGINT = 200000000;
        IF      @skill_name = ''ATTACK''      UPDATE erynfall.players SET attack_xp      = CASE WHEN attack_xp      + @xp_delta > @max_xp THEN @max_xp ELSE attack_xp      + @xp_delta END WHERE id = @player_id;
        ELSE IF @skill_name = ''STRENGTH''    UPDATE erynfall.players SET strength_xp    = CASE WHEN strength_xp    + @xp_delta > @max_xp THEN @max_xp ELSE strength_xp    + @xp_delta END WHERE id = @player_id;
        ELSE IF @skill_name = ''DEFENCE''     UPDATE erynfall.players SET defence_xp     = CASE WHEN defence_xp     + @xp_delta > @max_xp THEN @max_xp ELSE defence_xp     + @xp_delta END WHERE id = @player_id;
        ELSE IF @skill_name = ''MAGIC''       UPDATE erynfall.players SET magic_xp       = CASE WHEN magic_xp       + @xp_delta > @max_xp THEN @max_xp ELSE magic_xp       + @xp_delta END WHERE id = @player_id;
        ELSE IF @skill_name = ''PRAYER''      UPDATE erynfall.players SET prayer_xp      = CASE WHEN prayer_xp      + @xp_delta > @max_xp THEN @max_xp ELSE prayer_xp      + @xp_delta END WHERE id = @player_id;
        ELSE IF @skill_name = ''WOODCUTTING'' UPDATE erynfall.players SET woodcutting_xp = CASE WHEN woodcutting_xp + @xp_delta > @max_xp THEN @max_xp ELSE woodcutting_xp + @xp_delta END WHERE id = @player_id;
        ELSE IF @skill_name = ''FISHING''     UPDATE erynfall.players SET fishing_xp     = CASE WHEN fishing_xp     + @xp_delta > @max_xp THEN @max_xp ELSE fishing_xp     + @xp_delta END WHERE id = @player_id;
        ELSE IF @skill_name = ''COOKING''     UPDATE erynfall.players SET cooking_xp     = CASE WHEN cooking_xp     + @xp_delta > @max_xp THEN @max_xp ELSE cooking_xp     + @xp_delta END WHERE id = @player_id;
    END
    ');

    EXEC('
    CREATE OR ALTER PROCEDURE erynfall.sp_cleanup_idle_sessions
    AS
    BEGIN
        SET NOCOUNT ON;
        UPDATE erynfall.player_sessions
        SET    status = 1, session_end = GETDATE()
        WHERE  status = 0 AND DATEDIFF(SECOND, last_heartbeat, GETDATE()) > 1800;

        DELETE FROM erynfall.processed_packets
        WHERE DATEDIFF(SECOND, processed_at, GETDATE()) > 300;

        DELETE FROM erynfall.ground_loot
        WHERE despawn_at IS NOT NULL AND GETDATE() > despawn_at;
    END
    ');

    EXEC('
    CREATE OR ALTER PROCEDURE erynfall.sp_complete_quest_objective
        @player_id      INT,
        @quest_id       INT,
        @objective_index INT
    AS
    BEGIN
        SET NOCOUNT ON;
        DECLARE @bitmask INT = (1 << @objective_index);
        UPDATE erynfall.player_quests
        SET    completed_objectives = completed_objectives | @bitmask
        WHERE  player_id = @player_id AND quest_id = @quest_id;
    END
    ');

    EXEC('
    CREATE OR ALTER PROCEDURE erynfall.sp_create_player
        @username       VARCHAR(12),
        @password_hash  VARCHAR(255),
        @email          VARCHAR(255) = NULL,
        @new_player_id  INT OUTPUT
    AS
    BEGIN
        SET NOCOUNT ON;
        IF EXISTS (SELECT 1 FROM erynfall.players WHERE username = @username)
        BEGIN
            RAISERROR(''Username taken'', 16, 1);
            RETURN;
        END
        INSERT INTO erynfall.players (username, password_hash, email, created_at, x, y)
        VALUES (@username, @password_hash, @email, GETDATE(), 3222, 3218);
        SET @new_player_id = SCOPE_IDENTITY();
    END
    ');

    EXEC('
    CREATE OR ALTER PROCEDURE erynfall.sp_delete_player_safe
        @player_id INT
    AS
    BEGIN
        SET NOCOUNT ON;
        SET XACT_ABORT ON;
        BEGIN TRY
            BEGIN TRANSACTION;
            DELETE FROM erynfall.player_friends WHERE friend_player_id = @player_id;
            DELETE FROM erynfall.players         WHERE id              = @player_id;
            COMMIT TRANSACTION;
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            THROW;
        END CATCH
    END
    ');

    -- -------------------------------------------------------------------------
    -- 7. Record migration — marks this script as applied
    -- -------------------------------------------------------------------------
    INSERT INTO erynfall.schema_migrations (migration_key)
    VALUES ('2026_04_stage1_osrs_to_erynfall_schema_cutover');

    COMMIT TRANSACTION;

END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

-- ============================================================================
-- POST-RUN VERIFICATION QUERIES
-- Run these immediately after the script completes successfully.
-- ============================================================================

-- 1. Confirm no game tables remain under osrs
-- SELECT s.name AS schema_name, t.name AS table_name
-- FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id
-- WHERE s.name = 'osrs' ORDER BY t.name;
-- Expected: zero rows (or only objects you intentionally left)

-- 2. Confirm legacy archive received the stale auth tables
-- SELECT s.name AS schema_name, t.name AS table_name
-- FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id
-- WHERE s.name = 'legacy' ORDER BY t.name;
-- Expected: accounts, characters, auth_refresh_tokens, account_recovery_tokens, auth_audit

-- 3. Confirm all game tables are now under erynfall
-- SELECT s.name AS schema_name, t.name AS table_name
-- FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id
-- WHERE s.name = 'erynfall' ORDER BY t.name;

-- 4. Confirm views recreated under erynfall (none under osrs)
-- SELECT s.name AS schema_name, v.name AS view_name
-- FROM sys.views v JOIN sys.schemas s ON s.schema_id = v.schema_id
-- WHERE s.name IN ('osrs','erynfall','legacy') ORDER BY s.name, v.name;
-- Expected: erynfall.active_players, erynfall.hiscores only

-- 5. Confirm procs recreated under erynfall (none under osrs)
-- SELECT s.name AS schema_name, p.name AS procedure_name
-- FROM sys.procedures p JOIN sys.schemas s ON s.schema_id = p.schema_id
-- WHERE s.name IN ('osrs','erynfall','legacy') ORDER BY s.name, p.name;

-- 6. Confirm player rows survived intact
-- SELECT id, username, woodcutting_xp, fishing_xp, cooking_xp
-- FROM erynfall.players ORDER BY id;
-- Expected: same rows as before — data is preserved by ALTER SCHEMA TRANSFER

-- 7. Confirm migration is recorded
-- SELECT * FROM erynfall.schema_migrations ORDER BY applied_at;
