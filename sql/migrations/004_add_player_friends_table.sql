-- Migration 004: Add persistent friends/block list table.
-- Stores canonical player DB IDs (osrs.players.id), not runtime entity IDs.

IF OBJECT_ID(N'osrs.player_friends', N'U') IS NULL
BEGIN
    CREATE TABLE osrs.player_friends (
        id INT IDENTITY(1,1) PRIMARY KEY,
        player_id INT NOT NULL,
        friend_player_id INT NOT NULL,
        blocked BIT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        CONSTRAINT ck_player_friends_not_self CHECK (player_id <> friend_player_id),
        CONSTRAINT uq_player_friends_pair UNIQUE (player_id, friend_player_id)
    );
END
GO

IF EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'fk_player_friends_owner'
      AND parent_object_id = OBJECT_ID('osrs.player_friends')
)
BEGIN
    ALTER TABLE osrs.player_friends DROP CONSTRAINT fk_player_friends_owner;
END
GO

IF EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'fk_player_friends_target'
      AND parent_object_id = OBJECT_ID('osrs.player_friends')
)
BEGIN
    ALTER TABLE osrs.player_friends DROP CONSTRAINT fk_player_friends_target;
END
GO

ALTER TABLE osrs.player_friends
ADD CONSTRAINT fk_player_friends_owner
    FOREIGN KEY (player_id)
    REFERENCES osrs.players(id)
    ON DELETE CASCADE;
GO

ALTER TABLE osrs.player_friends
ADD CONSTRAINT fk_player_friends_target
    FOREIGN KEY (friend_player_id)
    REFERENCES osrs.players(id)
    ON DELETE NO ACTION;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_player_friends_owner'
      AND object_id = OBJECT_ID('osrs.player_friends')
)
BEGIN
    CREATE INDEX idx_player_friends_owner ON osrs.player_friends(player_id, blocked);
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_player_friends_target'
      AND object_id = OBJECT_ID('osrs.player_friends')
)
BEGIN
    CREATE INDEX idx_player_friends_target ON osrs.player_friends(friend_player_id);
END
GO
