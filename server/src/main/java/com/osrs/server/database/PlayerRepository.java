package com.osrs.server.database;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.osrs.server.config.ServerConfig;
import com.osrs.server.quest.Quest;
import com.osrs.server.quest.QuestManager;
import com.osrs.shared.EquipmentSlot;
import com.osrs.shared.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
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

    /** Returns the fully-qualified table name using the configured DB schema. */
    private static String table(String name) {
        return ServerConfig.get().dbSchema + "." + name;
    }

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
                "SELECT * FROM " + table("players") + " WHERE LOWER(username) = LOWER(?)"
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
                "UPDATE " + table("players") + " SET last_login = GETDATE() WHERE id = ?"
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
                "SELECT * FROM " + table("players") + " WHERE LOWER(username) = LOWER(?)"
            );
            ps.setString(1, characterName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Player player = mapPlayerFromRow(rs, characterName);
                int dbId = rs.getInt("id");

                PreparedStatement upd = conn.prepareStatement(
                    "UPDATE " + table("players") + " SET last_login = GETDATE() WHERE id = ?"
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
                "INSERT INTO " + table("players") + " (username, password_hash, x, y, hitpoints_xp) VALUES (?, ?, 50, 50, 11540)",
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
                "SELECT 1 FROM " + table("players") + " WHERE LOWER(username) = LOWER(?)"
            );
            check.setString(1, username);
            if (check.executeQuery().next()) {
                LOG.info("Register rejected — username taken: {}", username);
                return null;
            }

            // Insert new player at local world spawn (Tutorial Island center)
            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO " + table("players") + " (username, password_hash, x, y, hitpoints_xp) " +
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
    public static boolean savePlayer(Player player) {
        try (Connection conn = DatabaseManager.getConnection()) {
            int updatedRows;
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE " + table("players") + " SET " +
                        "x = ?, y = ?, last_logout = GETDATE(), " +
                        "attack_xp = ?, strength_xp = ?, defence_xp = ?, " +
                        "hitpoints_xp = ?, ranged_xp = ?, magic_xp = ?, " +
                        "prayer_xp = ?, woodcutting_xp = ?, fishing_xp = ?, cooking_xp = ?, " +
                        "mining_xp = ?, smithing_xp = ?, firemaking_xp = ?, crafting_xp = ?, " +
                        "runecrafting_xp = ?, fletching_xp = ?, agility_xp = ?, herblore_xp = ?, " +
                        "thieving_xp = ?, slayer_xp = ?, farming_xp = ?, hunter_xp = ?, construction_xp = ?, " +
                        "prayer_points = ? " +
                        "WHERE LOWER(username) = LOWER(?)"
                );
                ps.setInt(1, player.getX());
                ps.setInt(2, player.getY());
                ps.setLong(3, player.getSkillXp(Player.SKILL_ATTACK));
                ps.setLong(4, player.getSkillXp(Player.SKILL_STRENGTH));
                ps.setLong(5, player.getSkillXp(Player.SKILL_DEFENCE));
                ps.setLong(6, player.getSkillXp(Player.SKILL_HITPOINTS));
                ps.setLong(7, player.getSkillXp(Player.SKILL_RANGED));
                ps.setLong(8, player.getSkillXp(Player.SKILL_MAGIC));
                ps.setLong(9, player.getSkillXp(Player.SKILL_PRAYER));
                ps.setLong(10, player.getSkillXp(Player.SKILL_WOODCUTTING));
                ps.setLong(11, player.getSkillXp(Player.SKILL_FISHING));
                ps.setLong(12, player.getSkillXp(Player.SKILL_COOKING));
                ps.setLong(13, player.getSkillXp(Player.SKILL_MINING));
                ps.setLong(14, player.getSkillXp(Player.SKILL_SMITHING));
                ps.setLong(15, player.getSkillXp(Player.SKILL_FIREMAKING));
                ps.setLong(16, player.getSkillXp(Player.SKILL_CRAFTING));
                ps.setLong(17, player.getSkillXp(Player.SKILL_RUNECRAFTING));
                ps.setLong(18, player.getSkillXp(Player.SKILL_FLETCHING));
                ps.setLong(19, player.getSkillXp(Player.SKILL_AGILITY));
                ps.setLong(20, player.getSkillXp(Player.SKILL_HERBLORE));
                ps.setLong(21, player.getSkillXp(Player.SKILL_THIEVING));
                ps.setLong(22, player.getSkillXp(Player.SKILL_SLAYER));
                ps.setLong(23, player.getSkillXp(Player.SKILL_FARMING));
                ps.setLong(24, player.getSkillXp(Player.SKILL_HUNTER));
                ps.setLong(25, player.getSkillXp(Player.SKILL_CONSTRUCTION));
                ps.setInt(26, Math.max(1, player.getSkillLevel(Player.SKILL_PRAYER)));
                ps.setString(27, player.getName());
                updatedRows = ps.executeUpdate();
            } catch (SQLException extendedErr) {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE " + table("players") + " SET " +
                        "x = ?, y = ?, last_logout = GETDATE(), " +
                        "attack_xp = ?, strength_xp = ?, defence_xp = ?, " +
                        "hitpoints_xp = ?, ranged_xp = ?, magic_xp = ? " +
                        "WHERE LOWER(username) = LOWER(?)"
                );
                ps.setInt(1, player.getX());
                ps.setInt(2, player.getY());
                ps.setLong(3, player.getSkillXp(Player.SKILL_ATTACK));
                ps.setLong(4, player.getSkillXp(Player.SKILL_STRENGTH));
                ps.setLong(5, player.getSkillXp(Player.SKILL_DEFENCE));
                ps.setLong(6, player.getSkillXp(Player.SKILL_HITPOINTS));
                ps.setLong(7, player.getSkillXp(Player.SKILL_RANGED));
                ps.setLong(8, player.getSkillXp(Player.SKILL_MAGIC));
                ps.setString(9, player.getName());
                updatedRows = ps.executeUpdate();
                LOG.debug("Extended skill columns unavailable; saved legacy skills for {}", player.getName());
            }
            if (updatedRows <= 0) {
                conn.rollback();
                LOG.warn("Player save affected no rows for {}", player.getName());
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            LOG.error("Failed to save player: {}", player.getName(), e);
            return false;
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
                "SELECT id FROM " + table("players") + " WHERE LOWER(username) = LOWER(?)"
            );
            idPs.setString(1, player.getName());
            ResultSet rs = idPs.executeQuery();
            if (!rs.next()) return;
            int dbId = rs.getInt("id");

            // Wipe existing inventory rows
            PreparedStatement del = conn.prepareStatement(
                "DELETE FROM " + table("inventory") + " WHERE player_id = ?"
            );
            del.setInt(1, dbId);
            del.executeUpdate();

            // Re-insert occupied slots
            PreparedStatement ensureItem = conn.prepareStatement(
                "IF NOT EXISTS (SELECT 1 FROM " + table("items") + " WHERE id = ?) " +
                    "INSERT INTO " + table("items") + " (id, name, examine_text, stackable, weight_kg, tradeable, high_alchemy_value, low_alchemy_value) " +
                    "VALUES (?, ?, ?, ?, 0, 1, 0, 0)"
            );
            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO " + table("inventory") + " (player_id, slot_index, item_id, quantity) VALUES (?, ?, ?, ?)"
            );
            for (int slot = 0; slot < 28; slot++) {
                int itemId = player.getInventoryItemId(slot);
                if (itemId == 0) continue;

                ensureItem.setInt(1, itemId);
                ensureItem.setInt(2, itemId);
                ensureItem.setString(3, defaultItemName(itemId));
                ensureItem.setString(4, defaultItemExamine(itemId));
                ensureItem.setBoolean(5, defaultStackable(itemId));
                ensureItem.addBatch();

                ins.setInt(1, dbId);
                ins.setInt(2, slot);
                ins.setInt(3, itemId);
                ins.setInt(4, player.getInventoryQuantity(slot));
                ins.addBatch();
            }
            ensureItem.executeBatch();
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
                "SELECT id FROM " + table("players") + " WHERE LOWER(username) = LOWER(?)"
            );
            idPs.setString(1, player.getName());
            ResultSet idRs = idPs.executeQuery();
            if (!idRs.next()) return;
            int dbId = idRs.getInt("id");

            PreparedStatement ps = conn.prepareStatement(
                "SELECT slot_index, item_id, quantity FROM " + table("inventory") + " WHERE player_id = ?"
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

    public static void loadBank(Player player) {
        if (!DatabaseManager.isHealthy()) return;
        try (Connection conn = DatabaseManager.getConnection()) {
            int dbId = getDbIdByUsername(conn, player.getName());
            if (dbId < 0) return;

            player.clearBank();
            List<Player.BankSlot> slots = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT tab_index, slot_index, item_id, quantity, placeholder " +
                    "FROM " + table("player_bank_items") + " WHERE player_id = ? " +
                    "ORDER BY slot_index"
            )) {
                ps.setInt(1, dbId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        slots.add(new Player.BankSlot(
                            rs.getInt("slot_index"),
                            rs.getInt("tab_index"),
                            rs.getInt("item_id"),
                            rs.getLong("quantity"),
                            rs.getBoolean("placeholder")
                        ));
                    }
                }
            } catch (SQLException e) {
                if (isMissingTableError(e)) {
                    LOG.debug("player_bank_items table unavailable; loading empty bank for {}", player.getName());
                } else {
                    LOG.error("Failed to load bank for {}", player.getName(), e);
                }
                return;
            }
            player.setBankSlots(slots);
        } catch (SQLException e) {
            LOG.error("Failed to load bank for: {}", player.getName(), e);
        }
    }

    public static void saveBank(Player player) {
        if (!DatabaseManager.isHealthy()) return;
        try (Connection conn = DatabaseManager.getConnection()) {
            int dbId = getDbIdByUsername(conn, player.getName());
            if (dbId < 0) return;

            try (PreparedStatement del = conn.prepareStatement(
                "DELETE FROM " + table("player_bank_items") + " WHERE player_id = ?"
            )) {
                del.setInt(1, dbId);
                del.executeUpdate();
            } catch (SQLException e) {
                if (isMissingTableError(e)) {
                    LOG.debug("player_bank_items table unavailable; skipping bank save for {}", player.getName());
                    return;
                }
                throw e;
            }

            try (PreparedStatement ensureItem = conn.prepareStatement(
                    "IF NOT EXISTS (SELECT 1 FROM " + table("items") + " WHERE id = ?) " +
                        "INSERT INTO " + table("items") + " (id, name, examine_text, stackable, weight_kg, tradeable, high_alchemy_value, low_alchemy_value) " +
                        "VALUES (?, ?, ?, ?, 0, 1, 0, 0)"
                );
                 PreparedStatement ins = conn.prepareStatement(
                     "INSERT INTO " + table("player_bank_items") +
                         " (player_id, tab_index, slot_index, item_id, quantity, placeholder) " +
                         "VALUES (?, ?, ?, ?, ?, ?)"
                 )) {
                for (Player.BankSlot slot : player.getBankSlots()) {
                    if (slot == null || slot.getItemId() <= 0 || slot.getQuantity() <= 0) {
                        continue;
                    }
                    ensureItem.setInt(1, slot.getItemId());
                    ensureItem.setInt(2, slot.getItemId());
                    ensureItem.setString(3, defaultItemName(slot.getItemId()));
                    ensureItem.setString(4, defaultItemExamine(slot.getItemId()));
                    ensureItem.setBoolean(5, defaultStackable(slot.getItemId()));
                    ensureItem.addBatch();

                    ins.setInt(1, dbId);
                    ins.setInt(2, slot.getTabIndex());
                    ins.setInt(3, slot.getSlotIndex());
                    ins.setInt(4, slot.getItemId());
                    ins.setLong(5, slot.getQuantity());
                    ins.setBoolean(6, slot.isPlaceholder());
                    ins.addBatch();
                }
                ensureItem.executeBatch();
                ins.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            LOG.error("Failed to save bank for: {}", player.getName(), e);
        }
    }

    public static boolean saveInventoryBankAtomic(Player player) {
        if (!DatabaseManager.isHealthy()) return false;
        try (Connection conn = DatabaseManager.getConnection()) {
            int dbId = getDbIdByUsername(conn, player.getName());
            if (dbId < 0) return false;
            PreparedStatement ensureItem = conn.prepareStatement(
                "IF NOT EXISTS (SELECT 1 FROM " + table("items") + " WHERE id = ?) " +
                    "INSERT INTO " + table("items") + " (id, name, examine_text, stackable, weight_kg, tradeable, high_alchemy_value, low_alchemy_value) " +
                    "VALUES (?, ?, ?, ?, 0, 1, 0, 0)"
            );
            PreparedStatement delInv = conn.prepareStatement(
                "DELETE FROM " + table("inventory") + " WHERE player_id = ?"
            );
            delInv.setInt(1, dbId);
            delInv.executeUpdate();
            PreparedStatement insInv = conn.prepareStatement(
                "INSERT INTO " + table("inventory") + " (player_id, slot_index, item_id, quantity) VALUES (?, ?, ?, ?)"
            );
            for (int slot = 0; slot < 28; slot++) {
                int itemId = player.getInventoryItemId(slot);
                if (itemId == 0) continue;
                ensureItem.setInt(1, itemId);
                ensureItem.setInt(2, itemId);
                ensureItem.setString(3, defaultItemName(itemId));
                ensureItem.setString(4, defaultItemExamine(itemId));
                ensureItem.setBoolean(5, defaultStackable(itemId));
                ensureItem.addBatch();
                insInv.setInt(1, dbId);
                insInv.setInt(2, slot);
                insInv.setInt(3, itemId);
                insInv.setInt(4, player.getInventoryQuantity(slot));
                insInv.addBatch();
            }
            PreparedStatement delBank = conn.prepareStatement(
                "DELETE FROM " + table("player_bank_items") + " WHERE player_id = ?"
            );
            delBank.setInt(1, dbId);
            delBank.executeUpdate();
            PreparedStatement insBank = conn.prepareStatement(
                "INSERT INTO " + table("player_bank_items") +
                    " (player_id, tab_index, slot_index, item_id, quantity, placeholder) " +
                    "VALUES (?, ?, ?, ?, ?, ?)"
            );
            for (Player.BankSlot slot : player.getBankSlots()) {
                if (slot == null || slot.getItemId() <= 0 || slot.getQuantity() <= 0) continue;
                ensureItem.setInt(1, slot.getItemId());
                ensureItem.setInt(2, slot.getItemId());
                ensureItem.setString(3, defaultItemName(slot.getItemId()));
                ensureItem.setString(4, defaultItemExamine(slot.getItemId()));
                ensureItem.setBoolean(5, defaultStackable(slot.getItemId()));
                ensureItem.addBatch();
                insBank.setInt(1, dbId);
                insBank.setInt(2, slot.getTabIndex());
                insBank.setInt(3, slot.getSlotIndex());
                insBank.setInt(4, slot.getItemId());
                insBank.setLong(5, slot.getQuantity());
                insBank.setBoolean(6, slot.isPlaceholder());
                insBank.addBatch();
            }
            ensureItem.executeBatch();
            insInv.executeBatch();
            insBank.executeBatch();
            conn.commit();
            return true;
        } catch (SQLException e) {
            LOG.error("Failed to atomically save inventory+bank for: {}", player.getName(), e);
            return false;
        }
    }

    public static void saveFriends(Player player) {
        if (!DatabaseManager.isHealthy()) return;
        try (Connection conn = DatabaseManager.getConnection()) {
            int dbId = getDbIdByUsername(conn, player.getName());
            if (dbId < 0) return;

            try {
                PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM " + table("player_friends") + " WHERE player_id = ?"
                );
                del.setInt(1, dbId);
                del.executeUpdate();

                PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO " + table("player_friends") + " (player_id, friend_player_id, blocked) VALUES (?, ?, ?)"
                );
                for (Long friendId : player.getFriends()) {
                    ins.setInt(1, dbId);
                    ins.setLong(2, friendId);
                    ins.setBoolean(3, false);
                    ins.addBatch();
                }
                for (Long blockedId : player.getBlockedPlayers()) {
                    ins.setInt(1, dbId);
                    ins.setLong(2, blockedId);
                    ins.setBoolean(3, true);
                    ins.addBatch();
                }
                ins.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                if (isMissingTableError(e)) {
                    LOG.debug("player_friends table unavailable; skipping friend persistence for {}", player.getName());
                } else {
                    conn.rollback();
                    LOG.error("Failed to save friends for {}", player.getName(), e);
                }
            }
        } catch (SQLException e) {
            LOG.error("Failed to save friends for: {}", player.getName(), e);
        }
    }

    public static void loadFriends(Player player) {
        if (!DatabaseManager.isHealthy()) return;
        try (Connection conn = DatabaseManager.getConnection()) {
            int dbId = getDbIdByUsername(conn, player.getName());
            if (dbId < 0) return;

            player.clearFriends();
            for (Long blocked : new ArrayList<>(player.getBlockedPlayers())) {
                player.removeFromBlock(blocked);
            }

            try {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT friend_player_id, blocked FROM " + table("player_friends") + " WHERE player_id = ?"
                );
                ps.setInt(1, dbId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    long friendId = rs.getLong("friend_player_id");
                    boolean blocked = rs.getBoolean("blocked");
                    if (blocked) player.blockPlayer(friendId);
                    else player.addFriend(friendId);
                }
            } catch (SQLException e) {
                if (isMissingTableError(e)) {
                    LOG.debug("player_friends table unavailable; loading no persisted friends for {}", player.getName());
                } else {
                    LOG.error("Failed to load friends for {}", player.getName(), e);
                }
            }
        } catch (SQLException e) {
            LOG.error("Failed to load friends for: {}", player.getName(), e);
        }
    }

    public static int findDbIdByUsername(String username) {
        if (username == null || username.isBlank() || !DatabaseManager.isHealthy()) return -1;
        try (Connection conn = DatabaseManager.getConnection()) {
            return getDbIdByUsername(conn, username);
        } catch (SQLException e) {
            LOG.error("Failed to resolve DB id by username: {}", username, e);
            return -1;
        }
    }

    public static String findUsernameByDbId(int dbId) {
        if (dbId <= 0 || !DatabaseManager.isHealthy()) return "";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT username FROM " + table("players") + " WHERE id = ?")) {
            ps.setInt(1, dbId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return "";
                String username = rs.getString("username");
                return username == null ? "" : username;
            }
        } catch (SQLException e) {
            LOG.error("Failed to resolve username for dbId={}", dbId, e);
            return "";
        }
    }

    public static boolean playerExistsByDbId(int dbId) {
        if (dbId <= 0 || !DatabaseManager.isHealthy()) return false;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT 1 FROM " + table("players") + " WHERE id = ?")) {
            ps.setInt(1, dbId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOG.error("Failed to check player existence for dbId={}", dbId, e);
            return false;
        }
    }

    public static int tryMapEntityIdToDbId(int entityId) {
        int dbId = entityId - PLAYER_ENTITY_ID_OFFSET;
        return dbId > 0 ? dbId : -1;
    }

    public static void saveEquipment(Player player) {
        if (!DatabaseManager.isHealthy()) return;
        String sql = "UPDATE " + table("players") + " SET " +
            "equip_head=?, equip_cape=?, equip_neck=?, equip_ammo=?, equip_weapon=?," +
            "equip_shield=?, equip_body=?, equip_legs=?, equip_hands=?, equip_feet=?, equip_ring=?," +
            "ammo_quantity=?" +
            " WHERE id=?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int dbId = getDbIdByUsername(con, player.getName());
            if (dbId < 0) return;
            ps.setInt(1,  player.getEquipment(EquipmentSlot.HEAD));
            ps.setInt(2,  player.getEquipment(EquipmentSlot.CAPE));
            ps.setInt(3,  player.getEquipment(EquipmentSlot.NECK));
            ps.setInt(4,  player.getEquipment(EquipmentSlot.AMMO));
            ps.setInt(5,  player.getEquipment(EquipmentSlot.WEAPON));
            ps.setInt(6,  player.getEquipment(EquipmentSlot.SHIELD));
            ps.setInt(7,  player.getEquipment(EquipmentSlot.BODY));
            ps.setInt(8,  player.getEquipment(EquipmentSlot.LEGS));
            ps.setInt(9,  player.getEquipment(EquipmentSlot.HANDS));
            ps.setInt(10, player.getEquipment(EquipmentSlot.FEET));
            ps.setInt(11, player.getEquipment(EquipmentSlot.RING));
            ps.setInt(12, player.getAmmoQuantity());
            ps.setInt(13, dbId);
            ps.executeUpdate();
            con.commit();
        } catch (Exception e) {
            LOG.error("Failed to save equipment for player {}", player.getId(), e);
        }
    }

    public static void loadEquipment(Player player, ResultSet rs) throws SQLException {
        player.setEquipment(EquipmentSlot.HEAD,   safeGetInt(rs, "equip_head",   0));
        player.setEquipment(EquipmentSlot.CAPE,   safeGetInt(rs, "equip_cape",   0));
        player.setEquipment(EquipmentSlot.NECK,   safeGetInt(rs, "equip_neck",   0));
        player.setEquipment(EquipmentSlot.WEAPON, safeGetInt(rs, "equip_weapon", 0));
        player.setEquipment(EquipmentSlot.SHIELD, safeGetInt(rs, "equip_shield", 0));
        player.setEquipment(EquipmentSlot.BODY,   safeGetInt(rs, "equip_body",   0));
        player.setEquipment(EquipmentSlot.LEGS,   safeGetInt(rs, "equip_legs",   0));
        player.setEquipment(EquipmentSlot.HANDS,  safeGetInt(rs, "equip_hands",  0));
        player.setEquipment(EquipmentSlot.FEET,   safeGetInt(rs, "equip_feet",   0));
        player.setEquipment(EquipmentSlot.RING,   safeGetInt(rs, "equip_ring",   0));
        // Ammo slot: only restore if quantity is also known; if qty=0 with an item ID
        // the data is from before ammo_quantity was tracked — clear the slot to avoid
        // the "run out of ammo immediately" bug from an inconsistent zero-qty equipped stack.
        int ammoId  = safeGetInt(rs, "equip_ammo",    0);
        int ammoQty = safeGetInt(rs, "ammo_quantity",  0);
        if (ammoId > 0 && ammoQty > 0) {
            player.setEquipment(EquipmentSlot.AMMO, ammoId);
            player.setAmmoQuantity(ammoQty);
        } else {
            player.setEquipment(EquipmentSlot.AMMO, 0);
            player.setAmmoQuantity(0);
        }
    }

    public static void saveQuestProgress(Player player, QuestManager questManager) {
        if (questManager == null) return;

        try (Connection conn = DatabaseManager.getConnection()) {
            int dbId = getDbIdByUsername(conn, player.getName());
            if (dbId < 0) return;

            PreparedStatement upd = conn.prepareStatement(
                "UPDATE " + table("player_quests") + " SET status = ?, completed_objectives = ?, " +
                "completed_at = CASE WHEN ? = 2 AND completed_at IS NULL THEN GETDATE() ELSE completed_at END " +
                "WHERE player_id = ? AND quest_id = ?"
            );

            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO " + table("player_quests") + " (player_id, quest_id, status, completed_objectives, completed_at) " +
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
                    "DELETE FROM " + table("player_quest_tasks") + " WHERE player_id = ?"
                );
                delTasks.setInt(1, dbId);
                delTasks.executeUpdate();

                PreparedStatement insTask = conn.prepareStatement(
                    "INSERT INTO " + table("player_quest_tasks") + " (player_id, quest_id, task_id, progress_count) VALUES (?, ?, ?, ?)"
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
                "SELECT quest_id, status, completed_objectives FROM " + table("player_quests") + " WHERE player_id = ?"
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
                    "SELECT quest_id, task_id, progress_count FROM " + table("player_quest_tasks") + " WHERE player_id = ?"
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
            "SELECT id FROM " + table("players") + " WHERE LOWER(username) = LOWER(?)"
        );
        idPs.setString(1, username);
        ResultSet rs = idPs.executeQuery();
        if (!rs.next()) {
            return -1;
        }
        return rs.getInt("id");
    }

    private static boolean isMissingTableError(SQLException e) {
        String msg = e.getMessage();
        return e.getErrorCode() == 208 || (msg != null && msg.contains("Invalid object name"));
    }

    private static int toRuntimeEntityId(int dbId) {
        return PLAYER_ENTITY_ID_OFFSET + dbId;
    }

    public static void auditAdminAction(Player player, String actionType, String details) {
        if (player == null || actionType == null || actionType.isBlank()) return;
        if (!DatabaseManager.isHealthy()) return;
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement idPs = conn.prepareStatement(
                "SELECT id FROM " + table("players") + " WHERE LOWER(username) = LOWER(?)"
            );
            idPs.setString(1, player.getName());
            ResultSet rs = idPs.executeQuery();
            if (!rs.next()) return;
            int dbId = rs.getInt("id");
            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO " + table("admin_action_audit") + " (player_id, action_type, details) VALUES (?, ?, ?)"
            );
            ins.setInt(1, dbId);
            ins.setString(2, actionType);
            ins.setString(3, details);
            ins.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            LOG.error("Failed to audit admin action {} for {}", actionType, player.getName(), e);
        }
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
        player.setSkillXp(Player.SKILL_PRAYER, safeGetLong(rs, "prayer_xp", 0L));
        player.setSkillXp(Player.SKILL_WOODCUTTING, safeGetLong(rs, "woodcutting_xp", 0L));
        player.setSkillXp(Player.SKILL_FISHING, safeGetLong(rs, "fishing_xp", 0L));
        player.setSkillXp(Player.SKILL_COOKING, safeGetLong(rs, "cooking_xp", 0L));
        player.setSkillXp(Player.SKILL_MINING, safeGetLong(rs, "mining_xp", 0L));
        player.setSkillXp(Player.SKILL_SMITHING, safeGetLong(rs, "smithing_xp", 0L));
        player.setSkillXp(Player.SKILL_FIREMAKING, safeGetLong(rs, "firemaking_xp", 0L));
        player.setSkillXp(Player.SKILL_CRAFTING, safeGetLong(rs, "crafting_xp", 0L));
        player.setSkillXp(Player.SKILL_RUNECRAFTING, safeGetLong(rs, "runecrafting_xp", 0L));
        player.setSkillXp(Player.SKILL_FLETCHING, safeGetLong(rs, "fletching_xp", 0L));
        player.setSkillXp(Player.SKILL_AGILITY, safeGetLong(rs, "agility_xp", 0L));
        player.setSkillXp(Player.SKILL_HERBLORE, safeGetLong(rs, "herblore_xp", 0L));
        player.setSkillXp(Player.SKILL_THIEVING, safeGetLong(rs, "thieving_xp", 0L));
        player.setSkillXp(Player.SKILL_SLAYER, safeGetLong(rs, "slayer_xp", 0L));
        player.setSkillXp(Player.SKILL_FARMING, safeGetLong(rs, "farming_xp", 0L));
        player.setSkillXp(Player.SKILL_HUNTER, safeGetLong(rs, "hunter_xp", 0L));
        player.setSkillXp(Player.SKILL_CONSTRUCTION, safeGetLong(rs, "construction_xp", 0L));
        loadEquipment(player, rs);
        boolean isMember = false;
        try { isMember = rs.getBoolean("is_member"); } catch (SQLException ignored) {}
        player.setMember(isMember);
        boolean adminToolsEnabled = false;
        try { adminToolsEnabled = rs.getBoolean("admin_tools_enabled"); } catch (SQLException ignored) {}
        player.setAdminToolsEnabled(adminToolsEnabled);

        int hpLevel = player.getSkillLevel(Player.SKILL_HITPOINTS);
        player.setHealth(hpLevel);
        player.setMaxHealth(hpLevel);
        return player;
    }

    private static long safeGetLong(ResultSet rs, String column, long defaultValue) {
        try {
            return rs.getLong(column);
        } catch (SQLException ignored) {
            return defaultValue;
        }
    }

    private static int safeGetInt(ResultSet rs, String col, int def) {
        try { return rs.getInt(col); } catch (SQLException e) { return def; }
    }

    private static String defaultItemName(int itemId) {
        return switch (itemId) {
            case 1351 -> "Bronze axe";
            case 1511 -> "Logs";
            case 1521 -> "Oak logs";
            case 1522 -> "Willow logs";
            case 1523 -> "Maple logs";
            case 1524 -> "Yew logs";
            case 1525 -> "Magic logs";
            case 303 -> "Small fishing net";
            case 317 -> "Raw shrimps";
            case 315 -> "Shrimps";
            case 7954 -> "Burnt shrimps";
            case 526 -> "Bones";
            case 2134 -> "Raw rat meat";
            default -> "Item " + itemId;
        };
    }

    private static String defaultItemExamine(int itemId) {
        return switch (itemId) {
            case 1351 -> "A woodcutting axe made of bronze.";
            case 1511 -> "A set of logs.";
            case 1521 -> "Logs from a sturdy oak tree.";
            case 1522 -> "Logs from a flexible willow tree.";
            case 1523 -> "Logs from a vibrant maple tree.";
            case 1524 -> "Logs from an ancient yew tree.";
            case 1525 -> "Logs from a mystical magic tree.";
            case 303 -> "Useful for catching shrimp.";
            case 317 -> "I should cook these first.";
            case 315 -> "A nicely cooked shrimp.";
            case 7954 -> "Oops!";
            case 526 -> "These are bones.";
            case 2134 -> "Raw rat meat.";
            default -> "An item.";
        };
    }

    private static boolean defaultStackable(int itemId) {
        return itemId == 526;
    }

    public record AuthCharacter(int accountId, String characterName) {
    }
}
