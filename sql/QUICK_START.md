# OSRS-MMORP Database Setup — Quick Start Guide

## For: victorystyle (Windows 11 + SQL Server 2025 Express)

---

## Step 1: Open SQL Server Management Studio (SSMS)

1. Open **SSMS** on your Windows machine
2. Connect to `localhost` with Windows Authentication
3. Click **Connect**

---

## Step 2: Create Fresh Database (First Time ONLY)

### Option A: Fresh Install (Recommended)

1. Open SSMS Query Editor
2. Paste **entire** contents of `osrs_mmorp_schema.sql`
3. Click **Execute** (F5) or right-click → **Execute**
4. **Wait 10-15 seconds** for completion
5. You should see:
   ```
   ========== SCHEMA CREATION COMPLETE ==========
   Database: osrsmmorp
   Schema: osrs
   Status: READY FOR DEVELOPMENT
   ```

### Option B: If You Get Errors (Start Fresh)

**RUN THESE IN ORDER:**

1. Open new Query Editor tab
2. Paste **entire** contents of `osrs_mmorp_reset.sql`
3. Click **Execute** (F5)
4. **Wait 5 seconds** for database deletion
5. Open **another** new Query Editor tab
6. Paste **entire** contents of `osrs_mmorp_schema.sql`
7. Click **Execute** (F5)
8. **Wait 10-15 seconds** for completion

---

## Step 3: Verify Installation

In SSMS, run this simple query:

```sql
USE osrsmmorp;
GO

SELECT COUNT(*) AS tables_created FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'osrs';
```

**Expected result:** `14`

If you see `14 tables`, you're good. ✅

---

## Step 4: Test with Sample Data

Run this to create a test player:

```sql
USE osrsmmorp;
GO

DECLARE @player_id INT;

EXEC osrs.sp_create_player 
  @username = 'Tester',
  @password_hash = 'HASH_HERE',
  @email = 'test@example.com',
  @new_player_id = @player_id OUTPUT;

PRINT 'Created player ID: ' + CAST(@player_id AS VARCHAR(10));
```

**Expected result:**
```
Created player ID: 1
```

---

## Step 5: Add Some XP to Test Player

```sql
USE osrsmmorp;
GO

EXEC osrs.sp_add_experience 1, 'ATTACK', 5000;

-- Verify
SELECT username, attack_xp FROM osrs.players WHERE id = 1;
```

**Expected result:**
```
username  attack_xp
--------  ---------
Tester    5000
```

---

## Step 6: Check Hiscores

```sql
USE osrsmmorp;
GO

SELECT TOP 5 username, overall_level, overall_rank FROM osrs.hiscores ORDER BY overall_rank;
```

**Expected result:**
```
username  overall_level  overall_rank
--------  -------------  -----------
Tester    1              1
```

---

## Step 7: Copy Connection String for Java

Use this in your Java code:

```java
String jdbcUrl = "jdbc:sqlserver://localhost:1433;databaseName=osrsmmorp;encrypt=false;trustServerCertificate=true;";
String user = "sa";  // or your Windows login
String password = "...";  // leave empty if using Windows auth
```

---

## Troubleshooting

### Error: "Database 'osrsmmorp' already exists"

**Solution:** Run the RESET script first (see Step 2, Option B)

### Error: "Cannot open database 'osrsmmorp'"

The database wasn't created. Make sure you:
1. Ran the **entire** `osrs_mmorp_schema.sql` (not just part of it)
2. Waited for "SCHEMA CREATION COMPLETE" message
3. Can see the database in SSMS left panel under "Databases"

### Query Returns "0 tables"

The schema didn't create properly. **Use the RESET + fresh install sequence** (Step 2, Option B)

### "EXECUTE permission denied" Error

You don't have permission to run stored procedures. Contact your SQL Server admin, or:
1. Use Windows Authentication (not SQL Server login)
2. Or create a new SQL Server login with `db_owner` role on `osrsmmorp`

---

## What Got Created

### 14 Tables
- Players (accounts, XP, position, gold)
- Inventory (28-slot limit)
- Items (definitions: stackable, tradeable, prices)
- NPCs (Chicken, Goblin, Cow)
- NPC Loot Tables (drop rates)
- Ground Loot (items on map)
- Quests (progress tracking)
- Grand Exchange (orders)
- Trade History (audit)
- Achievements (tracking)
- Chat Messages (public + private)
- Sessions (online players)
- Processed Packets (anti-duplication)
- Config (server settings)

### 2 Views
- `osrs.hiscores` — Ranked leaderboard
- `osrs.active_players` — Online players with positions

### 4 Stored Procedures
- `sp_add_experience` — Award XP (atomic)
- `sp_create_player` — New player registration
- `sp_complete_quest_objective` — Quest updates (atomic)
- `sp_cleanup_idle_sessions` — Maintenance (hourly)

### 20+ Indices
Performance optimization for all common queries

---

## Next Steps (For Java Development)

1. **Add Maven dependency** (in `pom.xml`):
```xml
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>12.6.0.jre21</version>
</dependency>
```

2. **Create DatabaseManager.java** to initialize connection pool:
```java
HikariDataSource dataSource = new HikariDataSource();
dataSource.setJdbcUrl("jdbc:sqlserver://localhost:1433;databaseName=osrsmmorp;...");
dataSource.setUsername("sa");
dataSource.setPassword("...");
dataSource.setMaximumPoolSize(10);
```

3. **Run queries** from Java using parameterized PreparedStatements

4. **Use stored procedures** for complex operations (XP gain, quest completion)

---

## File Locations

On your Windows machine (Git repo):

```
osrs-mmorp/
├── sql/
│   ├── osrs_mmorp_schema.sql     ← Run this first (main schema)
│   ├── osrs_mmorp_reset.sql       ← Run this if you need a fresh start
│   ├── README.md                  ← Full documentation
│   └── QUICK_START.md             ← This file
```

---

## If Something Goes Wrong

**The RESET script is your friend:**

1. Run `osrs_mmorp_reset.sql` (wipes everything)
2. Wait 5 seconds
3. Run `osrs_mmorp_schema.sql` (fresh install)
4. You're back to a clean state ✅

---

## Quick Command Reference

| Task | Command |
|------|---------|
| Create test player | `EXEC osrs.sp_create_player 'TestName', 'hash', NULL, @id OUTPUT;` |
| Add XP | `EXEC osrs.sp_add_experience 1, 'ATTACK', 1000;` |
| Get player level | `SELECT CAST(POWER(attack_xp / 4.0, 1.0/3.0) AS INT) FROM osrs.players WHERE id = 1;` |
| View all players | `SELECT id, username, attack_xp FROM osrs.players;` |
| Check inventory | `SELECT * FROM osrs.inventory WHERE player_id = 1;` |
| Leaderboard | `SELECT TOP 10 * FROM osrs.hiscores ORDER BY overall_rank;` |
| Active players | `SELECT * FROM osrs.active_players;` |
| Cleanup sessions | `EXEC osrs.sp_cleanup_idle_sessions;` |

---

**Questions?** Check `README.md` for full documentation.

**Status:** ✅ Ready to use

Last updated: 2026-03-21
