-- Migration 002: Add auth/account core tables for Erynfall auth service.
-- IMPORTANT: This migration is non-destructive and safe for existing worlds.

IF OBJECT_ID(N'osrs.accounts', N'U') IS NULL
BEGIN
    CREATE TABLE osrs.accounts (
      id INT IDENTITY(1,1) PRIMARY KEY,
      email VARCHAR(320) NOT NULL,
      password_hash VARCHAR(255) NOT NULL,
      email_verified BIT NOT NULL DEFAULT 0,
      status INT NOT NULL DEFAULT 1,
      created_at DATETIME2 DEFAULT GETDATE(),
      last_login_at DATETIME2,
      UNIQUE (email)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_accounts_status' AND object_id = OBJECT_ID('osrs.accounts'))
BEGIN
    CREATE INDEX idx_accounts_status ON osrs.accounts(status);
END
GO

IF OBJECT_ID(N'osrs.characters', N'U') IS NULL
BEGIN
    CREATE TABLE osrs.characters (
      id INT IDENTITY(1,1) PRIMARY KEY,
      account_id INT NOT NULL REFERENCES osrs.accounts(id) ON DELETE CASCADE,
      character_name VARCHAR(12) NOT NULL,
      is_active BIT NOT NULL DEFAULT 1,
      created_at DATETIME2 DEFAULT GETDATE(),
      UNIQUE (character_name)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_characters_account' AND object_id = OBJECT_ID('osrs.characters'))
BEGIN
    CREATE INDEX idx_characters_account ON osrs.characters(account_id, is_active);
END
GO

IF OBJECT_ID(N'osrs.auth_refresh_tokens', N'U') IS NULL
BEGIN
    CREATE TABLE osrs.auth_refresh_tokens (
      id INT IDENTITY(1,1) PRIMARY KEY,
      account_id INT NOT NULL REFERENCES osrs.accounts(id) ON DELETE CASCADE,
      token_hash VARCHAR(255) NOT NULL,
      expires_at DATETIME2 NOT NULL,
      revoked_at DATETIME2,
      created_at DATETIME2 DEFAULT GETDATE(),
      UNIQUE (token_hash)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_auth_refresh_lookup' AND object_id = OBJECT_ID('osrs.auth_refresh_tokens'))
BEGIN
    CREATE INDEX idx_auth_refresh_lookup ON osrs.auth_refresh_tokens(token_hash, revoked_at, expires_at);
END
GO

IF OBJECT_ID(N'osrs.account_recovery_tokens', N'U') IS NULL
BEGIN
    CREATE TABLE osrs.account_recovery_tokens (
      id INT IDENTITY(1,1) PRIMARY KEY,
      account_id INT NOT NULL REFERENCES osrs.accounts(id) ON DELETE CASCADE,
      purpose VARCHAR(32) NOT NULL,
      token_hash VARCHAR(255) NOT NULL,
      expires_at DATETIME2 NOT NULL,
      used_at DATETIME2,
      created_at DATETIME2 DEFAULT GETDATE(),
      UNIQUE (token_hash)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_recovery_lookup' AND object_id = OBJECT_ID('osrs.account_recovery_tokens'))
BEGIN
    CREATE INDEX idx_recovery_lookup ON osrs.account_recovery_tokens(token_hash, used_at, expires_at);
END
GO

IF OBJECT_ID(N'osrs.auth_audit', N'U') IS NULL
BEGIN
    CREATE TABLE osrs.auth_audit (
      id INT IDENTITY(1,1) PRIMARY KEY,
      account_id INT NULL REFERENCES osrs.accounts(id) ON DELETE SET NULL,
      event_type VARCHAR(64) NOT NULL,
      success BIT NOT NULL,
      ip_address VARCHAR(64),
      user_agent VARCHAR(512),
      details VARCHAR(1024),
      created_at DATETIME2 DEFAULT GETDATE()
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_auth_audit_account' AND object_id = OBJECT_ID('osrs.auth_audit'))
BEGIN
    CREATE INDEX idx_auth_audit_account ON osrs.auth_audit(account_id, created_at);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_auth_audit_type' AND object_id = OBJECT_ID('osrs.auth_audit'))
BEGIN
    CREATE INDEX idx_auth_audit_type ON osrs.auth_audit(event_type, created_at);
END
GO
