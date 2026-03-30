# AGENTS.md

Operational guide for coding agents working in this repository.
Scope: entire monorepo (`client`, `server`, `shared`).

## Project Snapshot
- Language/runtime: Java 21.
- Build tool: Maven (multi-module parent at repo root).
- Modules: `shared` (protocol/models), `server` (authoritative game loop), `client` (LibGDX UI/rendering).
- Networking: Netty + Protocol Buffers (`shared/src/main/proto/network.proto`).
- Logging: SLF4J + Logback.
- CI workflow (`.github/workflows/compile-check.yml`) runs `mvn clean install -DskipTests` then `mvn test`.

## Source of Truth Documents
- Read `CLAUDE.md` for architecture and common commands.
- Read `docs/CONTRIBUTING.md` for branch/commit/process standards.
- Read `docs/ARCHITECTURE.md` for server-authoritative design decisions.
- No Cursor rules were found (`.cursor/rules/` and `.cursorrules` absent at time of writing).
- No Copilot rules were found (`.github/copilot-instructions.md` absent at time of writing).

## Build, Test, and Run Commands
Run from repository root unless noted.

```bash
# Full build
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run all tests
mvn clean test

# Run tests in one module
mvn test -pl server
mvn test -pl client
mvn test -pl shared

# Run one test class (important)
mvn test -pl server -Dtest=YourTestClass

# Run one test method
mvn test -pl server -Dtest=CombatEngineTest#testHitRollDeterministic

# Run server/client
mvn -pl server exec:java -Dexec.mainClass="com.osrs.server.Server"
mvn -pl client exec:java -Dexec.mainClass="com.osrs.client.Client"

# Run auth service
mvn -pl auth spring-boot:run
```

Single-test examples:
- `mvn test -pl server -Dtest=CombatEngineTest`
- `mvn test -pl client -Dtest=GameScreenTest`

### Lint/Static Analysis Status
- No dedicated lint/checkstyle/spotless/pmd config is currently present.
- Use compilation + tests as the quality gate (`mvn clean install`, `mvn clean test`).
- Keep code style consistent manually (see rules below).

## Code Style and Conventions
These rules reflect existing code in `client/src/main/java`, `server/src/main/java`, and `shared/src/main/java`.

### Imports
- Prefer explicit imports; avoid wildcard imports in new code.
- Order imports by group: project (`com.osrs...`), third-party (`io.netty`, `org.slf4j`, `com.badlogic.gdx`), then JDK (`java...`).
- Keep imports stable and remove unused imports before finishing.
- Avoid static imports; use fully-qualified names instead.

### Formatting
- Use 4 spaces for indentation, no tabs.
- Opening braces stay on the same line (`if (...) {`).
- Keep one statement per line unless concise getter/setter style is already used in the file.
- Preserve existing section separators and spacing style in larger classes.
- Keep lines reasonably readable; split fluent builders/chained calls across lines.
- Field ordering: static constants → instance fields → constructors → methods.

### Naming
- Classes/interfaces/enums: `PascalCase`.
- Methods/fields/local vars: `camelCase`.
- Constants: `SCREAMING_SNAKE_CASE`.
- Package names: lowercase (`com.osrs.server.network`).
- Use descriptive names matching domain language (tick, combat, loot, walk, dialogue).

### Types and Data Structures
- Prefer concrete generics (`Map<Integer, Player>`) over raw types.
- Use primitives for hot-path/game-loop values (`int`, `long`, `float`) where practical.
- Tick/time values are typically `long` (`tickCount`, intervals in ns/ticks).
- Keep protocol-facing IDs as numeric primitives to match protobuf schema.
- Avoid introducing nullable ambiguity; document sentinel values (commonly `-1` for none).

### Control Flow and Error Handling
- Validate early and return early (common pattern in packet handlers).
- Catch specific exceptions when possible; log context with SLF4J.
- Do not swallow exceptions silently.
- For `InterruptedException`, re-set interrupt flag with `Thread.currentThread().interrupt()`.
- Avoid `System.out.println` and `printStackTrace`; use `LOG.debug/info/warn/error`.
- Fail fast for invalid state in core systems, but degrade gracefully for optional dependencies (e.g., DB unavailable).

### Logging
- Use class-level logger: `private static final Logger LOG = LoggerFactory.getLogger(MyClass.class);`
- `debug`: high-frequency or diagnostic detail.
- `info`: lifecycle/state transitions (startup, connect/disconnect, major events).
- `warn`: recoverable issues, invalid client input, fallback paths.
- `error`: exceptions or unrecoverable failures.
- Include identifiers in logs (playerId, npcId, tickCount) when relevant.

### Concurrency and Threading
- Server simulation is tick-based and effectively single-threaded by design.
- Netty handlers may be on separate threads; avoid unsynchronized cross-thread mutations without explicit design.
- Use thread-safe containers only where needed by cross-thread access patterns.
- Keep deterministic game logic in tick-processing paths.

### Network/Protocol Rules
- Add or modify packet contracts in `shared/src/main/proto/network.proto` first.
- Regenerate protobuf classes through Maven build (do not hand-edit generated classes).
- Keep field numbering stable; never reuse removed field numbers.
- Keep client intent lightweight; server remains authoritative for validation/results.

### Domain Rules to Preserve
- Respect server-authoritative architecture (client renders and sends intent only).
- Keep tick-order determinism in `GameLoop` stage sequencing.
- Prefer named constants over magic numbers (distances, speeds, ranges, timers).
- Preserve OSRS-like behavior notes when present (timings, XP, chat/pickup semantics).

### Comments and Documentation
- Comment the reason/constraint, not obvious mechanics.
- Keep high-value Javadocs on public APIs and non-trivial logic.
- Use `@Override` annotation consistently when overriding methods.
- When adding systems/protocol/config files, update docs (`docs/ARCHITECTURE.md`, `docs/PROGRESS.md`) as appropriate.

## Testing Guidance for Agents
- Add tests for deterministic logic (combat roll, pathing, inventory operations, serialization).
- For behavior coupled to ticks/RNG, seed or inject deterministic values.
- Run focused tests first (`-Dtest=...`), then run broader module/all tests.
- If adding protocol fields, run at least `shared` + dependent module tests/build.

## Git and Collaboration Expectations
- Branch naming: `<type>/<sprint>-<task-id>-<description>` (see `docs/CONTRIBUTING.md`).
- Keep commits atomic with imperative, descriptive messages.
- Update `docs/PROGRESS.md` when starting/completing tracked work.
- Avoid unrelated refactors in feature/fix commits unless required for correctness.

## Agent Checklist Before Finishing
- Code compiles (`mvn clean install` or targeted module build).
- Relevant tests pass (at least changed module; ideally full suite).
- No unused imports, debug prints, or commented-out dead code.
- Protocol/data/model changes reflected in docs and dependent modules.
- Changes follow style and architecture guidance in this file.
