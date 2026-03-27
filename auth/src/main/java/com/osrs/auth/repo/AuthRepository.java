package com.osrs.auth.repo;

import com.osrs.auth.config.AuthSettings;
import com.osrs.auth.db.Db;
import com.osrs.auth.model.AccountRecord;
import com.osrs.auth.model.CharacterRecord;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class AuthRepository {

    private final Db db;
    private final String schema;

    public AuthRepository(Db db, AuthSettings settings) {
        this.db = db;
        this.schema = settings.dbSchema();
    }

    public AccountRecord findAccountByEmail(String email) throws Exception {
        try (Connection conn = db.open()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, email, password_hash, email_verified, status FROM " + table("accounts") + " WHERE LOWER(email)=LOWER(?)"
            );
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return new AccountRecord(
                rs.getInt("id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getBoolean("email_verified"),
                rs.getInt("status")
            );
        }
    }

    public AccountRecord findAccountById(int id) throws Exception {
        try (Connection conn = db.open()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, email, password_hash, email_verified, status FROM " + table("accounts") + " WHERE id=?"
            );
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return new AccountRecord(
                rs.getInt("id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getBoolean("email_verified"),
                rs.getInt("status")
            );
        }
    }

    public AccountRecord createAccount(String email, String passwordHash, boolean emailVerified) throws Exception {
        try (Connection conn = db.open()) {
            conn.setAutoCommit(false);

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO " + table("accounts") + " (email, password_hash, email_verified, status) VALUES (?, ?, ?, 1)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            ps.setBoolean(3, emailVerified);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (!keys.next()) {
                conn.rollback();
                throw new IllegalStateException("No account id returned");
            }
            int id = keys.getInt(1);
            conn.commit();

            return new AccountRecord(id, email, passwordHash, emailVerified, 1);
        }
    }

    public RegistrationRecord createAccountAndCharacter(String email, String passwordHash,
                                                        boolean emailVerified, String characterName) throws Exception {
        try (Connection conn = db.open()) {
            conn.setAutoCommit(false);

            int accountId;
            PreparedStatement accountPs = conn.prepareStatement(
                "INSERT INTO " + table("accounts") + " (email, password_hash, email_verified, status) VALUES (?, ?, ?, 1)",
                Statement.RETURN_GENERATED_KEYS
            );
            accountPs.setString(1, email);
            accountPs.setString(2, passwordHash);
            accountPs.setBoolean(3, emailVerified);
            accountPs.executeUpdate();
            ResultSet accountKeys = accountPs.getGeneratedKeys();
            if (!accountKeys.next()) {
                conn.rollback();
                throw new IllegalStateException("No account id returned");
            }
            accountId = accountKeys.getInt(1);

            PreparedStatement characterPs = conn.prepareStatement(
                "INSERT INTO " + table("characters") + " (account_id, character_name, is_active) VALUES (?, ?, 1)",
                Statement.RETURN_GENERATED_KEYS
            );
            characterPs.setInt(1, accountId);
            characterPs.setString(2, characterName);
            characterPs.executeUpdate();
            ResultSet characterKeys = characterPs.getGeneratedKeys();
            if (!characterKeys.next()) {
                conn.rollback();
                throw new IllegalStateException("No character id returned");
            }
            int characterId = characterKeys.getInt(1);

            conn.commit();
            return new RegistrationRecord(
                new AccountRecord(accountId, email, passwordHash, emailVerified, 1),
                new CharacterRecord(characterId, accountId, characterName, true)
            );
        }
    }

    public CharacterRecord createCharacter(int accountId, String characterName) throws Exception {
        try (Connection conn = db.open()) {
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO " + table("characters") + " (account_id, character_name, is_active) VALUES (?, ?, 1)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, accountId);
            ps.setString(2, characterName);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (!keys.next()) {
                conn.rollback();
                throw new IllegalStateException("No character id returned");
            }
            int id = keys.getInt(1);
            conn.commit();
            return new CharacterRecord(id, accountId, characterName, true);
        }
    }

    public CharacterRecord getActiveCharacter(int accountId) throws Exception {
        try (Connection conn = db.open()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT TOP 1 id, account_id, character_name, is_active " +
                    "FROM " + table("characters") + " WHERE account_id = ? AND is_active = 1 ORDER BY id"
            );
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return new CharacterRecord(
                rs.getInt("id"),
                rs.getInt("account_id"),
                rs.getString("character_name"),
                rs.getBoolean("is_active")
            );
        }
    }

    public void storeRefreshToken(int accountId, String tokenHash, Instant expiresAt) throws Exception {
        try (Connection conn = db.open()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO " + table("auth_refresh_tokens") + " (account_id, token_hash, expires_at) VALUES (?, ?, ?)"
            );
            ps.setInt(1, accountId);
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, Timestamp.from(expiresAt));
            ps.executeUpdate();
        }
    }

    public Integer findRefreshTokenAccount(String tokenHash) throws Exception {
        try (Connection conn = db.open()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT account_id FROM " + table("auth_refresh_tokens") + " " +
                    "WHERE token_hash = ? AND revoked_at IS NULL AND expires_at > GETDATE()"
            );
            ps.setString(1, tokenHash);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return rs.getInt("account_id");
        }
    }

    public void revokeRefreshToken(String tokenHash) throws Exception {
        try (Connection conn = db.open()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE " + table("auth_refresh_tokens") + " SET revoked_at = GETDATE() WHERE token_hash = ? AND revoked_at IS NULL"
            );
            ps.setString(1, tokenHash);
            ps.executeUpdate();
        }
    }

    public void revokeAllRefreshTokens(int accountId) throws Exception {
        try (Connection conn = db.open()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE " + table("auth_refresh_tokens") + " SET revoked_at = GETDATE() " +
                    "WHERE account_id = ? AND revoked_at IS NULL"
            );
            ps.setInt(1, accountId);
            ps.executeUpdate();
        }
    }

    public void touchLastLogin(int accountId) throws Exception {
        try (Connection conn = db.open()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE " + table("accounts") + " SET last_login_at = GETDATE() WHERE id = ?"
            );
            ps.setInt(1, accountId);
            ps.executeUpdate();
        }
    }

    private String table(String tableName) {
        return schema + "." + tableName;
    }

    public record RegistrationRecord(AccountRecord account, CharacterRecord character) {
    }
}
