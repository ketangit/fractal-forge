"use client";

import dynamic from "next/dynamic";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { useCart } from "@/components/CartContext";
import PuzzlePreview from "@/components/PuzzlePreview";
import { fetchProduct, generatePuzzle } from "@/lib/api";
import { formatPrice } from "@/lib/cart";
import {
  MATERIAL_LABELS,
  productToParams,
  type GenerateResponse,
  type Product,
} from "@/lib/types";

const Puzzle3D = dynamic(() => import("@/components/Puzzle3D"), { ssr: false });

function ProductView() {
  const search = useSearchParams();
  const id = Number(search.get("id"));
  const cart = useCart();

  const [product, setProduct] = useState<Product | null>(null);
  const [preview, setPreview] = useState<GenerateResponse | null>(null);
  const [view, setView] = useState<"2d" | "3d">("3d");
  const [added, setAdded] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    fetchProduct(id)
      .then((p) => {
        setProduct(p);
        return generatePuzzle(productToParams(p));
      })
      .then(setPreview)
      .catch((e) => setError(e.message));
  }, [id]);

  if (error) return <div className="error">{error}</div>;
  if (!product) return <p className="muted">Loading product…</p>;

  const addToCart = () => {
    cart.add({
      kind: "product",
      productId: product.id,
      name: product.name,
      material: product.material,
      unitPriceCents: product.priceCents,
      quantity: 1,
      pieceCount: preview?.pieceCount,
    });
    setAdded(true);
    setTimeout(() => setAdded(false), 1800);
  };

  return (
    <div className="designer-layout">
      <div>
        <h1 style={{ marginTop: 0 }}>{product.name}</h1>
        <p className="muted">{product.description}</p>
        <table className="spec-table">
          <tbody>
            <tr>
              <td>Material</td>
              <td>{MATERIAL_LABELS[product.material]}</td>
            </tr>
            {preview && (
              <>
                <tr>
                  <td>Size</td>
                  <td>
                    {Math.round(preview.widthMm)} × {Math.round(preview.heightMm)} mm
                  </td>
                </tr>
                <tr>
                  <td>Pieces</td>
                  <td>{preview.pieceCount}</td>
                </tr>
              </>
            )}
            <tr>
              <td>Tile style</td>
              <td>{product.shape.toLowerCase()}</td>
            </tr>
          </tbody>
        </table>
        <div className="row" style={{ marginTop: 18 }}>
          <span className="price">{formatPrice(product.priceCents)}</span>
          <button className="button" onClick={addToCart} disabled={!preview}>
            {added ? "Added ✓" : "Add to cart"}
          </button>
        </div>
        <p className="muted" style={{ marginTop: 14 }}>
          Rotate the 3D preview with your mouse or finger — this is the exact puzzle you will
          receive, rendered from its generator seed.
        </p>
      </div>

      <div>
        <div className="row" style={{ marginBottom: 10, justifyContent: "flex-end" }}>
          <div className="toggle-group" role="tablist" aria-label="Preview mode">
            <button className={view === "2d" ? "active" : ""} onClick={() => setView("2d")}>
              2D
            </button>
            <button className={view === "3d" ? "active" : ""} onClick={() => setView("3d")}>
              3D
            </button>
          </div>
        </div>
        <div className="preview-frame">
          {!preview && <div style={{ aspectRatio: "1", color: "#9c8d7c", display: "grid", placeItems: "center" }}>Generating preview…</div>}
          {preview && view === "2d" && (
            <PuzzlePreview
              widthMm={preview.widthMm}
              heightMm={preview.heightMm}
              piecePaths={preview.piecePaths}
              framePath={preview.framePath}
            />
          )}
          {preview && view === "3d" && (
            <Puzzle3D
              widthMm={preview.widthMm}
              heightMm={preview.heightMm}
              piecePaths={preview.piecePaths}
              material={product.material}
              thickness={product.material === "WALNUT" ? 4 : 3}
            />
          )}
        </div>
      </div>
    </div>
  );
}

export default function ProductPage() {
  return (
    <Suspense fallback={<p className="muted">Loading…</p>}>
      <ProductView />
    </Suspense>
  );
}
