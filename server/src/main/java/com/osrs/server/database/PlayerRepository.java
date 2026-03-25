package com.osrs.server.database;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.osrs.shared.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Handles all player persistence: create, load, save.
 *
 * All writes use explicit conn.commit() because the pool has autoCommit=false.
 */
public class PlayerRepository {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerRepository.class);

    // BCrypt cost factor — 10 is a reasonable production default (~100ms on modern hardware)
    private static final int BCRYPT_COST = 10;

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
            int dbId  = rs.getInt("id");
            int x     = rs.getInt("x");
            int y     = rs.getInt("y");

            Player player = new Player(assignedId, username, x, y);
            player.setSkillXp(Player.SKILL_ATTACK,    rs.getLong("attack_xp"));
            player.setSkillXp(Player.SKILL_STRENGTH,  rs.getLong("strength_xp"));
            player.setSkillXp(Player.SKILL_DEFENCE,   rs.getLong("defence_xp"));
            player.setSkillXp(Player.SKILL_HITPOINTS, rs.getLong("hitpoints_xp"));
            player.setSkillXp(Player.SKILL_RANGED,    rs.getLong("ranged_xp"));
            player.setSkillXp(Player.SKILL_MAGIC,     rs.getLong("magic_xp"));
            // HP = hitpoints level
            int hpLevel = player.getSkillLevel(Player.SKILL_HITPOINTS);
            player.setHealth(hpLevel);
            player.setMaxHealth(hpLevel);

            // Update last_login timestamp
            PreparedStatement upd = conn.prepareStatement(
                "UPDATE osrs.players SET last_login = GETDATE() WHERE id = ?"
            );
            upd.setInt(1, dbId);
            upd.executeUpdate();
            conn.commit();

            LOG.info("Login successful: {} (dbId={})", username, dbId);
            return player;

        } catch (SQLException e) {
            LOG.error("DB error during login for {}", username, e);
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

            // Insert new player at Tutorial Island spawn (3222, 3218)
            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO osrs.players (username, password_hash, x, y, hitpoints_xp) " +
                "VALUES (?, ?, 3222, 3218, 1154)",
                Statement.RETURN_GENERATED_KEYS
            );
            ins.setString(1, username);
            ins.setString(2, hash);
            ins.executeUpdate();
            conn.commit();

            LOG.info("Registered new account: {}", username);
            // New player — use defaults (all XP = 0 except HP which we set in constructor)
            return new Player(assignedId, username, 3222, 3218);

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
}
