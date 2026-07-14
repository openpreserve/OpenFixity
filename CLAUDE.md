# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OpenFixity is a Dropwizard-based web application for monitoring digital file integrity (fixity). It scans file system paths, computes cryptographic hashes, and tracks changes over time. It provides both a REST API and a Mustache-templated web UI.

## Build & Run Commands

```bash
# Build, test and run static analysis. Use this, not `package`.
# SpotBugs is bound to the verify phase, and package runs BEFORE verify, so
# `mvn clean package` silently skips the analysis.
mvn clean verify

# Build executable über-JAR only
mvn clean package

# Run the server (dev config)
java -jar target/open-fixity-1.0.0-ALPHA.jar server dev-server.yml

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=ScanUpdaterDatabaseTest

# Run a specific test method
mvn test -Dtest=ScanUpdaterDatabaseTest#testFirstScanFilesAreAdded

# Skip tests during build
mvn clean package -DskipTests
```

## Architecture

The app follows a layered Dropwizard architecture:

**Entry point:** `OpenFixityServer` (extends `Application<OpenFixityConfiguration>`) in `apps/server/` — registers Hibernate entities, REST resources, exception mappers, and the Quartz scheduler.

**Layers:**
- `apps/server/resources/api/` — JSON REST endpoints (Collections, Paths, Digests, Folders, Scheduler, ScanResults)
- `apps/server/resources/views/` — HTML view endpoints backed by Mustache templates
- `apps/dao/` — Hibernate entities + DAOs: `Collection`, `CollectionPath`, `PathRegistration`, `PathScan`, `FileScanRecord`, `FolderScanRecord`, `DigestRecord`, `PathSummaryRecord`
- `apps/schedule/` — Quartz-based scan job scheduling (`ScheduleManager`, `ScanJob`, `ScanUpdater`, `BatchScanner`)
- `core/paths/` — Scanning logic: `PathScannerImpl` and `AbstractPathScanner` implement `PathScanner`; produces `PathScanResult` / `FileScanResult`
- `core/digests/` — Hash computation: `HasherImpl` implements `Hasher`; `Algorithms` enum lists supported algorithms

**Data flow for a scan:**
1. `CollectionPath` represents a registered filesystem root path
2. `ScanJob` (Quartz) creates a `BatchScanner` (extends `AbstractPathScanner`) and calls `scan()`
3. `AbstractPathScanner` walks the directory tree, calling `processResults()` per batch; `BatchScanner` accumulates `FileScanRecord` objects in-memory on the `PathScan`
4. `PathScan.updateFrom()` finalises status, populates `FolderScanRecord` aggregates, and computes `damagedCount`/`deniedCount` into `PathSummaryRecord`
5. `ScanUpdater.updateDatabase()` loads the persisted `CollectionPath`, compares with the previous scan, then cascades the full `PathScan` → `FileScanRecord` → `DigestRecord` / `FolderScanRecord` graph to H2 in a single transaction

**Entity relationships:**
```
CollectionPath
  └── PathScan (many)
        ├── PathSummaryRecord (one-to-one, eager) — stores totalFiles, totalBytes, damagedCount, deniedCount
        ├── FileScanRecord (one-to-many, lazy, @BatchSize(100))
        │     └── DigestRecord (many-to-many via digest_calculation, lazy, @BatchSize(50))
        └── FolderScanRecord (one-to-many, lazy)
              └── FileScanRecord (one-to-many, lazy, back-reference)
```

**Database:** H2 (file-based at `~/.openfixity/open-fixity`), managed by Hibernate with `hbm2ddl.auto: update`.

**Config file:** `dev-server.yml` sets the application port (8080), the H2 connection URL, log levels, and JDBC batch size. It **is** committed, and is the config the README tells people to run with. It holds no secrets. Note it does not pin the admin connector, which therefore defaults to port 8081.

## Key Technology Versions

- Java 17, Maven 3.9+
- Dropwizard 5.0.1 (Jetty, Jersey, Jackson)
- Hibernate 6.x / Jakarta Persistence
- Quartz Scheduler 2.5.2
- H2 2.4.240
- JUnit 5 for tests, EqualsVerifier for entity equality tests

## Hibernate & Quartz Integration

Quartz jobs run on threads that have no Hibernate session. To use DAOs from a `ScanJob`, wrap the target class with `UnitOfWorkAwareProxyFactory`:

```java
ScanUpdater updater = new UnitOfWorkAwareProxyFactory(OpenFixityServer.getHibernate())
        .create(ScanUpdater.class, CollectionPathDAO.class, cpDAO);
```

