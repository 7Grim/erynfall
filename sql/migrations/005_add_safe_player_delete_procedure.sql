-- Migration 005: Add a safe hard-delete procedure for players.
-- Required because osrs.player_friends.friend_player_id uses ON DELETE NO ACTION.

IF OBJECT_ID(N'osrs.sp_delete_player_safe', N'P') IS NOT NULL
BEGIN
    DROP PROCEDURE osrs.sp_delete_player_safe;
END
GO

CREATE PROCEDURE osrs.sp_delete_player_safe
    @player_id INT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        DELETE FROM osrs.player_friends
        WHERE friend_player_id = @player_id;

        DELETE FROM osrs.players
        WHERE id = @player_id;

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END
GO
