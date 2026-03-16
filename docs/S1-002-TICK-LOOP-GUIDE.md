# S1-002: Server Tick Loop - Implementation Guide

**Task:** Implement a proper 256-tick/sec game loop with nanosecond precision.

**Time Estimate:** 2-4 hours

**Difficulty:** Medium (mostly math + timing logic)

---

## Current State

The stub code exists in `server/src/main/java/com/osrs/server/GameLoop.java`:

```java
public void run() {
    long lastTickNs = System.nanoTime();
    long logIntervalTicks = 256; // Log once per second
    
    while (running) {
        // ... processes tick ...
        // ... sleeps until next tick ...
    }
}
```

The **skeleton is there**, but needs refinement:
- Timing precision needs verification
- `processTick()` is empty (can stay empty for now)
- Logging could be clearer

---

## What to Implement

### 1. Verify Nanosecond Timing

**Current code (in `run()` method):**

```java
long sleepNs = tickIntervalNs - tickDuration;

if (sleepNs > 0) {
    Thread.sleep(sleepNs / 1_000_000, (int) (sleepNs % 1_000_000));
} else {
    // Tick overran
    if (tickDuration > tickIntervalNs * 1.1) {
        LOG.warn("Tick {} took {} ms (expected {:.1f} ms)", ...);
    }
}
```

**This is correct.** Verify it's calculating sleep properly:

- `tickIntervalNs = 1_000_000_000 / 256 = 3_906_250 ns = 3.90625 ms`
- If tick took 3ms, sleep for 0.90625ms
- If tick took 4.5ms, log warning but don't sleep (overrun)

**Your job:** Make sure the math is right. Run the server and verify.

### 2. Improve `processTick()` Method

Currently:

```java
private void processTick() {
    // TODO: Implement tick processing
}
```

**For S1-002, this can stay empty or have a comment.** The key is the loop runs.

Optional: Add a stub comment showing what will go here later:

```java
private void processTick() {
    // Tick processing stages (implemented in later sprints):
    // 1. Process player input (S1-005)
    // 2. Update entity state (S1-009)
    // 3. Calculate collisions (S1-009)
    // 4. Broadcast deltas (S1-005)
}
```

### 3. Add Better Logging

The logging is decent, but make it clear:

```java
LOG.info("Tick {} (uptime: {} sec)", tickCount, tickCount / 256);
```

This prints every 256 ticks (once per second). **Verify this is in the code.**

Optional improvements:
- Add startup message: `LOG.info("Starting game loop at {} Hz", TICK_RATE_HZ);`
- Add shutdown message: `LOG.info("Game loop stopped at tick {}", tickCount);`

---

## Step-by-Step Implementation

### Step 1: Review Current Code

Open `server/src/main/java/com/osrs/server/GameLoop.java`.

Read through the entire file. Understand:
- `tickIntervalNs` calculation
- The `run()` loop logic
- How `Thread.sleep()` is called
- Where logging happens

**Time: 15 min**

### Step 2: Test Current Implementation

Create a feature branch:

```bash
git checkout -b feature/s1-002-tick-loop
```

Run the server:

1. Open `Server.java` in IntelliJ
2. Click green play button ▶️
3. Watch console output for 30 seconds
4. Note the tick output:
   ```
   [INFO] Tick 256 (uptime: 1 sec)
   [INFO] Tick 512 (uptime: 2 sec)
   [INFO] Tick 768 (uptime: 3 sec)
   ...
   ```
5. Press Ctrl+C to stop

**Expected:** Ticks increment smoothly, one log per second, clean shutdown.

**If not working:**
- No ticks printing? → processTick() isn't being called (check loop logic)
- Irregular ticks? → Timing math is wrong (check sleep calculation)
- Doesn't shut down? → SIGTERM handler not installed (check Runtime.getRuntime().addShutdownHook)

**Time: 10 min**

### Step 3: Add Startup Logging

In `GameLoop.start()` method, add:

```java
public void start() {
    LOG.info("Starting game loop at {} Hz ({} ns per tick)", TICK_RATE_HZ, tickIntervalNs);
    // ... rest of method ...
}
```

**Why:** Makes it clear the loop is starting + what the target rate is.

**Time: 2 min**

### Step 4: Test Again

Run the server again:

```
[INFO] Starting game loop at 256 Hz (3906250 ns per tick)
[INFO] Tick 256 (uptime: 1 sec)
[INFO] Tick 512 (uptime: 2 sec)
...
```

Watch for 30+ seconds. Verify:
- ✅ Logs print exactly once per second
- ✅ No timing drift (ticks are 256 apart)
- ✅ No warnings about overruns
- ✅ Ctrl+C exits cleanly

**Time: 10 min**

### Step 5: Add processTick() Comment

In the `processTick()` method, replace TODO with a stub:

```java
private void processTick() {
    // Tick processing stages:
    // 1. Process player input (queued from Netty)
    // 2. Update entity positions
    // 3. Calculate collisions
    // 4. Execute combat calculations
    // 5. Send delta updates to clients
    // 
    // Implemented in later sprints (S1-005+)
}
```

