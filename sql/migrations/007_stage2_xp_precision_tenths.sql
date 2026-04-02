-- ============================================================================
-- Migration 007: Stage 2 — XP precision: convert all skill XP from whole
--   integers to fixed-point tenths (value * 10).
--
-- WHAT THIS DOES
--   Multiplies every *_xp column in erynfall.players by 10 so that the Java
--   layer can store fractional OSRS XP values exactly (e.g. Oak 37.5 XP is
--   stored as 375, Bones prayer 4.5 XP as 45).
--
--   Existing player XP is scaled up (5 = 50, meaning "5.0 XP" — no precision
--   is lost; whole-number XP is still representable exactly).
--
-- PREREQUISITES
--   - Migration 006 applied (tables live in erynfall schema).
--   - Game server STOPPED.
--
-- SAFE TO RE-RUN: Yes. Migration ledger guards against double-application.
-- ============================================================================

SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

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

    IF EXISTS (
        SELECT 1 FROM erynfall.schema_migrations
        WHERE migration_key = '2026_04_stage2_xp_precision_tenths'
    )
    BEGIN
        COMMIT TRANSACTION;
        RETURN;
    END;

    -- -------------------------------------------------------------------------
    -- 2. Scale all 23 skill XP columns by 10
    -- -------------------------------------------------------------------------
    UPDATE erynfall.players SET
        attack_xp        = attack_xp        * 10,
        strength_xp      = strength_xp      * 10,
        defence_xp       = defence_xp       * 10,
        hitpoints_xp     = hitpoints_xp     * 10,
        ranged_xp        = ranged_xp        * 10,
        magic_xp         = magic_xp         * 10,
        prayer_xp        = prayer_xp        * 10,
        woodcutting_xp   = woodcutting_xp   * 10,
        fishing_xp       = fishing_xp       * 10,
        cooking_xp       = cooking_xp       * 10,
        mining_xp        = mining_xp        * 10,
        smithing_xp      = smithing_xp      * 10,
        firemaking_xp    = firemaking_xp    * 10,
        crafting_xp      = crafting_xp      * 10,
        runecrafting_xp  = runecrafting_xp  * 10,
        fletching_xp     = fletching_xp     * 10,
        agility_xp       = agility_xp       * 10,
        herblore_xp      = herblore_xp      * 10,
        thieving_xp      = thieving_xp      * 10,
        slayer_xp        = slayer_xp        * 10,
        farming_xp       = farming_xp       * 10,
        hunter_xp        = hunter_xp        * 10,
        construction_xp  = construction_xp  * 10;

    -- -------------------------------------------------------------------------
    -- 3. Record migration
    -- -------------------------------------------------------------------------
    INSERT INTO erynfall.schema_migrations (migration_key)
    VALUES ('2026_04_stage2_xp_precision_tenths');

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

-- ============================================================================
-- Post-run verification (run manually to confirm):
-- SELECT id, username, hitpoints_xp FROM erynfall.players;
-- -- Expect: hitpoints_xp = 11540 for new characters (was 1154), or existing
-- --         XP values multiplied by 10.
-- SELECT migration_key, applied_at FROM erynfall.schema_migrations ORDER BY id;
-- ============================================================================