`@UnitOfWork` on the proxy's **public** methods then opens and commits a Hibernate session automatically. **`@UnitOfWork` on private methods is silently ignored** — CGLIB proxies cannot intercept private calls. Any private helper that appears to have its own unit of work is actually running in the caller's session.

## Persisted Enums: Always `@Enumerated(EnumType.STRING)`

**Every enum field on an entity must be annotated `@Enumerated(EnumType.STRING)`.** JPA
defaults to `ORDINAL`, which stores the constant's *position*, so reordering or inserting a
constant silently rewrites the meaning of every row already written.

This is not hypothetical. `Algorithms` was reordered during development: `SHA_1` moved from
ordinal 2 to 0, `SHA_256` from 4 to 1. `DigestRecord.algorithm` was stored as an ordinal, so
any database written before the reorder would have had every digest re-attributed to a
different algorithm. A SHA-1 hash would read back as SHA-512. Since `checkDigests()` matches
on algorithm, the fixity comparison would either find no match and report `UNVERIFIED`,
quietly abandoning the check, or compare hashes from two different algorithms and report
`CHANGED` on a file that never changed.

`EnumMappingTest` reflects over every registered entity and fails if any enum lands on an
integer column. Do not weaken it into a hand-written list of columns; that is exactly how the
`DigestRecord` one was missed.

Changing an enum mapping changes the column type, and `hbm2ddl.auto: update` cannot convert
`TINYINT` to `ENUM`. Existing dev databases must be deleted (see Dev Database below).

## Front End: No CDNs

All front-end assets (Bootstrap, Bootstrap Icons, jQuery, bootstrap-table) are **vendored**
under `assets/css/vendor/` and `assets/js/vendor/` and served from the application. Do not
reintroduce a CDN `<link>` or `<script>`. OpenFixity runs on air-gapped and network-restricted
preservation workstations, where a CDN-dependent page renders unstyled, without icons, and
with non-functional modals.

Icons are **Bootstrap Icons** (`<i class="bi bi-...">`), not FontAwesome. The icon fonts live
in `assets/css/vendor/fonts/` because the icons stylesheet resolves them by relative path.
Bootstrap is loaded as the **bundle**, which embeds Popper, so dropdowns and tooltips work if
anyone adds one.

Third-party components are attributed in `NOTICE`. Add to it if you bundle anything new.

## Performance Patterns

- Use `@org.hibernate.annotations.BatchSize` on lazy `@OneToMany` / `@ManyToMany` collections to avoid N+1 SELECT queries. Current batch sizes: `PathScan.results` = 100, `FileScanRecord.digestResults` = 50.
- Use `JOIN FETCH` in named queries for associations needed on list pages (e.g. `PathScan.findAll` fetches `collectionPath` to avoid N+1 per row).
- Store derived counts (damaged, denied) in `PathSummaryRecord` at scan-completion time rather than computing them from lazy collections at read time.
- Enable JDBC batching in `dev-server.yml` (`hibernate.jdbc.batch_size: 50`, `hibernate.order_inserts: true`, `hibernate.order_updates: true`) to group large cascade inserts.

## Testing Notes

Tests live in `src/test/java/org/openpreservation/fixity/`. JUnit 5 style (`@Test` annotations). Entity equality tests use `EqualsVerifier`.

**Test session infrastructure** (`TestSessionFactory`):
- Builds a shared in-memory H2 session factory (`jdbc:h2:mem:testdb`) for all DAO/integration tests
- `beginTransaction()` — opens a session and binds it to the current thread via `ManagedSessionContext`
- `rollback()` — rolls back and closes; used in `@AfterEach` to keep tests isolated
- `@UnitOfWork` annotations on `ScanUpdater` are **not** active in tests (no proxy); methods run in whatever session is currently bound to the thread

**Tests that need committed data or concurrency** (e.g. `ScanUpdaterConcurrentTest`) build their own isolated `SessionFactory` with a unique in-memory DB URL and manage their own `inSession()` commit/rollback lifecycle. This avoids polluting the shared `TestSessionFactory` DB.

## Dev Database

The H2 file database lives at `~/.openfixity/open-fixity.*`. With `hbm2ddl.auto: update`, Hibernate runs `ALTER TABLE` on startup to apply schema changes. This works for adding nullable columns or columns with a `DEFAULT`, but **adding a NOT NULL column without a DEFAULT to a table with existing rows will fail**. When that happens, delete the DB files and restart:

```bash
rm ~/.openfixity/open-fixity.*
java -jar target/open-fixity-1.0.0-ALPHA.jar server dev-server.yml
```

## Commit Conventions

```
FEAT: Add new feature
FIX:  Bug fix
PERF: Performance improvement
TEST: Add or update tests
DOCS: Documentation only
REFACTOR: Code change with no behaviour change
```
