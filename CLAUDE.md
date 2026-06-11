# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Full-stack puzzle generator/shop. Monorepo, two modules:
- `backend/` — Java 25, Spring Boot 4.0, Maven. API + fractal SVG generator + shop.
- `frontend/` — Next.js 15.5 / React 19, TypeScript, npm. Static-export mode.

Ships as a single Docker container: built frontend export is served by the backend jar.

## Commands

Backend (run from `backend/`):
- `mvn spring-boot:run` — run on port 8080
- `mvn test` — JUnit (golden-file parity, determinism, API)
- `mvn package` — build jar
- `mvn spotless:check` — verify Java formatting; `mvn spotless:apply` to fix

Frontend (run from `frontend/`):
- `npm run dev` — dev server on port 3000
- `npm run build` — static export to `out/`
- `npm test` — Vitest; `npm run test:watch` for watch mode
- `npm run lint` — ESLint (Next.js config); run `npm install` once to pull new lint deps

Full stack: `docker compose up --build` (port 8080).

## Gotchas — do not break these

- **SVG parity**: the backend generator reproduces the original JS generator byte-for-byte using `StrictMath.sin` (fdlibm). Golden-file tests in `backend/src/test/resources/golden-*.svg` assert this across seed/shape/size. Any change to generator math must keep these tests green — do not swap `StrictMath` for `Math`.
- **Static-export build**: `frontend` uses `output: "export"` (`next.config.mjs`). `npm run build` emits `out/`, which the multi-stage `Dockerfile` copies into `backend/src/main/resources/static/`. The combined jar serves frontend at `/` and API at `/api/`.
- **H2 resets on restart**: in-memory H2 with DDL auto=`create` — DB is wiped every startup. Don't rely on persisted data; swap datasource to Postgres in `application.properties` for prod.
- **Dev port proxy**: in dev, backend is 8080 and frontend is 3000; frontend reaches the API via `NEXT_PUBLIC_API_BASE` (`.env.development`). In the container it's a single port 8080.

## Conventions

- Commits: Conventional Commits (`feat:`, `fix:`, `chore:`…), feature branches, PR before merge.
- Linting: frontend ESLint (`npm run lint`), backend Spotless/google-java-format (`mvn spotless:check`). Run before committing. Frontend TS is `strict`. Path alias `@/*` → frontend root.
