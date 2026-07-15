# Running OpenFixity with Docker

The **desktop app is the primary way to run OpenFixity** (see the
[README](README.md) and [INSTALL.md](INSTALL.md)). This page is for the other case:
running OpenFixity as a long-lived **service**, typically on a machine that sits
next to the network storage you want to scan, so scheduled scans run without anyone
needing to keep an app open.

If you are a desktop user, you can ignore this file entirely.

## Quick start

You need Docker with Compose.

```bash
docker compose up -d
```

Then open <http://127.0.0.1:8080/app>.

```bash
docker compose logs -f      # watch the logs
docker compose down         # stop it (your data is kept)
```

The first run builds the image, which takes a few minutes; later runs are instant.

## Scanning your files

OpenFixity scans files it can see inside the container, so mount the directories you
want to check. Edit `docker-compose.yml` and uncomment the example volume, pointing
it at your storage:

```yaml
    volumes:
      - openfixity-data:/data
      - /srv/collections:/scan:ro     # your files, read-only, visible as /scan
```

Then in the UI, browse to `/scan` and register it. Mount as many directories as you
like. The `:ro` (read-only) is deliberate: OpenFixity only ever reads the files it
checks, never writes to them.

## Keeping your data

The H2 database (collections, scans, digests, schedules) and the logs live in the
`openfixity-data` named volume, mounted at `/data`. It survives
`docker compose down`. To start completely fresh:

```bash
docker compose down -v      # -v also deletes the data volume
```

To back up the data, copy the volume's contents, for example:

```bash
docker run --rm -v openfixity-data:/data -v "$PWD":/backup alpine \
  tar czf /backup/openfixity-data.tgz -C /data .
```

## Security

OpenFixity has **no built-in authentication**. Anyone who can reach its port has
full control, including browsing the container's filesystem.

The provided `docker-compose.yml` publishes the port to **`127.0.0.1` only**, so by
default it is reachable only from the host machine. This is the safe default.

To make it available to other people:

- Put an **authenticating reverse proxy** (nginx, Caddy, Traefik, with HTTP auth or
  SSO) in front of it, or
- Only expose it on a **trusted, isolated network**, and understand that anyone on
  that network has full access.

Do not change the port mapping to `8080:8080` (all interfaces) on a shared network
without a proxy.

## Ports

| Port | Purpose |
| --- | --- |
| 8080 | Web UI (`/app`) and REST API (`/api`) |
| 8081 | Dropwizard admin and health check |

## Building the image yourself

```bash
docker build -t openfixity:local .
```

The build compiles the React frontend and the Java server into a single jar (the
Maven build downloads its own Node), then packages it on a small JRE base image. No
local Java, Maven, or Node install is needed to build the image.

## Configuration

The container runs with [docker-server.yml](docker-server.yml), which binds to
`0.0.0.0` (so the published port works) and stores data under `/data`. To change
settings, edit that file and rebuild, or mount your own config over
`/app/server.yml`.
