"use client";

import dynamic from "next/dynamic";
import { useCallback, useEffect, useRef, useState } from "react";
import { useCart } from "@/components/CartContext";
import PuzzlePreview from "@/components/PuzzlePreview";
import { downloadBlob, exportPuzzle, generatePuzzle } from "@/lib/api";
import { formatPrice } from "@/lib/cart";
import {
  DEFAULT_PARAMS,
  MATERIAL_LABELS,
  type ExportMode,
  type GenerateResponse,
  type Material,
  type PuzzleParams,
} from "@/lib/types";

const Puzzle3D = dynamic(() => import("@/components/Puzzle3D"), { ssr: false });

const EXPORTS: Array<{ mode: ExportMode; label: string; hint: string }> = [
  {
    mode: "NON_OVERLAP",
    label: "SVG · laser (non-overlapping)",
    hint: "Each edge cut once — feed straight to the laser cutter.",
  },
  {
    mode: "NON_OVERLAP_SINGLE_PATH",
    label: "SVG · laser, single path",
    hint: "Same cuts as one path element — plays nicer with Trotec controllers.",
  },
  {
    mode: "OVERLAP",
    label: "SVG · CNC (contoured pieces)",
    hint: "Every piece individually contoured — for milling or replacement pieces.",
  },
  {
    mode: "COLORED",
    label: "SVG · colored solution sheet",
    hint: "Filled pieces for the box cover or as the solution.",
  },
];

