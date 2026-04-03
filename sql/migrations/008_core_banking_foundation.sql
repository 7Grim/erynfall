-- ============================================================================
-- Migration 008: Core banking foundation (character-specific bank storage)
--
-- Adds erynfall.player_bank_items keyed by erynfall.players.id.
-- This is schema-only groundwork for secure, session-owned bank interactions.
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
        WHERE migration_key = '2026_04_core_banking_foundation'
    )
    BEGIN
        COMMIT TRANSACTION;
        RETURN;
    END;

    CREATE TABLE erynfall.player_bank_items (
        id          INT IDENTITY(1,1) PRIMARY KEY,
        player_id   INT NOT NULL,
        tab_index   INT NOT NULL DEFAULT 0,
        slot_index  INT NOT NULL,
        item_id     INT NOT NULL,
        quantity    BIGINT NOT NULL,
        placeholder BIT NOT NULL DEFAULT 0,
        CONSTRAINT FK_player_bank_items_player
            FOREIGN KEY (player_id) REFERENCES erynfall.players(id) ON DELETE CASCADE,
        CONSTRAINT FK_player_bank_items_item
            FOREIGN KEY (item_id) REFERENCES erynfall.items(id)
    );

    ALTER TABLE erynfall.player_bank_items
        ADD CONSTRAINT UQ_player_bank_items_player_item
        UNIQUE (player_id, item_id);

    ALTER TABLE erynfall.player_bank_items
        ADD CONSTRAINT UQ_player_bank_items_player_tab_slot
        UNIQUE (player_id, tab_index, slot_index);

    CREATE INDEX IX_player_bank_items_player_tab_slot
        ON erynfall.player_bank_items (player_id, tab_index, slot_index);

    INSERT INTO erynfall.schema_migrations (migration_key)
    VALUES ('2026_04_core_banking_foundation');

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
