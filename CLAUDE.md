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

- **Deploy needs GCP secrets**: `.github/workflows/deploy.yml` runs on push to `main` — test job, then build+push image to Artifact Registry via Workload Identity. Requires repo secrets `GCP_PROJECT_ID`, `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_SERVICE_ACCOUNT`. Unset → deploy job fails at GCP auth (test job still passes).
- **SVG parity**: the backend generator reproduces the original JS generator byte-for-byte using `StrictMath.sin` (fdlibm). Golden-file tests in `backend/src/test/resources/golden-*.svg` assert this across seed/shape/size. Any change to generator math must keep these tests green — do not swap `StrictMath` for `Math`.
- **Static-export build**: `frontend` uses `output: "export"` (`next.config.mjs`). `npm run build` emits `out/`, which the multi-stage `Dockerfile` copies into `backend/src/main/resources/static/`. The combined jar serves frontend at `/` and API at `/api/`.
- **H2 resets on restart**: in-memory H2 with DDL auto=`create` — DB is wiped every startup. Don't rely on persisted data; swap datasource to Postgres in `application.properties` for prod.
- **Dev port proxy**: in dev, backend is 8080 and frontend is 3000; frontend reaches the API via `NEXT_PUBLIC_API_BASE` (`.env.development`). In the container it's a single port 8080.

## Conventions

- Commits: Conventional Commits (`feat:`, `fix:`, `chore:`…), feature branches, PR before merge.
- Linting: frontend ESLint (`npm run lint`), backend Spotless/google-java-format (`mvn spotless:check`). Run before committing. Frontend TS is `strict`. Path alias `@/*` → frontend root.

## Panel shapes: square tray vs round coaster

The puzzle can be cut as the original **square** (rounded-rectangle) tray or as a **round coaster** disc. Square is the default everywhere, so existing clients, saved products, and the golden-file tests are unaffected.

- **`generator/PanelShape`** — enum `SQUARE` | `CIRCLE`.
- **`generator/PuzzleSpec`** — adds `panelShape` and `panelDiameter` (default 110 mm). The original 10-arg constructor is kept and delegates with `SQUARE`, so nothing that constructed a spec before needs to change. `widthMm()` / `heightMm()` return the diameter for a circular panel.
- **`api/PuzzleRequest`** — adds optional `panelShape` and `panelDiameter`; when omitted in the JSON body they default to `SQUARE` / 110 mm (Jackson leaves missing record components null/0, normalised in `toSpec()`).
- **`generator/CircleFractalJigsaw`** — **all original methods are left byte-for-byte unchanged.** Panel support is added as overloads of `createFrame` and the four `exportSvg*` methods that take `(…, PanelShape panel, double diameter)`. For `SQUARE` they delegate to the originals (identical output → golden tests stay green). For `CIRCLE`:
  - The grid is centred on the disc; the SVG canvas is a square sized to the diameter with a viewBox centred on the disc (`svgHeaderCircle`).
  - **Frame size = border-ring width.** Pieces are clipped to an inner circle of radius `diameter/2 − frame`; both the inner frame edge (when `frame > 0`) and the outer coaster edge (radius `diameter/2`) are emitted as real cut paths (`circlePanelFrameElements`), giving a solid frame annulus.
  - The SVG `clipPath` (id `panel-clip`) masks the pieces to the inner disc; the inner circle is also a real cut line so the ring is a true annulus.

API examples:

```jsonc
// square (panelShape omitted → default)
{ "seed":42, "ncols":8, "nrows":8, "tileRadius":6, "frame":6, "frameCorner":4,
  "minPieceSize":4, "maxPieceSize":12, "shape":"CIRCULAR" }

// round 110 mm coaster
{ "seed":42, "ncols":8, "nrows":8, "tileRadius":6, "frame":6, "frameCorner":4,
  "minPieceSize":4, "maxPieceSize":12, "shape":"CIRCULAR",
  "panelShape":"CIRCLE", "panelDiameter":110 }
```

## Bright acrylic piece colouring

The 2D preview, 3D preview, and the `COLORED` solution-sheet export all use a shared **bright acrylic palette** with adjacency-aware colouring: no two touching pieces share a colour, and the outer edge / frame / baseboard is always **Bark Brown** (`#4A3526`).

- **`frontend/lib/palette.ts`** — `BARK_BROWN`, the 15-colour `ACRYLIC_PIECE_COLORS`, and `assignPieceColors(piecePaths)`. The latter parses each piece's path `d` for its boundary vertices, builds a piece-adjacency graph (pieces sharing any vertex), and greedily colours it (least-used colour first) so adjacent pieces differ.
- **`backend/.../CircleFractalJigsaw`** — `assignPieceColors(frame, rad)` is a **direct port** of the frontend logic, using the same palette and order. Because a piece outline is a continuous arc chain, the backend's set of arc endpoints equals the vertex set the frontend extracts from the path string, so both produce the **identical colourway** — the downloaded `COLORED` sheet matches the on-screen preview piece-for-piece. Both `exportSvgColored` overloads fill pieces with the assigned colours and stroke them / the frame in Bark Brown (`coloredFrameElement`, `coloredCirclePanelFrameElements`).

### Colouring gotchas — do not break these

- **Only the `COLORED` export is colourised.** The cut-file exports (`OVERLAP`, `NON_OVERLAP`, `NON_OVERLAP_SINGLE_PATH`) keep black `0.1` hairlines — correct for laser/CNC — and feed the golden-file parity tests. Don't recolour them.
- **`COLORED` emits exactly one `fill="#rrggbb"` per piece** (Bark Brown is a `stroke`, not a `fill`); the `coloredExportEmitsValidSixDigitHexColors` test counts on that.
- Keep `palette.ts` and `CircleFractalJigsaw.assignPieceColors` in sync (same palette array, same greedy rule) or the preview and the printed sheet will drift apart.

## Designer UI (frontend)

- **`lib/types.ts`** — `PanelShape`, plus `panelShape` / `panelDiameter` on `PuzzleParams`; `DEFAULT_PARAMS` is a square tray and `productToParams` keeps catalogue products square.
- **`app/designer/page.tsx`** — a **Panel shape** selector (Square tray / Round coaster) and a **Coaster diameter (mm)** field when circular; frame-corner radius is disabled for circles. Disc geometry (centre, inner/outer radius, viewBox, clip path) and the adjacency-aware `pieceColors` are memoised and fed to both previews.
- **`components/PuzzlePreview.tsx`** (2D) — optional `viewBox`, `clipPathD`, and `pieceColors`. Circular panels use a disc-centred viewBox, clip pieces to the inner circle, stroke the inner frame edge, and fill the frame in Bark Brown.
- **`components/Puzzle3D.tsx`** (3D) — optional `clip` and `pieceColors`. For a coaster the baseboard is a disc (cylinder) and the extruded pieces are clipped to the puzzle circle with a ring of 96 radial clipping planes (`renderer.localClippingEnabled`). The group is centred on the **disc centre** (not the canvas centre) so OrbitControls orbits the middle of the coaster, not its edge. Baseboard is Bark Brown; pieces use the bright `pieceColors`.
