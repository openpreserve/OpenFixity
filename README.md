# OpenFixity

**OpenFixity** monitors the integrity of digital collections. It scans files on
local disks, removable storage, and network locations, and reports whether anything
has been changed, added, removed, moved, or renamed. Scans run on a schedule, so
problems surface early.

OpenFixity is the successor to Fixity Pro, originally developed by AVP and since
transferred to the Open Preservation Foundation. See
[ACKNOWLEDGEMENTS.md](ACKNOWLEDGEMENTS.md) for the project's history, and for the
people it is owed to.

> **Status: pre-alpha.** Not yet ready for use against collections you care about.

## Ways to run it

OpenFixity is one application with three front doors:

1. **Desktop app** (primary). A native installer for Windows, macOS, and Linux,
   with a bundled Java runtime, so users install nothing else. It opens the UI in
   its own window and keeps all data on the local machine.
2. **Server**. The same application run as a web server, for example alongside the
   network storage it scans. The UI is served in a browser.
3. **Docker**. The server, containerised, to run as a service next to network
   drives.

The user interface is a React single page app, served by the application itself.
A legacy server-rendered UI is also present during the transition.

## Building

Requires **JDK 17**, **Maven**, and **Node 20+** (Maven downloads its own Node for
the build; a local install is only needed for frontend development).

```bash
# Build everything: React frontend, tests, static analysis, and the runnable jar.
mvn clean verify
```

`mvn clean verify` is the full build. It runs the tests and SpotBugs; `mvn clean
package` builds the jar without them. Add `-DskipFrontend=true` to build the
backend alone, without Node.

### Desktop installers

```bash
./build-desktop.sh
```

Produces a native installer in `target/desktop` for the OS it runs on
(`.msi` on Windows, `.dmg` on macOS, `.deb` or `.rpm` on Linux). jpackage cannot
cross-compile, so each platform is built on its own machine; the GitHub workflow
does this on a tag.

## Running the server

```bash
java -jar target/open-fixity-0.1.0-ALPHA.jar server dev-server.yml
```

The React app is served at <http://localhost:8080/app>. Configuration lives in
`dev-server.yml`; data is persisted to an embedded H2 database under
`~/.openfixity`.

## Architecture

| Package | Responsibility |
| --- | --- |
| `core/digests` | Digest algorithms (MD2 to SHA3-512), hashing, digest results |
| `core/paths` | Path scanning, file scan results, path summaries |
| `apps/dao` | JPA entities and DAOs for collections, paths, registrations, scans |
| `apps/schedule` | Quartz jobs, schedule manager, batch scanner |
| `apps/server` | Dropwizard application, REST API, and the desktop launcher |
| `frontend/` | React single page app (Vite, TanStack Query, Tailwind) |

Java 17, Dropwizard 5, Hibernate 6, embedded H2, Quartz, JavaFX for the desktop
window, and a React frontend embedded in the jar.

## Licence

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).

Copyright © 2026 Open Preservation Foundation.
