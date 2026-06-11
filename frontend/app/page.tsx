"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import ProductCard from "@/components/ProductCard";
import { fetchProducts } from "@/lib/api";
import type { Product } from "@/lib/types";

export default function HomePage() {
  const [featured, setFeatured] = useState<Product[]>([]);

  useEffect(() => {
    fetchProducts()
      .then((all) => setFeatured(all.slice(0, 3)))
      .catch(() => setFeatured([]));
  }, []);

  return (
    <>
      <section className="hero">
        <div>
          <h1>
            Jigsaw puzzles grown by an algorithm,
            <br /> cut by a laser.
          </h1>
          <p className="muted" style={{ fontSize: "1.05rem" }}>
            Every FractalForge puzzle is procedurally generated — winding, dragon-curve-like pieces
            that interlock like nothing you have solved before. Pick a classic from the shop, or
            design your own from scratch and preview it in 3D before you buy.
          </p>
          <div className="row" style={{ marginTop: 18 }}>
            <Link href="/designer" className="button">
              Design your own
            </Link>
            <Link href="/shop" className="button secondary">
              Browse the shop
            </Link>
          </div>
        </div>
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Why fractal puzzles?</h3>
          <p className="muted">
            No corners, no edge-sorting, no straight lines. Pieces snake across the board and every
            single puzzle is unique — flip on non-deterministic mode and even we could not cut the
            same one twice.
          </p>
          <table className="spec-table">
            <tbody>
              <tr><td>Materials</td><td>Birch ply, walnut, acrylic</td></tr>
              <tr><td>Piece style</td><td>Circular, octagonal or square tiles</td></tr>
              <tr><td>Cut files</td><td>Laser (non-overlap) &amp; CNC (contoured)</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <h2>Featured puzzles</h2>
      <div className="grid">
        {featured.map((p) => (
          <ProductCard key={p.id} product={p} />
        ))}
        {featured.length === 0 && (
          <p className="muted">Catalog is loading (is the backend running?)</p>
        )}
      </div>
    </>
  );
}
