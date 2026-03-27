-- Migration 003: Introduce erynfall schema for auth tables.
-- This keeps existing osrs.* tables untouched while enabling schema migration.

IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'erynfall')
BEGIN
  EXEC sp_executesql N'CREATE SCHEMA erynfall';
END
GO

IF OBJECT_ID(N'erynfall.accounts', N'U') IS NULL
BEGIN
    CREATE TABLE erynfall.accounts (
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

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_erynfall_accounts_status' AND object_id = OBJECT_ID('erynfall.accounts'))
BEGIN
    CREATE INDEX idx_erynfall_accounts_status ON erynfall.accounts(status);
END
GO

IF OBJECT_ID(N'erynfall.characters', N'U') IS NULL
BEGIN
    CREATE TABLE erynfall.characters (
      id INT IDENTITY(1,1) PRIMARY KEY,
      account_id INT NOT NULL REFERENCES erynfall.accounts(id) ON DELETE CASCADE,
      character_name VARCHAR(12) NOT NULL,
      is_active BIT NOT NULL DEFAULT 1,
      created_at DATETIME2 DEFAULT GETDATE(),
      UNIQUE (character_name)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_erynfall_characters_account' AND object_id = OBJECT_ID('erynfall.characters'))
BEGIN
    CREATE INDEX idx_erynfall_characters_account ON erynfall.characters(account_id, is_active);
END
GO

IF OBJECT_ID(N'erynfall.auth_refresh_tokens', N'U') IS NULL
BEGIN
    CREATE TABLE erynfall.auth_refresh_tokens (
      id INT IDENTITY(1,1) PRIMARY KEY,
      account_id INT NOT NULL REFERENCES erynfall.accounts(id) ON DELETE CASCADE,
      token_hash VARCHAR(255) NOT NULL,
      expires_at DATETIME2 NOT NULL,
      revoked_at DATETIME2,
      created_at DATETIME2 DEFAULT GETDATE(),
      UNIQUE (token_hash)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_erynfall_auth_refresh_lookup' AND object_id = OBJECT_ID('erynfall.auth_refresh_tokens'))
BEGIN
    CREATE INDEX idx_erynfall_auth_refresh_lookup ON erynfall.auth_refresh_tokens(token_hash, revoked_at, expires_at);
END
GO

IF OBJECT_ID(N'erynfall.account_recovery_tokens', N'U') IS NULL
BEGIN
    CREATE TABLE erynfall.account_recovery_tokens (
      id INT IDENTITY(1,1) PRIMARY KEY,
      account_id INT NOT NULL REFERENCES erynfall.accounts(id) ON DELETE CASCADE,
      purpose VARCHAR(32) NOT NULL,
      token_hash VARCHAR(255) NOT NULL,
      expires_at DATETIME2 NOT NULL,
      used_at DATETIME2,
      created_at DATETIME2 DEFAULT GETDATE(),
      UNIQUE (token_hash)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_erynfall_recovery_lookup' AND object_id = OBJECT_ID('erynfall.account_recovery_tokens'))
BEGIN
    CREATE INDEX idx_erynfall_recovery_lookup ON erynfall.account_recovery_tokens(token_hash, used_at, expires_at);
END
GO

IF OBJECT_ID(N'erynfall.auth_audit', N'U') IS NULL
BEGIN
    CREATE TABLE erynfall.auth_audit (
      id INT IDENTITY(1,1) PRIMARY KEY,
      account_id INT NULL REFERENCES erynfall.accounts(id) ON DELETE SET NULL,
      event_type VARCHAR(64) NOT NULL,
      success BIT NOT NULL,
      ip_address VARCHAR(64),
      user_agent VARCHAR(512),
      details VARCHAR(1024),
      created_at DATETIME2 DEFAULT GETDATE()
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_erynfall_auth_audit_account' AND object_id = OBJECT_ID('erynfall.auth_audit'))
BEGIN
    CREATE INDEX idx_erynfall_auth_audit_account ON erynfall.auth_audit(account_id, created_at);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_erynfall_auth_audit_type' AND object_id = OBJECT_ID('erynfall.auth_audit'))
BEGIN
    CREATE INDEX idx_erynfall_auth_audit_type ON erynfall.auth_audit(event_type, created_at);
END
GO

-- Optional one-time copy from osrs schema if target is empty.
IF EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'osrs')
   AND EXISTS (SELECT 1 FROM sys.tables WHERE object_id = OBJECT_ID(N'osrs.accounts'))
   AND NOT EXISTS (SELECT 1 FROM erynfall.accounts)
BEGIN
    SET IDENTITY_INSERT erynfall.accounts ON;
    INSERT INTO erynfall.accounts (id, email, password_hash, email_verified, status, created_at, last_login_at)
    SELECT id, email, password_hash, email_verified, status, created_at, last_login_at
    FROM osrs.accounts;
    SET IDENTITY_INSERT erynfall.accounts OFF;
END
GO

IF EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'osrs')
   AND EXISTS (SELECT 1 FROM sys.tables WHERE object_id = OBJECT_ID(N'osrs.characters'))
   AND NOT EXISTS (SELECT 1 FROM erynfall.characters)
BEGIN
    SET IDENTITY_INSERT erynfall.characters ON;
    INSERT INTO erynfall.characters (id, account_id, character_name, is_active, created_at)
    SELECT id, account_id, character_name, is_active, created_at
    FROM osrs.characters;
    SET IDENTITY_INSERT erynfall.characters OFF;
END
GO

IF EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'osrs')
   AND EXISTS (SELECT 1 FROM sys.tables WHERE object_id = OBJECT_ID(N'osrs.auth_refresh_tokens'))
   AND NOT EXISTS (SELECT 1 FROM erynfall.auth_refresh_tokens)
BEGIN
    INSERT INTO erynfall.auth_refresh_tokens (account_id, token_hash, expires_at, revoked_at, created_at)
    SELECT account_id, token_hash, expires_at, revoked_at, created_at
    FROM osrs.auth_refresh_tokens;
END
GO

IF EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'osrs')
   AND EXISTS (SELECT 1 FROM sys.tables WHERE object_id = OBJECT_ID(N'osrs.account_recovery_tokens'))
   AND NOT EXISTS (SELECT 1 FROM erynfall.account_recovery_tokens)
BEGIN
    INSERT INTO erynfall.account_recovery_tokens (account_id, purpose, token_hash, expires_at, used_at, created_at)
    SELECT account_id, purpose, token_hash, expires_at, used_at, created_at
    FROM osrs.account_recovery_tokens;
END
GO

IF EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'osrs')
   AND EXISTS (SELECT 1 FROM sys.tables WHERE object_id = OBJECT_ID(N'osrs.auth_audit'))
   AND NOT EXISTS (SELECT 1 FROM erynfall.auth_audit)
BEGIN
    INSERT INTO erynfall.auth_audit (account_id, event_type, success, ip_address, user_agent, details, created_at)
    SELECT account_id, event_type, success, ip_address, user_agent, details, created_at
    FROM osrs.auth_audit;
END
GO
