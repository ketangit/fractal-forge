/**
 * Bright acrylic palette for piece previews.
 *
 * Outer edge / tray always uses Bark Brown; interior pieces use the vivid
 * colours below. {@link assignPieceColors} greedily colours pieces so that no
 * two touching pieces share a colour (a map-colouring over the piece adjacency
 * graph derived from shared boundary vertices).
 */

/** Folkfirt — Bark Brown (2907CA). Always the outer edge / baseboard. */
export const BARK_BROWN = "#4A3526";

/** Vivid interior colours (acrylic paints), ordered for high adjacent contrast. */
export const ACRYLIC_PIECE_COLORS = [
  "#F7831A", // Folkfirt Pure Orange (2903CA)
  "#2EB24A", // Folkfirt Bright Green (2950CA)
  "#2350B5", // Craft Smart Cobalt Blue
  "#FFD21A", // DecoArt Bright Yellow
  "#7B2D8E", // Folkfirt Purple (2977CA)
  "#1BB6C1", // Craft Smart Turquoise
  "#D61F3A", // Folkfirt Lipstick Red (7270)
  "#A6CE39", // Folkfirt Lime Green (2914CA)
  "#D43F7C", // Craft Smart Raspberry Rose
  "#1487A6", // Folkfirt Blue Peacock (2387)
  "#A6477F", // Folkfirt Juneberry (2386)
  "#4FD1C5", // Craft Smart Aquamarine
  "#8A2F4A", // Craft Smart French Wine
  "#20308F", // Craft Smart Sapphire
  "#E22128", // Craft Smart Red
];

const PALETTE = ACRYLIC_PIECE_COLORS;

/** Deterministic fallback colour by index (used when no adjacency map is given). */
export function pieceColor(index: number): string {
  return PALETTE[index % PALETTE.length];
}

/** Boundary vertices of one piece's SVG path "d" (quantised so shared points match). */
function pieceVertices(d: string): string[] {
  const tk = d
    .replace(/([A-Za-z])/g, " $1 ")
    .replace(/,/g, " ")
    .trim()
    .split(/\s+/);
  const verts: string[] = [];
  const push = (x: number, y: number) => {
    if (Number.isFinite(x) && Number.isFinite(y)) {
      verts.push(`${Math.round(x * 1000)}:${Math.round(y * 1000)}`);
    }
  };
  let i = 0;
  while (i < tk.length) {
    const c = tk[i++].toUpperCase();
    if (c === "M" || c === "L" || c === "T") {
      push(+tk[i], +tk[i + 1]);
      i += 2;
    } else if (c === "A") {
      // rx ry xrot large sweep x y → the point is the final pair
      push(+tk[i + 5], +tk[i + 6]);
      i += 7;
    } else if (c === "C") {
      push(+tk[i + 4], +tk[i + 5]);
      i += 6;
    } else if (c === "Q" || c === "S") {
      push(+tk[i + 2], +tk[i + 3]);
      i += 4;
    } else if (c === "H" || c === "V") {
      i += 1;
    }
    // Z and unknown tokens carry no coordinates
  }
  return verts;
}

/**
 * Assigns a bright colour to every piece such that adjacent pieces (those that
 * share at least one boundary vertex) never share a colour. Greedy graph
 * colouring in generation order, biased toward the least-used colour so the
 * whole palette gets exercised.
 */
export function assignPieceColors(piecePaths: string[]): string[] {
  const n = piecePaths.length;
  const verts = piecePaths.map(pieceVertices);

  const byVertex = new Map<string, number[]>();
  verts.forEach((vs, i) => {
    for (const v of new Set(vs)) {
      const arr = byVertex.get(v);
      if (arr) arr.push(i);
      else byVertex.set(v, [i]);
    }
  });

  const adj: Array<Set<number>> = Array.from({ length: n }, () => new Set<number>());
  for (const arr of byVertex.values()) {
    if (arr.length < 2) continue;
    for (let a = 0; a < arr.length; a++) {
      for (let b = a + 1; b < arr.length; b++) {
        adj[arr[a]].add(arr[b]);
        adj[arr[b]].add(arr[a]);
      }
    }
  }

  const C = ACRYLIC_PIECE_COLORS.length;
  const colorIdx = new Array<number>(n).fill(-1);
  const usage = new Array<number>(C).fill(0);

  for (let i = 0; i < n; i++) {
    const banned = new Set<number>();
    for (const nb of adj[i]) if (colorIdx[nb] >= 0) banned.add(colorIdx[nb]);

    let best = -1;
    for (let k = 0; k < C; k++) {
      if (banned.has(k)) continue;
      if (best === -1 || usage[k] < usage[best]) best = k;
    }
    if (best === -1) {
      // More neighbours than colours: fall back to the globally least-used.
      best = 0;
      for (let k = 1; k < C; k++) if (usage[k] < usage[best]) best = k;
    }
    colorIdx[i] = best;
    usage[best]++;
  }

  return colorIdx.map((k) => ACRYLIC_PIECE_COLORS[k]);
}
