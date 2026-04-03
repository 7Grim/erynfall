-- ============================================================================
-- Migration 009: Normalize bank slot identity for tabs/search
--
-- Makes slot_index globally unique per player. tab_index remains persistent
-- metadata used for grouping/filtering, but slot_index becomes the primary
-- bank position identity across the whole bank.
-- ============================================================================
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
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
        WHERE migration_key = '2026_04_bank_slot_global_identity'
    )
    BEGIN
        COMMIT TRANSACTION;
        RETURN;
    END;
    IF NOT EXISTS (
        SELECT 1
        FROM sys.key_constraints
        WHERE name = 'UQ_player_bank_items_player_slot'
    )
    BEGIN
        ALTER TABLE erynfall.player_bank_items
            ADD CONSTRAINT UQ_player_bank_items_player_slot
            UNIQUE (player_id, slot_index);
    END;
    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = 'IX_player_bank_items_player_slot'
          AND object_id = OBJECT_ID('erynfall.player_bank_items')
    )
    BEGIN
        CREATE INDEX IX_player_bank_items_player_slot
            ON erynfall.player_bank_items (player_id, slot_index);
    END;
    INSERT INTO erynfall.schema_migrations (migration_key)
    VALUES ('2026_04_bank_slot_global_identity');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
