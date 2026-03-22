# OSRS-MMORP Database — Quick Reference

**Status:** ✅ COMPLETE & VERIFIED (2026-03-22)

---

## Database Connection

```
Server: localhost
Database: osrsmmorp
Port: 1433
Auth: Windows Authentication
```

## SQL Server File Locations

```
Data: C:\Program Files\Microsoft SQL Server\MSSQL17.MSSQLSERVER\MSSQL\DATA\osrsmmorp.mdf
Log:  C:\Program Files\Microsoft SQL Server\MSSQL17.MSSQLSERVER\MSSQL\DATA\osrsmmorp_log.ldf
```

## Schema Objects

**14 Tables:**
- osrs.config, osrs.items, osrs.npcs, osrs.npc_loot_tables
- osrs.players, osrs.inventory, osrs.ground_loot
- osrs.player_quests, osrs.ge_orders, osrs.trade_history
- osrs.player_achievements, osrs.chat_messages, osrs.player_sessions
- osrs.processed_packets

**2 Views:**
- osrs.hiscores (ranked leaderboard)
- osrs.active_players (online tracking)

**4 Procedures:**
- osrs.sp_add_experience (atomic XP gain)
- osrs.sp_cleanup_idle_sessions (hourly maintenance)
- osrs.sp_create_player (new player registration)
- osrs.sp_complete_quest_objective (quest tracking)

---

## Common Queries

### Create Test Player
```sql
DECLARE @player_id INT;
EXEC osrs.sp_create_player 'TestPlayer', 'hash', 'test@example.com', @player_id OUTPUT;
```

### Award XP
```sql
EXEC osrs.sp_add_experience 1, 'ATTACK', 1000;
```

### Check Level
```sql
SELECT 
  username,
  attack_xp,
  CAST(POWER(attack_xp / 4.0, 1.0/3.0) AS INT) AS attack_level
FROM osrs.players WHERE id = 1;
```

### View Hiscores
```sql
SELECT TOP 10 * FROM osrs.hiscores ORDER BY overall_rank;
```

### Check Active Players
```sql
SELECT * FROM osrs.active_players;
```

### Cleanup (Maintenance)
```sql
EXEC osrs.sp_cleanup_idle_sessions;
```

---

## Java Integration

### Maven Dependency
```xml
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>12.6.0.jre21</version>
</dependency>
```

### Connection String
```java
String jdbcUrl = "jdbc:sqlserver://localhost:1433;databaseName=osrsmmorp;encrypt=false;trustServerCertificate=true;";
```

### Stored Procedure Call
```java
try (Connection conn = dataSource.getConnection();
     CallableStatement stmt = conn.prepareCall("{CALL osrs.sp_add_experience(?, ?, ?)}")) {
  stmt.setInt(1, playerId);
  stmt.setString(2, "ATTACK");
  stmt.setLong(3, xpDelta);
  stmt.execute();
}
```

---

## Key Constraints

- **Username:** 3-12 characters, unique
- **Inventory:** 28 slots hard limit
- **XP per tick:** Max 10,000 (validated in Java)
- **Max XP:** 200,000,000 (Level 99)
- **Session timeout:** 30 minutes idle → mark offline
- **Packet dedup window:** 5 minutes (old records cleaned)
- **Loot despawn:** 1 hour after drop

---

## Critical Design Decisions

✅ Authority-server validation (all calculations server-side)
✅ 28-slot inventory (hard limit, non-negotiable)
✅ Cascade delete: primary FK CASCADE, secondary NO ACTION
✅ Atomic operations via stored procedures
✅ Filtered indices for performance (GE order matching O(log n))
✅ Anti-duplication protection (processed_packets table)
✅ Session tracking with heartbeat mechanism
✅ Bitmask for quest objectives (32 max per quest)

---

## Files in Repository

```
osrs-mmorp/
├── sql/
│   ├── osrs_mmorp_schema.sql      ← Master schema (23.1 KB)
│   ├── osrs_mmorp_reset.sql       ← Database wipe script
│   ├── README.md                  ← Full documentation
│   ├── QUICK_START.md             ← Setup guide
│   └── COMMAND_REFERENCE.sql      ← Test queries
└── DATABASE_SETUP_SUMMARY.md      ← Architecture overview
```

---

## Memory Files

```
memory/osrs-mmorp/
├── DATABASE_SCHEMA_FINAL.md       ← Design rationale & structure
├── DATABASE_IMPLEMENTATION_LOG.md ← Complete timeline & decisions
└── QUICK_REFERENCE.md             ← This file
```

---

## Next Steps

1. **S1.5 Integration:**
   - Create DatabaseManager.java (HikariCP)
   - Update Server.java to initialize DB
   - Estimated: 2-3 hours

2. **S2 Systems:**
   - Wire CombatEngine to DB (XP, loot)
   - Implement InventoryManager (28-slot)
   - Add QuestManager
   - Estimated: 12-18 hours

---

## Verification (Run in SSMS)

```sql
-- Should return 14
SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'osrs';

-- Should return 2
SELECT COUNT(*) FROM INFORMATION_SCHEMA.VIEWS WHERE TABLE_SCHEMA = 'osrs';

-- Should return 4
SELECT COUNT(*) FROM INFORMATION_SCHEMA.ROUTINES WHERE ROUTINE_SCHEMA = 'osrs' AND ROUTINE_TYPE = 'PROCEDURE';
```

---

**Last Updated:** 2026-03-22  
**Status:** ✅ COMPLETE & VERIFIED
