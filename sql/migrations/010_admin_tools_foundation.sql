-- ============================================================================
-- Migration 010: Admin tools foundation
--
-- Adds a per-character admin tools flag and an audit table for future admin
-- actions. This is the secure foundation only; no gameplay powers are added
-- by this migration.
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
        WHERE migration_key = '2026_04_admin_tools_foundation'
    )
    BEGIN
        COMMIT TRANSACTION;
        RETURN;
    END;
    IF COL_LENGTH('erynfall.players', 'admin_tools_enabled') IS NULL
    BEGIN
        ALTER TABLE erynfall.players
            ADD admin_tools_enabled BIT NOT NULL CONSTRAINT DF_players_admin_tools_enabled DEFAULT 0;
    END;
    IF OBJECT_ID(N'erynfall.admin_action_audit', N'U') IS NULL
    BEGIN
        CREATE TABLE erynfall.admin_action_audit (
            id          INT IDENTITY(1,1) PRIMARY KEY,
            player_id   INT NOT NULL,
            action_type VARCHAR(64) NOT NULL,
            details     VARCHAR(1024) NULL,
            created_at  DATETIME2 NOT NULL DEFAULT GETDATE(),
            CONSTRAINT FK_admin_action_audit_player
                FOREIGN KEY (player_id) REFERENCES erynfall.players(id) ON DELETE CASCADE
        );
        CREATE INDEX IX_admin_action_audit_player_created
            ON erynfall.admin_action_audit (player_id, created_at DESC);
    END;
    INSERT INTO erynfall.schema_migrations (migration_key)
    VALUES ('2026_04_admin_tools_foundation');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
