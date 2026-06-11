"use client";

import React, { createContext, useContext, useEffect, useState } from "react";
import type { CartItem } from "@/lib/types";
import { addItem, cartCount, cartTotalCents, removeItem, setQuantity } from "@/lib/cart";

interface CartApi {
  items: CartItem[];
  count: number;
  totalCents: number;
  add: (item: Omit<CartItem, "key">) => void;
  remove: (key: string) => void;
  setQty: (key: string, quantity: number) => void;
  clear: () => void;
}

const CartContext = createContext<CartApi | null>(null);
const STORAGE_KEY = "fractal-puzzle-cart-v1";

export function CartProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (raw) setItems(JSON.parse(raw));
    } catch {
      /* corrupted cart: start fresh */
    }
    setHydrated(true);
  }, []);

  useEffect(() => {
    if (hydrated) {
      try {
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
      } catch {
        /* storage full or unavailable */
      }
    }
  }, [items, hydrated]);

  const api: CartApi = {
    items,
    count: cartCount(items),
    totalCents: cartTotalCents(items),
    add: (item) => setItems((prev) => addItem(prev, item)),
    remove: (key) => setItems((prev) => removeItem(prev, key)),
    setQty: (key, quantity) => setItems((prev) => setQuantity(prev, key, quantity)),
    clear: () => setItems([]),
  };

  return <CartContext.Provider value={api}>{children}</CartContext.Provider>;
}

export function useCart(): CartApi {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error("useCart must be used inside CartProvider");
  return ctx;
}
