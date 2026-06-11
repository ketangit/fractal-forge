import type { CartItem } from "./types";

/** Pure cart operations (unit-tested; the React context is a thin wrapper). */

export function itemKey(item: Omit<CartItem, "key">): string {
  if (item.kind === "product") {
    return `product-${item.productId}-${item.material}`;
  }
  const p = item.params!;
  return `custom-${p.seed}-${p.ncols}x${p.nrows}-${p.tileRadius}-${p.frame}-${p.frameCorner}-${p.minPieceSize}-${p.maxPieceSize}-${p.shape}-${item.material}`;
}

export function addItem(items: CartItem[], incoming: Omit<CartItem, "key">): CartItem[] {
  const key = itemKey(incoming);
  const existing = items.find((i) => i.key === key);
  if (existing) {
    return items.map((i) =>
      i.key === key ? { ...i, quantity: Math.min(i.quantity + incoming.quantity, 50) } : i,
    );
  }
  return [...items, { ...incoming, key }];
}

export function removeItem(items: CartItem[], key: string): CartItem[] {
  return items.filter((i) => i.key !== key);
}

export function setQuantity(items: CartItem[], key: string, quantity: number): CartItem[] {
  if (quantity <= 0) return removeItem(items, key);
  return items.map((i) => (i.key === key ? { ...i, quantity: Math.min(quantity, 50) } : i));
}

export function cartTotalCents(items: CartItem[]): number {
  return items.reduce((sum, i) => sum + i.unitPriceCents * i.quantity, 0);
}

export function cartCount(items: CartItem[]): number {
  return items.reduce((sum, i) => sum + i.quantity, 0);
}

export function formatPrice(cents: number, currency = "USD"): string {
  return new Intl.NumberFormat("en-US", { style: "currency", currency }).format(cents / 100);
}
