# OpenFixity frontend

The OpenFixity user interface: a React single page app built with Vite, TanStack
Query, and Tailwind. It is served by the OpenFixity Java application at `/app`; the
Maven build compiles it and embeds it in the jar, so it is not deployed separately.

For the project as a whole, see the [top-level README](../README.md).

## Development

```bash
npm install
npm run dev
```

`npm run dev` starts Vite with hot reload. It proxies `/api` and `/scans` to the
OpenFixity Java backend on `http://localhost:8080`, so run the backend alongside it:

```bash
# from the repository root
java -jar target/open-fixity-0.1.1-ALPHA.jar server dev-server.yml
```

The proxy targets are configured in `vite.config.ts`.

## Build

```bash
npm run build   # outputs to dist/
```

You rarely need to run this directly. `mvn clean package` at the repository root
builds the frontend and copies `dist/` into the jar automatically.

## Notes

- The canonical backend is the Java application in this repository. An earlier Go
  implementation was only ever a stand-in used while the Java backend was being
  built, and is not part of this project.
- The app is bundled to run offline: fonts and all assets are local, with no CDN or
  external requests, so it works on air-gapped workstations.

## Scripts

| Command | What it does |
| --- | --- |
| `npm run dev` | Vite dev server with hot reload, proxying to the backend |
| `npm run build` | Production build into `dist/` |
| `npm run preview` | Serve the production build locally |
| `npm run lint` | ESLint |
