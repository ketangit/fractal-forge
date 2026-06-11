"use client";

import { useEffect, useState } from "react";
import ProductCard from "@/components/ProductCard";
import { fetchProducts } from "@/lib/api";
import type { Product } from "@/lib/types";

export default function ShopPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchProducts().then(setProducts).catch((e) => setError(e.message));
  }, []);

  return (
    <>
      <h1>The shop</h1>
      <p className="muted">
        Curated puzzles, generated from seeds we loved too much to let go. Each one is cut to order.
      </p>
      {error && <div className="error">{error}</div>}
      <div className="grid" style={{ marginTop: 16 }}>
        {products.map((p) => (
          <ProductCard key={p.id} product={p} />
        ))}
      </div>
    </>
  );
}
