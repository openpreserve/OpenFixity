# Open Fixity

**Open Fixity** monitors the integrity of digital collections. It scans files on
local disks, removable storage, and network locations, and reports whether anything
has been changed, added, removed, moved, or renamed. Scans run on a schedule, so
problems surface early.

It runs as a server with a web interface, or locally on a workstation in the way the
desktop application always did.

Open Fixity is the successor to Fixity Pro, originally developed by AVP and since
transferred to the Open Preservation Foundation. See
[ACKNOWLEDGEMENTS.md](ACKNOWLEDGEMENTS.md) for the project's history, and for the
people it is owed to.

> **Status: pre-alpha.** Not yet ready for use against collections you care about.

## Building

Requires JDK 17 and Maven.

```bash
mvn clean package
```

## Running

```bash
java -jar target/open-fixity-1.0.0-ALPHA.jar server dev-server.yml
```

The web application is served at <http://localhost:8080>. Configuration lives in
`dev-server.yml`. Data is persisted to an embedded H2 database at
`~/.openfixity/open-fixity`.

## Architecture

| Package | Responsibility |
| --- | --- |
| `core/digests` | Digest algorithms (MD2 to SHA3-512), hashing, digest results |
| `core/paths` | Path scanning, file scan results, path summaries |
| `apps/dao` | JPA entities and DAOs for collections, paths, registrations, scans |
| `apps/schedule` | Quartz jobs, schedule manager, batch scanner |
| `apps/server` | Dropwizard application, REST API, Mustache views |

Java 17, Dropwizard 5, Hibernate 6, embedded H2, Quartz.

## Licence

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).

Copyright © 2026 Open Preservation Foundation.
