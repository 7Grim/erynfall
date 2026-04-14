# Setup Guide

For the current art workflow and hotkeys, see `ARTIST_GUIDE.md`.
For architecture and code conventions, see `AGENTS.md` and `docs/ARCHITECTURE.md`.

---

## Prerequisites

- **Java 21+** (LTS)
- **Maven 3.8+**
- **Git**

### Install Java 21

**Windows:**
```powershell
choco install openjdk21
# or download from https://jdk.java.net/21
```

**macOS:**
```bash
brew install openjdk@21
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt install openjdk-21-jdk
```

Verify: `java -version` — should show `openjdk version 21`.

### Install Maven

**Windows:**
```powershell
choco install maven
```

**macOS:**
```bash
brew install maven
```

**Linux:**
```bash
sudo apt install maven
```

Verify: `mvn -v` — should show `Apache Maven 3.8+`.

---

## Clone and Build

```bash
git clone https://github.com/EarthDeparture/erynfall.git
cd erynfall
mvn clean install
```

First build downloads ~500 MB of dependencies. Subsequent builds are under a minute.

---

## Run the Server

```bash
mvn -pl server exec:java -Dexec.mainClass="com.osrs.server.Server"
```

Server starts on port 43594. Ready when you see ticks incrementing in the log.

---

## Run the Client

```bash
mvn -pl client exec:exec
```

**macOS note:** The client exec config already includes `-XstartOnFirstThread`. If running directly with `java -jar`, add that flag manually:
```bash
java -XstartOnFirstThread -jar client/target/osrs-client-0.1.0-SNAPSHOT.jar
```

---

## Run Artist Mode

Build the client jar first:

```bash
mvn -pl client -am -DskipTests package
```

Then launch with the appropriate script:

**macOS / Linux:**
```bash
./scripts/run-artist-client.sh
```

**Windows (PowerShell):**
```powershell
.\scripts\run-artist-client.ps1
```

**Windows (cmd):**
```cmd
scripts\run-artist-client.bat
```

In artist mode, press `F6` to open the Art Workbench.

---

## Run the Auth Service (optional — for full login flow)

```bash
mvn -pl auth spring-boot:run
```

Configure DB and JWT env vars — see `docs/ARCHITECTURE.md` for details.

---

## Module Structure

```
erynfall/
├── shared/      Protocol Buffers schema + shared models/registries
├── auth/        Spring Boot JWT auth service (Azure App Service deployment)
├── server/      Authoritative game loop, gameplay systems, Netty networking
├── client/      LibGDX renderer, UI, artist mode, Art Workbench
├── art/         Blender source (.blend), runtime models (.glb/.g3dj), sprites, world scene
├── scripts/     Art validation, model generation, Blender export, artist launch scripts
├── sql/         SQL Server schema and setup docs
└── docs/        Architecture, progress, art pipeline, contributing
```

---

## IDE Setup (IntelliJ IDEA)

1. **File → Open** → select the `erynfall` folder
2. Trust the project — IntelliJ auto-detects the Maven multi-module structure

### Run Configurations

**Server:**
- Main class: `com.osrs.server.Server`
- Working directory: `$ProjectFileDir$/server`

**Client:**
- Main class: `com.osrs.client.Client`
- Working directory: `$ProjectFileDir$/client`
- VM options (macOS only): `-XstartOnFirstThread`

**Auth service:**
- Use the Maven run configuration: `spring-boot:run` in the `auth` module

---

## Troubleshooting

### "Maven not found"
```bash
mvn -v
# Install: brew install maven (macOS) / choco install maven (Windows)
```

### "Java 21 not found"
```bash
java -version
# Install: brew install openjdk@21 (macOS) / choco install openjdk21 (Windows)
```

### Client window doesn't open (macOS)
Ensure you're using `-XstartOnFirstThread`. Without it the LWJGL3 OpenGL context will fail silently on macOS.

### "Cannot find symbol: class ..."
Rebuild the shared module first:
```bash
mvn clean install -pl shared -am
```

### Art validation fails on Windows
Art validation is auto-skipped on Windows (Maven profile). If you want to run it manually:
```bash
python3 scripts/validate-art.py
python3 scripts/validate-models.py
```

### Build takes a long time
First build downloads all Maven dependencies (~500 MB). After that, targeted builds are fast:
```bash
mvn -pl client -am -DskipTests package  # client only, no tests
```
