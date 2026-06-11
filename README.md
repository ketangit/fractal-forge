# FractalForge — fractal jigsaw puzzle generator & shop

Modernized rewrite of the original single-file fractal jigsaw generator
(`index.html` + `flatten.js`) as a cloud-native web shop:

- **Backend** — Java 25, Spring Boot 4 (`backend/`): faithful port of the
  generation algorithm, SVG export API, product catalog and mock-checkout
  orders (H2 in-memory).
- **Frontend** — React 19 / Next.js 15, static export (`frontend/`): puzzle
  designer, storefront, cart, and an interactive **three.js 3D preview** so
  customers can inspect a puzzle before buying. Responsive, works on mobile
  and desktop browsers.
- **Deployment** — one container: the Next.js export is baked into the Spring
  Boot jar and everything is served from port 8080.

## Run it

```bash
docker compose up --build        # then open http://localhost:8080
```

(or `docker build -t fractal-puzzle . && docker run -p 8080:8080 fractal-puzzle`)

### Local development

```bash
# terminal 1 — backend on :8080
cd backend && mvn spring-boot:run

# terminal 2 — frontend on :3000 (proxies API to :8080 via NEXT_PUBLIC_API_BASE)
cd frontend && npm install && npm run dev
```

### Tests

```bash
cd backend && mvn test          # JUnit: golden-file parity, determinism, API
cd frontend && npm test         # Vitest: cart logic, API client, components
```

## Algorithm parity with the reference generator

The Java port reproduces the original JavaScript generator **byte-for-byte**:

- The seeded PRNG uses `StrictMath.sin` (fdlibm), which matches the `Math.sin`
  used by V8/SpiderMonkey — so a seed produces the identical puzzle the reference
  page produced.
- Parity was verified by diffing all four SVG export modes across dozens of
  seed/shape/size combinations against the reference code running in Node
  (38/38 byte-identical after numeric canonicalization), including
  single-piece "lampshade" mode.
- Golden files captured from the reference implementation are asserted in
  `CircleFractalJigsawTest` (`src/test/resources/golden-*.svg`).

## API

| Endpoint | Description |
|---|---|
| `POST /api/puzzle/generate` | Generate; returns piece paths, frame path, piece count, seed used, price quotes per material |
| `POST /api/puzzle/export?mode=…` | Download SVG. Modes: `OVERLAP` (individually contoured pieces — CNC), `NON_OVERLAP` (each edge once — laser), `NON_OVERLAP_SINGLE_PATH` (laser, Trotec-friendly), `COLORED` (solution sheet) |
| `GET /api/products`, `GET /api/products/{id}` | Catalog |
| `POST /api/orders`, `GET /api/orders/{id}` | Mock checkout (prices recomputed server-side) |
| `GET /actuator/health` | Liveness/readiness |

## Non-deterministic mode

Like the original "use non-deterministic randomness" checkbox, but better
behaved: the backend draws a fresh seed from OS entropy (`SecureRandom`) per
generation, so every click is unique — and the drawn seed is returned, so the
exact previewed puzzle can still be exported as a cut file and ordered.

## Deliberate changes from the reference code

- Colored-export hex colors are zero-padded (the original could emit invalid
  5-digit colors like `#1a2b3`).
- The custom SVG border upload was dropped from v1 (it depended on browser
  DOM APIs); rectangular frames with corner radius are fully supported.
- Coordinates are limited to 6 decimal places (sub-µm) in SVG output.

## Notes

- Orders/products live in H2 **in-memory** — data resets on restart. Swap the
  datasource in `application.properties` for Postgres in production.
- Pricing: base + per-piece + per-cm², with material multipliers
  (`PricingService`).
