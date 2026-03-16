# Setup Guide - OSRS MMORP

This guide will get you running the server and client locally in under 10 minutes.

## Prerequisites

### System Requirements
- **OS:** Windows, macOS, or Linux
- **Java:** Java 21+ (LTS)
- **Maven:** 3.8+
- **Git:** Latest

### Install Java 21

**Windows:**
```bash
# Using choco
choco install openjdk21

# Or download from https://jdk.java.net/21
```

**macOS:**
```bash
brew install openjdk@21
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt install openjdk-21-jdk
```

**Verify:**
```bash
java -version
# Output should show "openjdk version 21"
```

### Install Maven (Option 1: Recommended for Windows)

**Windows (using Chocolatey):**
```powershell
choco install maven
```

Then **restart PowerShell/CMD** and verify:
```powershell
mvn -v
# Output should show "Apache Maven 3.8+"
```

**macOS:**
```bash
brew install maven
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt install maven
```

### Alternative: Use Maven Wrapper (No Installation Needed)

If you don't want to install Maven, use the wrapper included in the repo:

**Windows (PowerShell):**
```powershell
./mvnw.cmd clean install
```

**macOS/Linux:**
```bash
./mvnw clean install
```

The wrapper will auto-download Maven on first run (~100 MB).

### Install Git

Download from https://git-scm.com or use your package manager.

---

## Clone the Repository

```bash
git clone https://github.com/EarthDeparture/osrs-mmorp.git
cd osrs-mmorp
```

---

## Build the Project

```bash
mvn clean install
```

This will:
1. Download all dependencies (~500 MB, first time only)
2. Compile server, client, and shared modules
3. Run unit tests
4. Create JAR files in `target/` directories

**First build takes 3-5 minutes. Subsequent builds take <1 minute.**

---

## Run the Server

### From IDE (IntelliJ IDEA)

1. Open `server/src/main/java/com/osrs/server/Server.java`
2. Right-click → "Run Server.main()"
3. Server starts on localhost:43594

### From Command Line

```bash
cd server
mvn exec:java -Dexec.mainClass="com.osrs.server.Server"
```

**Expected output:**
```
[INFO] OSRS MMORP Server starting...
[INFO] Server tick rate: 256 Hz (3.90625ms per tick)
[INFO] Starting game loop (interval: 3906250 ns)
[INFO] Tick 256 (uptime: 1 sec)
[INFO] Tick 512 (uptime: 2 sec)
...
```

Server is ready when you see ticks incrementing.

---

## Run the Client

### From IDE (IntelliJ IDEA)

1. Open `client/src/main/java/com/osrs/client/Client.java`
2. Right-click → "Run Client.main()"
3. Window appears (dark background with placeholder UI)

### From Command Line

```bash
cd client
mvn exec:java -Dexec.mainClass="com.osrs.client.Client"
```

**Expected output:**
```
[INFO] OSRS MMORP Client starting...
[INFO] Creating LibGDX application
```

A window should appear with a gray background.

---

## IDE Setup (IntelliJ IDEA)

### Import Project

1. **File** → **Open** → Select `osrs-mmorp` folder
2. **Trust Project** (if prompted)
3. IntelliJ auto-detects Maven structure

### Configure Run Configurations

**For Server:**
1. Top-right, **Edit Configurations**
2. Click **+** → **Application**
3. Name: `Server`
4. Main class: `com.osrs.server.Server`
5. Working directory: `$ProjectFileDir$/server`
6. Save

**For Client:**
1. **+** → **Application**
2. Name: `Client`
3. Main class: `com.osrs.client.Client`
4. Working directory: `$ProjectFileDir$/client`
5. Save

Now you can press **Shift+F10** to run (or use the green play button).

---

## Project Structure

```
osrs-mmorp/
├── server/              # Game server (port 43594)
│   ├── pom.xml
│   └── src/main/java/com/osrs/server/
│       ├── Server.java          ← Main entry point
│       └── GameLoop.java        ← 256-tick loop
├── client/              # Game client (LibGDX)
│   ├── pom.xml
│   └── src/main/java/com/osrs/client/
│       ├── Client.java          ← Main entry point
│       └── GameScreen.java      ← Rendering
├── shared/              # Shared code + network protocol
│   ├── pom.xml
│   ├── src/main/java/com/osrs/shared/
│   │   ├── Entity.java
│   │   ├── Player.java
│   │   └── NPC.java
│   └── src/main/proto/
│       └── network.proto        ← Protocol Buffers schema
├── assets/              # Game data (YAML configs, sprites)
│   └── data/
│       ├── map.yaml
│       ├── npcs.yaml
│       ├── quests.yaml
│       └── dialogue.yaml
├── docs/
│   ├── ARCHITECTURE.md  ← System design
│   ├── CONTRIBUTING.md  ← Git workflow
│   └── PROGRESS.md      ← Sprint tracking
└── README.md
```

---

## Troubleshooting

### "Maven not found"
```bash
# Check Maven is installed
mvn -v

# If not, install:
# macOS: brew install maven
# Windows: choco install maven
# Linux: sudo apt install maven
```

### "Java 21 not found"
```bash
# Check Java version
java -version

# If Java < 21, upgrade:
# macOS: brew install openjdk@21
# Windows: choco install openjdk21
# Linux: sudo apt install openjdk-21-jdk
```

### "Cannot find symbol: class Entity"
```bash
# Rebuild shared module first
cd shared
mvn clean install
cd ../server
mvn clean compile
```

### "LibGDX window doesn't open"
```bash
# Check you have graphics drivers installed (not in VM)
# If on VM, you may need to skip client for now

# Test with a simple console output instead
cd client
mvn exec:java -Dexec.mainClass="com.osrs.client.Client" 2>&1 | grep -i "gdx\|error"
```

### Build takes forever
```bash
# First build downloads 500 MB of dependencies
# Subsequent builds are much faster
# If stuck, check internet connection and try:
mvn clean install -o  # Offline mode (if deps cached)
```

---

## Next Steps

1. **Read [PROGRESS.md](docs/PROGRESS.md)** — See sprint tasks
2. **Read [CONTRIBUTING.md](docs/CONTRIBUTING.md)** — Git workflow + code standards
3. **Check [ARCHITECTURE.md](docs/ARCHITECTURE.md)** — System design
4. **Start S1-002** — Implement the tick loop properly (see PROGRESS.md)

---

## Quick Commands

```bash
# Build everything
mvn clean install

# Build + run tests
mvn clean test

# Run server
cd server && mvn exec:java -Dexec.mainClass="com.osrs.server.Server"

# Run client
cd client && mvn exec:java -Dexec.mainClass="com.osrs.client.Client"

# Skip tests (faster)
mvn clean install -DskipTests

# Compile only (no tests)
mvn clean compile

# Check for issues
mvn spotbugs:check  # (after spotbugs plugin added)
```

---

## IDE Keyboard Shortcuts (IntelliJ)

| Action | Shortcut |
|--------|----------|
| Run | Shift+F10 |
| Debug | Shift+F9 |
| Stop | Ctrl+F2 |
| Open file | Ctrl+N |
| Build | Ctrl+F9 |
| Find in files | Ctrl+Shift+F |
| Git commit | Ctrl+K |
| Git push | Ctrl+Shift+K |

---

Still stuck? Check Discord (#parsundra) or see [CONTRIBUTING.md](docs/CONTRIBUTING.md) for blockers.
