package com.osrs.server.database;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.osrs.server.quest.Quest;
import com.osrs.server.quest.QuestManager;
import com.osrs.shared.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.UUID;

/**
 * Handles all player persistence: create, load, save.
 *
 * All writes use explicit conn.commit() because the pool has autoCommit=false.
 */
public class PlayerRepository {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerRepository.class);

    // BCrypt cost factor — 10 is a reasonable production default (~100ms on modern hardware)
    private static final int BCRYPT_COST = 10;
    private static final int PLAYER_ENTITY_ID_OFFSET = 100_000;

    // -----------------------------------------------------------------------
    // Auth: login / register
    // -----------------------------------------------------------------------

    /**
     * Attempt login. Returns the loaded Player if credentials are valid, null otherwise.
     *
     * @param username  case-insensitive username
     * @param plainPassword  password entered by the client (NOT hashed)
     * @param assignedId  the Netty channel ID to use as in-memory player ID
     */
    public static Player login(String username, String plainPassword, int assignedId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, password_hash, x, y, " +
                "attack_xp, strength_xp, defence_xp, hitpoints_xp, ranged_xp, magic_xp " +
                "FROM osrs.players WHERE LOWER(username) = LOWER(?)"
            );
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                LOG.info("Login failed — username not found: {}", username);
                return null;
            }

            String storedHash = rs.getString("password_hash");
            BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), storedHash);
            if (!result.verified) {
                LOG.info("Login failed — wrong password for: {}", username);
                return null;
            }

            // Credentials valid — build Player from DB row
            Player player = mapPlayerFromRow(rs, username);
            int dbId = rs.getInt("id");

            // Update last_login timestamp
            PreparedStatement upd = conn.prepareStatement(
                "UPDATE osrs.players SET last_login = GETDATE() WHERE id = ?"
            );
            upd.setInt(1, dbId);
            upd.executeUpdate();
            conn.commit();

            LOG.info("Login successful: {} (dbId={}, entityId={})", username, dbId, player.getId());
            return player;

        } catch (SQLException e) {
            LOG.error("DB error during login for {}", username, e);
            return null;
        }
    }

    public static Player loginOrRegisterTokenCharacter(String characterName) {
        if (characterName == null || characterName.isBlank() || characterName.length() > 12) {
            LOG.warn("Token login rejected — invalid character name: {}", characterName);
            return null;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, x, y, " +
                    "attack_xp, strength_xp, defence_xp, hitpoints_xp, ranged_xp, magic_xp " +
                    "FROM osrs.players WHERE LOWER(username) = LOWER(?)"
            );
            ps.setString(1, characterName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Player player = mapPlayerFromRow(rs, characterName);
                int dbId = rs.getInt("id");

                PreparedStatement upd = conn.prepareStatement(
                    "UPDATE osrs.players SET last_login = GETDATE() WHERE id = ?"
                );
                upd.setInt(1, dbId);
                upd.executeUpdate();
                conn.commit();

                LOG.info("Token login successful: {} (dbId={}, entityId={})",
                    characterName, dbId, player.getId());
                return player;
            }

            String placeholderHash = BCrypt.withDefaults().hashToString(
                BCRYPT_COST,
                UUID.randomUUID().toString().toCharArray()
            );
            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO osrs.players (username, password_hash, x, y, hitpoints_xp) VALUES (?, ?, 50, 50, 1154)",
                Statement.RETURN_GENERATED_KEYS
            );
            ins.setString(1, characterName);
            ins.setString(2, placeholderHash);
            ins.executeUpdate();

            ResultSet keys = ins.getGeneratedKeys();
            if (!keys.next()) {
                conn.rollback();
                LOG.error("Token login registration created row but no id for {}", characterName);
                return null;
            }

            int dbId = keys.getInt(1);
            conn.commit();

            int runtimeEntityId = toRuntimeEntityId(dbId);
            LOG.info("Provisioned player row for token-auth character {} (dbId={}, entityId={})",
                characterName, dbId, runtimeEntityId);
            return new Player(runtimeEntityId, characterName, 50, 50);
        } catch (SQLException e) {
            LOG.error("DB error during token login for {}", characterName, e);
            return null;
        }
    }

    public static AuthCharacter findActiveAuthCharacterById(String authSchema, int characterId) {
        if (authSchema == null || !authSchema.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            LOG.error("Invalid auth schema provided to token resolver: {}", authSchema);
            return null;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT c.account_id, c.character_name " +
                    "FROM " + authSchema + ".characters c " +
                    "INNER JOIN " + authSchema + ".accounts a ON a.id = c.account_id " +
                    "WHERE c.id = ? AND c.is_active = 1 AND a.status = 1"
            );
            ps.setInt(1, characterId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            return new AuthCharacter(rs.getInt("account_id"), rs.getString("character_name"));
        } catch (SQLException e) {
            LOG.error("Failed to resolve token-auth character id {} from schema {}", characterId, authSchema, e);
            return null;
        }
    }

    /**
     * Register a new account. Returns the newly created Player, or null if
     * the username is already taken or a DB error occurs.
     *
     * @param username      must be 1-12 characters
     * @param plainPassword plain-text password (will be hashed here)
     * @param assignedId    Netty channel ID
     */
    public static Player register(String username, String plainPassword, int assignedId) {
        if (username == null || username.isEmpty() || username.length() > 12) {
            LOG.warn("Register rejected — invalid username length: {}", username);
            return null;
        }

        String hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, plainPassword.toCharArray());

        try (Connection conn = DatabaseManager.getConnection()) {
            // Check for duplicate username
            PreparedStatement check = conn.prepareStatement(
                "SELECT 1 FROM osrs.players WHERE LOWER(username) = LOWER(?)"
            );
            check.setString(1, username);
            if (check.executeQuery().next()) {
                LOG.info("Register rejected — username taken: {}", username);
                return null;
            }

            // Insert new player at local world spawn (Tutorial Island center)
            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO osrs.players (username, password_hash, x, y, hitpoints_xp) " +
                "VALUES (?, ?, 50, 50, 1154)",
                Statement.RETURN_GENERATED_KEYS
            );
            ins.setString(1, username);
            ins.setString(2, hash);
            ins.executeUpdate();

            ResultSet keys = ins.getGeneratedKeys();
            if (!keys.next()) {
                LOG.error("Registration succeeded but no DB id generated for {}", username);
                conn.rollback();
                return null;
            }
            int dbId = keys.getInt(1);
            conn.commit();

            int runtimeEntityId = toRuntimeEntityId(dbId);
            LOG.info("Registered new account: {} (dbId={}, entityId={})", username, dbId, runtimeEntityId);
            // New player — use defaults (all XP = 0 except HP which we set in constructor)
            return new Player(runtimeEntityId, username, 50, 50);

        } catch (SQLException e) {
            LOG.error("DB error during registration for {}", username, e);
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Save
    // -----------------------------------------------------------------------

    /**
     * Persists position and all skill XP for a player. Called on logout and
     * on the autosave interval (~60 seconds).
     */
    public static void savePlayer(Player player) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE osrs.players SET " +
                "x = ?, y = ?, last_logout = GETDATE(), " +
                "attack_xp = ?, strength_xp = ?, defence_xp = ?, " +
                "hitpoints_xp = ?, ranged_xp = ?, magic_xp = ? " +
                "WHERE LOWER(username) = LOWER(?)"
            );
            ps.setInt   (1, player.getX());
            ps.setInt   (2, player.getY());
            ps.setLong  (3, player.getSkillXp(Player.SKILL_ATTACK));
            ps.setLong  (4, player.getSkillXp(Player.SKILL_STRENGTH));
            ps.setLong  (5, player.getSkillXp(Player.SKILL_DEFENCE));
            ps.setLong  (6, player.getSkillXp(Player.SKILL_HITPOINTS));
            ps.setLong  (7, player.getSkillXp(Player.SKILL_RANGED));
            ps.setLong  (8, player.getSkillXp(Player.SKILL_MAGIC));
            ps.setString(9, player.getName());
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            LOG.error("Failed to save player: {}", player.getName(), e);
        }
    }

    /**
     * Saves the 28-slot inventory for a player. Clears existing rows and
     * re-inserts occupied slots (skip empty slots where itemId == 0).
     */
    public static void saveInventory(Player player) {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Look up DB id by username
            PreparedStatement idPs = conn.prepareStatement(
                "SELECT id FROM osrs.players WHERE LOWER(username) = LOWER(?)"
            );
            idPs.setString(1, player.getName());
            ResultSet rs = idPs.executeQuery();
            if (!rs.next()) return;
            int dbId = rs.getInt("id");

            // Wipe existing inventory rows
            PreparedStatement del = conn.prepareStatement(
                "DELETE FROM osrs.inventory WHERE player_id = ?"
            );
            del.setInt(1, dbId);
            del.executeUpdate();

            // Re-insert occupied slots
            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO osrs.inventory (player_id, slot_index, item_id, quantity) VALUES (?, ?, ?, ?)"
            );
            for (int slot = 0; slot < 28; slot++) {
                int itemId = player.getInventoryItemId(slot);
                if (itemId == 0) continue;
                ins.setInt(1, dbId);
                ins.setInt(2, slot);
                ins.setInt(3, itemId);
                ins.setInt(4, player.getInventoryQuantity(slot));
                ins.addBatch();
            }
            ins.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            LOG.error("Failed to save inventory for: {}", player.getName(), e);
        }
    }

    /**
     * Loads inventory from DB into the player. Called after login.
     */
    public static void loadInventory(Player player) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement idPs = conn.prepareStatement(
                "SELECT id FROM osrs.players WHERE LOWER(username) = LOWER(?)"
            );
            idPs.setString(1, player.getName());
            ResultSet idRs = idPs.executeQuery();
            if (!idRs.next()) return;
            int dbId = idRs.getInt("id");

            PreparedStatement ps = conn.prepareStatement(
                "SELECT slot_index, item_id, quantity FROM osrs.inventory WHERE player_id = ?"
            );
            ps.setInt(1, dbId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                player.setInventoryItem(rs.getInt("slot_index"), rs.getInt("item_id"), rs.getInt("quantity"));
            }
        } catch (SQLException e) {
            LOG.error("Failed to load inventory for: {}", player.getName(), e);
        }
    }

    public static void saveQuestProgress(Player player, QuestManager questManager) {
        if (questManager == null) return;

        try (Connection conn = DatabaseManager.getConnection()) {
            int dbId = getDbIdByUsername(conn, player.getName());
            if (dbId < 0) return;

            PreparedStatement upd = conn.prepareStatement(
                "UPDATE osrs.player_quests SET status = ?, completed_objectives = ?, " +
                "completed_at = CASE WHEN ? = 2 AND completed_at IS NULL THEN GETDATE() ELSE completed_at END " +
                "WHERE player_id = ? AND quest_id = ?"
            );

            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO osrs.player_quests (player_id, quest_id, status, completed_objectives, completed_at) " +
                "VALUES (?, ?, ?, ?, ?)"
            );

            for (Quest quest : questManager.getQuests().values()) {
                int status = questManager.getStatusCode(quest.id);
                int bitmask = questManager.getCompletedObjectivesBitmask(quest.id);

                upd.setInt(1, status);
                upd.setInt(2, bitmask);
                upd.setInt(3, status);
                upd.setInt(4, dbId);
                upd.setInt(5, quest.id);
                int updated = upd.executeUpdate();

                if (updated == 0) {
                    ins.setInt(1, dbId);
                    ins.setInt(2, quest.id);
                    ins.setInt(3, status);
                    ins.setInt(4, bitmask);
                    if (status == 2) {
                        ins.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                    } else {
                        ins.setNull(5, Types.TIMESTAMP);
                    }
                    ins.addBatch();
                }
            }

            ins.executeBatch();

            // Persist exact per-task counts for quantity objectives.
            // This table is optional for older databases; failures should not block core save.
            try {
                PreparedStatement delTasks = conn.prepareStatement(
                    "DELETE FROM osrs.player_quest_tasks WHERE player_id = ?"
                );
                delTasks.setInt(1, dbId);
                delTasks.executeUpdate();

                PreparedStatement insTask = conn.prepareStatement(
                    "INSERT INTO osrs.player_quest_tasks (player_id, quest_id, task_id, progress_count) VALUES (?, ?, ?, ?)"
                );
                for (Quest quest : questManager.getQuests().values()) {
                    for (Quest.Task task : quest.tasks) {
                        int progress = questManager.getTaskProgress(quest.id, task.id);
                        if (progress <= 0) continue;
                        insTask.setInt(1, dbId);
                        insTask.setInt(2, quest.id);
                        insTask.setString(3, task.id);
                        insTask.setInt(4, progress);
                        insTask.addBatch();
                    }
                }
                insTask.executeBatch();
            } catch (SQLException taskSaveErr) {
                LOG.debug("Quest task progress table unavailable; saved coarse quest progress only for {}", player.getName());
            }

            conn.commit();
        } catch (SQLException e) {
            LOG.error("Failed to save quest progress for: {}", player.getName(), e);
        }
    }

    public static void loadQuestProgress(Player player, QuestManager questManager) {
        if (questManager == null) return;

        try (Connection conn = DatabaseManager.getConnection()) {
            int dbId = getDbIdByUsername(conn, player.getName());
            if (dbId < 0) return;

            PreparedStatement ps = conn.prepareStatement(
                "SELECT quest_id, status, completed_objectives FROM osrs.player_quests WHERE player_id = ?"
            );
            ps.setInt(1, dbId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int questId = rs.getInt("quest_id");
                int status = rs.getInt("status");
                int bitmask = rs.getInt("completed_objectives");
                questManager.applyPersistedState(questId, status, bitmask);
            }

            try {
                PreparedStatement taskPs = conn.prepareStatement(
                    "SELECT quest_id, task_id, progress_count FROM osrs.player_quest_tasks WHERE player_id = ?"
                );
                taskPs.setInt(1, dbId);
                ResultSet taskRs = taskPs.executeQuery();
                while (taskRs.next()) {
                    questManager.applyPersistedTaskProgress(
                        taskRs.getInt("quest_id"),
                        taskRs.getString("task_id"),
                        taskRs.getInt("progress_count")
                    );
                }
            } catch (SQLException taskLoadErr) {
                LOG.debug("Quest task progress table unavailable; loaded coarse quest progress only for {}", player.getName());
            }
        } catch (SQLException e) {
            LOG.error("Failed to load quest progress for: {}", player.getName(), e);
        }
    }

    private static int getDbIdByUsername(Connection conn, String username) throws SQLException {
        PreparedStatement idPs = conn.prepareStatement(
            "SELECT id FROM osrs.players WHERE LOWER(username) = LOWER(?)"
        );
        idPs.setString(1, username);
        ResultSet rs = idPs.executeQuery();
        if (!rs.next()) {
            return -1;
        }
        return rs.getInt("id");
    }

    private static int toRuntimeEntityId(int dbId) {
        return PLAYER_ENTITY_ID_OFFSET + dbId;
    }

    private static Player mapPlayerFromRow(ResultSet rs, String username) throws SQLException {
        int dbId = rs.getInt("id");
        int x = rs.getInt("x");
        int y = rs.getInt("y");

        int runtimeEntityId = toRuntimeEntityId(dbId);
        Player player = new Player(runtimeEntityId, username, x, y);
        player.setSkillXp(Player.SKILL_ATTACK, rs.getLong("attack_xp"));
        player.setSkillXp(Player.SKILL_STRENGTH, rs.getLong("strength_xp"));
        player.setSkillXp(Player.SKILL_DEFENCE, rs.getLong("defence_xp"));
        player.setSkillXp(Player.SKILL_HITPOINTS, rs.getLong("hitpoints_xp"));
        player.setSkillXp(Player.SKILL_RANGED, rs.getLong("ranged_xp"));
        player.setSkillXp(Player.SKILL_MAGIC, rs.getLong("magic_xp"));

        int hpLevel = player.getSkillLevel(Player.SKILL_HITPOINTS);
        player.setHealth(hpLevel);
        player.setMaxHealth(hpLevel);
        return player;
    }

    public record AuthCharacter(int accountId, String characterName) {
    }
}
