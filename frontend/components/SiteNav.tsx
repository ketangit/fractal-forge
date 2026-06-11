"use client";

import Link from "next/link";
import { useCart } from "./CartContext";

export default function SiteNav() {
  const { count } = useCart();
  return (
    <header className="site-header">
      <div className="inner">
        <Link href="/" className="brand">
          Fractal<span>Forge</span>
        </Link>
        <nav className="nav" aria-label="Main navigation">
          <Link href="/shop">Shop</Link>
          <Link href="/designer">Designer</Link>
          <Link href="/cart">
            Cart{count > 0 && <span className="cart-badge">{count}</span>}
          </Link>
        </nav>
      </div>
    </header>
  );
}
