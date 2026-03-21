-- ============================================================================
-- OSRS-MMORP Database RESET Script
-- ============================================================================
-- Purpose: COMPLETE database wipe for fresh start
-- WARNING: This script DELETES EVERYTHING. Only run if you want a clean slate.
-- Created: 2026-03-21
-- Usage: Run in SSMS when you need to start over (errors, corruption, testing)
-- ============================================================================

USE [master];
GO

-- ============================================================================
-- STEP 1: Kill all active connections to osrsmmorp
-- ============================================================================

PRINT 'Closing all connections to osrsmmorp database...';
GO

ALTER DATABASE [osrsmmorp] SET OFFLINE WITH ROLLBACK IMMEDIATE;
GO

-- Wait a moment for cleanup
WAITFOR DELAY '00:00:02';
GO

-- ============================================================================
-- STEP 2: Drop the database completely
-- ============================================================================

IF EXISTS (SELECT 1 FROM sys.databases WHERE name = N'osrsmmorp')
BEGIN
  PRINT 'Dropping osrsmmorp database...';
  DROP DATABASE [osrsmmorp];
  PRINT 'Database osrsmmorp dropped successfully.';
END
ELSE
BEGIN
  PRINT 'Database osrsmmorp does not exist. Skipping drop.';
END
GO

-- Wait for file cleanup
WAITFOR DELAY '00:00:02';
GO

-- ============================================================================
-- STEP 3: Verify deletion
-- ============================================================================

IF NOT EXISTS (SELECT 1 FROM sys.databases WHERE name = N'osrsmmorp')
BEGIN
  PRINT '';
  PRINT '========== RESET COMPLETE ==========';
  PRINT 'osrsmmorp database has been completely removed.';
  PRINT 'You may now run osrs_mmorp_schema.sql to create a fresh database.';
  PRINT '';
END
ELSE
BEGIN
  RAISERROR('ERROR: osrsmmorp database still exists!', 16, 1);
END
GO