**Why:** Documents what this method will do, so future tasks know where to hook in.

**Time: 5 min**

### Step 6: Commit Your Work

```bash
# Review changes
git status

# Stage files
git add server/src/main/java/com/osrs/server/

# Commit (short, imperative message)
git commit -m "Improve tick loop logging and documentation"

# Or if you made multiple changes:
git commit -m "Add startup logging to tick loop"
git commit -m "Document processTick() stages for future sprints"
```

**Time: 5 min**

### Step 7: Create PR on GitHub

Push your branch:

```bash
git push origin feature/s1-002-tick-loop
```

Go to GitHub:
1. https://github.com/EarthDeparture/osrs-mmorp
2. Click **Pull Requests**
3. Click **New Pull Request**
4. Set: `feature/s1-002-tick-loop` → `main`
5. Fill in description:

```markdown
## Sprint S1, Task #002: Server Tick Loop Implementation

### What
Verified + enhanced the 256-tick/sec game loop with proper nanosecond timing.

### Why
Foundation for all game systems. Tick rate must be deterministic and drift-free.

### Testing
- ✅ Server runs 30+ seconds without stopping
- ✅ Tick count increments smoothly (256 ticks per second)
- ✅ One log output per second (no drift)
- ✅ No timing warnings or exceptions
- ✅ Ctrl+C exits cleanly

### Changes
- Added startup logging (clarifies tick rate)
- Verified nanosecond timing math
- Documented processTick() stages for future sprints

### Related Issues
Closes S1-002
```

6. Request review
7. Click **Create Pull Request**

**Time: 10 min**

---

## Acceptance Criteria Checklist

Before submitting PR, verify:

- [ ] Server runs for 30+ seconds without stopping
- [ ] Tick output prints once per second
- [ ] Tick count is correct (256 per second = N×256 ticks at N seconds)
- [ ] No warnings about timing overruns
- [ ] No exceptions or errors in console
- [ ] Shutdown is clean (Ctrl+C exits in <2 seconds)
- [ ] Code compiles without warnings
- [ ] Comments added to `processTick()` documenting future stages
- [ ] Commit messages are clear + imperative
- [ ] PR description includes test results

---

## Common Issues & Fixes

### Issue: Ticks don't print

**Cause:** `processTick()` isn't being called, or `running` flag isn't set.

**Fix:** 
```java
// Make sure you have:
running = true;  // in start()
while (running) { processTick(); ... }  // in run()
```

### Issue: Ticks print irregularly (gaps or doubles)

**Cause:** Timing math is wrong, or sleep calculation is off.

**Fix:** Print intermediate values:
```java
long tickDuration = now - lastTickNs;
long sleepNs = tickIntervalNs - tickDuration;
LOG.debug("Duration: {}ns, Sleep: {}ns", tickDuration, sleepNs);
```

Compare to expected (3906250 ns). If much higher/lower, timing math needs adjustment.

### Issue: Server doesn't shut down on Ctrl+C

**Cause:** `running` flag isn't being set to false, or loop is stuck in sleep.

**Fix:** Add shutdown hook (check if it exists):
```java
Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
```

Make sure `shutdown()` sets `running = false`.

### Issue: High CPU usage

**Cause:** Sleep calculation is negative (tick takes longer than interval), so loop spins continuously.

**Fix:** Add minimum sleep:
```java
if (sleepNs > 0) {
    Thread.sleep(...);
} else {
    // Don't spin; just log overrun and move on
}
```

---

## Implementation Time Breakdown

| Step | Time | Total |
|------|------|-------|
| Review code | 15 min | 15 min |
| Test current impl | 10 min | 25 min |
| Add logging | 2 min | 27 min |
| Test again | 10 min | 37 min |
| Comment processTick | 5 min | 42 min |
| Commit | 5 min | 47 min |
| Create PR | 10 min | 57 min |
| **Buffer** | ~90 min | **2-4 hours total** |

Buffer accounts for debugging timing issues or understanding the code.

---

## What Not to Do

❌ **Don't** rewrite the entire GameLoop from scratch  
❌ **Don't** implement actual entity processing (that's S1-010)  
❌ **Don't** add networking code (that's S1-003)  
❌ **Don't** commit debug logs (System.out.println) — use LOG.debug  
❌ **Don't** add magical constants — all numbers should be in fields with comments  

---

## What This Sets Up

Once S1-002 is done:

- **S1-003:** Netty server can add input processing to `processTick()`
- **S1-005:** Network handlers can queue packets to be processed in `processTick()`
- **S1-009:** Entity updates can be added to `processTick()`

This is the **backbone**. Everything hooks into the tick loop.

---

## Questions?

If stuck:
1. Check the code in `server/src/main/java/com/osrs/server/GameLoop.java`
2. Compare to ARCHITECTURE.md (System Overview section on tick loop)
3. Ask in Discord (#parsundra)

---

**Ready?** 

```bash
git checkout -b feature/s1-002-tick-loop
```

Start with Step 1. You got this. 🚀
