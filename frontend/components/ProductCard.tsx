"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { generatePuzzle } from "@/lib/api";
import { formatPrice } from "@/lib/cart";
import { MATERIAL_LABELS, productToParams, type GenerateResponse, type Product } from "@/lib/types";
import PuzzlePreview from "./PuzzlePreview";

export default function ProductCard({ product }: { product: Product }) {
  const [preview, setPreview] = useState<GenerateResponse | null>(null);

  useEffect(() => {
    let cancelled = false;
    generatePuzzle(productToParams(product))
      .then((res) => {
        if (!cancelled) setPreview(res);
      })
      .catch(() => {
        /* preview is decorative; the product page retries */
      });
    return () => {
      cancelled = true;
    };
  }, [product]);

  return (
    <Link href={`/product?id=${product.id}`} className="card" style={{ display: "block" }}>
      <div className="preview-frame" style={{ marginBottom: 12 }}>
        {preview ? (
          <PuzzlePreview
            widthMm={preview.widthMm}
            heightMm={preview.heightMm}
            piecePaths={preview.piecePaths}
            framePath={preview.framePath}
          />
        ) : (
          <div style={{ aspectRatio: "1", display: "grid", placeItems: "center", color: "#9c8d7c" }}>
            Generating…
          </div>
        )}
      </div>
      <strong>{product.name}</strong>
      <div className="muted">{MATERIAL_LABELS[product.material]}</div>
      {preview && <div className="muted">{preview.pieceCount} pieces</div>}
      <div className="price" style={{ marginTop: 6 }}>
        {formatPrice(product.priceCents)}
      </div>
    </Link>
  );
}