export default function DesignerPage() {
  const [params, setParams] = useState<PuzzleParams>(DEFAULT_PARAMS);
  const [result, setResult] = useState<GenerateResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [view, setView] = useState<"2d" | "3d">("2d");
  const [material, setMaterial] = useState<Material>("BIRCH_PLY");
  const [added, setAdded] = useState(false);
  const cart = useCart();
  const generationRef = useRef(0);

  const set = <K extends keyof PuzzleParams>(key: K, value: PuzzleParams[K]) =>
    setParams((p) => ({ ...p, [key]: value }));

  const generate = useCallback(async (p: PuzzleParams) => {
    const ticket = ++generationRef.current;
    setBusy(true);
    setError(null);
    try {
      const res = await generatePuzzle(p);
      if (ticket === generationRef.current) setResult(res);
    } catch (e) {
      if (ticket === generationRef.current) setError((e as Error).message);
    } finally {
      if (ticket === generationRef.current) setBusy(false);
    }
  }, []);

  useEffect(() => {
    generate(DEFAULT_PARAMS);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /** Pinned params reproducing exactly what is previewed (ND seed resolved). */
  const pinnedParams = (): PuzzleParams | null =>
    result ? { ...params, seed: result.seedUsed, nonDeterministic: false } : null;

  const download = async (mode: ExportMode) => {
    const pinned = pinnedParams();
    if (!pinned) return;
    try {
      const blob = await exportPuzzle(pinned, mode);
      downloadBlob(blob, `jigsaw-${mode.toLowerCase()}-seed${Math.round(pinned.seed)}.svg`);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const addToCart = () => {
    const pinned = pinnedParams();
    if (!pinned || !result) return;
    cart.add({
      kind: "custom",
      name: `Custom puzzle ${Math.round(result.widthMm)}×${Math.round(result.heightMm)}mm (seed ${Math.round(result.seedUsed)})`,
      material,
      unitPriceCents: result.priceCentsByMaterial[material],
      quantity: 1,
      params: pinned,
      pieceCount: result.pieceCount,
    });
    setAdded(true);
    setTimeout(() => setAdded(false), 1800);
  };

  const widthMm = params.ncols * 2 * params.tileRadius + 2 * params.frame;
  const heightMm = params.nrows * 2 * params.tileRadius + 2 * params.frame;

  return (
    <>
      <h1>Puzzle designer</h1>
      <p className="muted">
        The full generator from the original tool — now with live pricing, 3D preview and ordering.
      </p>
      <div className="designer-layout">
        <div className="card controls">
          <label>
            Seed
            <input
              type="number"
              value={params.seed}
              disabled={params.nonDeterministic}
              onChange={(e) => set("seed", Number(e.target.value) || 0)}
            />
            <input
              type="range"
              min={0}
              max={9999}
              value={Math.min(params.seed, 9999)}
              disabled={params.nonDeterministic}
              onChange={(e) => set("seed", Number(e.target.value))}
              aria-label="Seed slider"
            />
          </label>

          <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <input
              type="checkbox"
              checked={params.nonDeterministic}
              onChange={(e) => set("nonDeterministic", e.target.checked)}
            />
            Non-deterministic randomness
          </label>
          {params.nonDeterministic && (
            <p className="muted" style={{ margin: 0 }}>
              A fresh seed is drawn from OS entropy on every generation — each result is unique.
              The drawn seed is shown afterwards so your exact puzzle can still be exported and
              ordered.
            </p>
          )}

          <label>
            Tile shape
            <select
              value={params.shape}
              onChange={(e) => set("shape", e.target.value as PuzzleParams["shape"])}
            >
              <option value="CIRCULAR">Circular</option>
              <option value="OCTAGONAL">Octagonal</option>
              <option value="SQUARE">Square</option>
            </select>
          </label>

          <label>
            Columns: {params.ncols}
            <input type="range" min={2} max={250} value={params.ncols}
              onChange={(e) => set("ncols", Number(e.target.value))} />
          </label>
          <label>
            Rows: {params.nrows}
            <input type="range" min={2} max={250} value={params.nrows}
              onChange={(e) => set("nrows", Number(e.target.value))} />
          </label>
          <label>
            Tile radius (mm)
            <input type="number" step={0.5} min={1} value={params.tileRadius}
              onChange={(e) => set("tileRadius", Number(e.target.value) || 1)} />
          </label>
          <label>
            Frame size (mm)
            <input type="number" step={0.5} min={0} value={params.frame}
              onChange={(e) => set("frame", Number(e.target.value) || 0)} />
          </label>
          <label>
            Frame corner radius (mm)
            <input type="number" step={0.5} min={0} value={params.frameCorner}
              onChange={(e) => set("frameCorner", Number(e.target.value) || 0)} />
          </label>
          <label>
            Min piece size (tiles)
            <input type="number" min={2} value={params.minPieceSize}
              onChange={(e) => set("minPieceSize", Number(e.target.value) || 2)} />
          </label>
          <label>
            Max piece size (tiles)
            <input type="number" min={2} value={params.maxPieceSize}
              onChange={(e) => set("maxPieceSize", Number(e.target.value) || 2)} />
          </label>

          <p className="muted" style={{ margin: 0 }}>
            Finished size: {Math.round(widthMm)} × {Math.round(heightMm)} mm
          </p>

          <button className="button" onClick={() => generate(params)} disabled={busy}>
            {busy ? "Generating…" : "Generate jigsaw"}
          </button>
        </div>

        <div>
          <div className="row" style={{ marginBottom: 10, justifyContent: "space-between" }}>
            <div className="muted">
              {result && (
                <>
                  {result.pieceCount} pieces · seed {Math.round(result.seedUsed)}
                </>
              )}
            </div>
            <div className="toggle-group" role="tablist" aria-label="Preview mode">
              <button className={view === "2d" ? "active" : ""} onClick={() => setView("2d")}>
                2D
              </button>
              <button className={view === "3d" ? "active" : ""} onClick={() => setView("3d")}>
                3D
              </button>
            </div>
          </div>

          {error && <div className="error" style={{ marginBottom: 10 }}>{error}</div>}

          <div className="preview-frame">
            {!result && (
              <div style={{ aspectRatio: "1", color: "#9c8d7c", display: "grid", placeItems: "center" }}>
                {busy ? "Generating…" : "Press Generate"}
              </div>
            )}
            {result && view === "2d" && (
              <PuzzlePreview
                widthMm={result.widthMm}
                heightMm={result.heightMm}
                piecePaths={result.piecePaths}
                framePath={result.framePath}
              />
            )}
            {result && view === "3d" && (
              <Puzzle3D
                widthMm={result.widthMm}
                heightMm={result.heightMm}
                piecePaths={result.piecePaths}
                material={material}
                thickness={material === "WALNUT" ? 4 : 3}
              />
            )}
          </div>

          {result && (
            <>
              <div className="card" style={{ marginTop: 16 }}>
                <h3 style={{ marginTop: 0 }}>Order this puzzle</h3>
                <div className="row">
                  <select
                    value={material}
                    onChange={(e) => setMaterial(e.target.value as Material)}
                    aria-label="Material"
                    style={{ padding: "9px 10px", borderRadius: 8, border: "1px solid var(--line)" }}
                  >
                    {(Object.keys(MATERIAL_LABELS) as Material[]).map((m) => (
                      <option key={m} value={m}>
                        {MATERIAL_LABELS[m]} — {formatPrice(result.priceCentsByMaterial[m])}
                      </option>
                    ))}
                  </select>
                  <button className="button" onClick={addToCart}>
                    {added ? "Added ✓" : `Add to cart · ${formatPrice(result.priceCentsByMaterial[material])}`}
                  </button>
                </div>
              </div>

              <div className="card" style={{ marginTop: 16 }}>
                <h3 style={{ marginTop: 0 }}>Download cut files</h3>
                <div className="controls">
                  {EXPORTS.map((e) => (
                    <div className="row" key={e.mode} style={{ justifyContent: "space-between" }}>
                      <div style={{ flex: 1, minWidth: 220 }}>
                        <strong style={{ fontSize: "0.92rem" }}>{e.label}</strong>
                        <div className="muted">{e.hint}</div>
                      </div>
                      <button className="button secondary" onClick={() => download(e.mode)}>
                        Download
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </>
  );
}
