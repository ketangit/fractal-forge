import { describe, expect, it } from "vitest";
import {
  addItem,
  cartCount,
  cartTotalCents,
  formatPrice,
  itemKey,
  removeItem,
  setQuantity,
} from "@/lib/cart";
import { DEFAULT_PARAMS, type CartItem } from "@/lib/types";

const productItem: Omit<CartItem, "key"> = {
  kind: "product",
  productId: 1,
  name: "Dragon Classic",
  material: "BIRCH_PLY",
  unitPriceCents: 4900,
  quantity: 1,
};

const customItem: Omit<CartItem, "key"> = {
  kind: "custom",
  name: "Custom puzzle",
  material: "WALNUT",
  unitPriceCents: 7300,
  quantity: 1,
  params: { ...DEFAULT_PARAMS, seed: 1234 },
};

describe("cart operations", () => {
  it("adds new items with a stable key", () => {
    const items = addItem([], productItem);
    expect(items).toHaveLength(1);
    expect(items[0].key).toBe("product-1-BIRCH_PLY");
  });

  it("merges duplicate items by incrementing quantity", () => {
    let items = addItem([], productItem);
    items = addItem(items, productItem);
    expect(items).toHaveLength(1);
    expect(items[0].quantity).toBe(2);
  });

  it("keeps custom puzzles with different seeds separate", () => {
    let items = addItem([], customItem);
    items = addItem(items, {
      ...customItem,
      params: { ...DEFAULT_PARAMS, seed: 9999 },
    });
    expect(items).toHaveLength(2);
  });

  it("treats same product in different material as distinct", () => {
    expect(itemKey(productItem)).not.toBe(
      itemKey({ ...productItem, material: "ACRYLIC_CLEAR" }),
    );
  });

  it("computes totals and counts", () => {
    let items = addItem([], productItem);
    items = addItem(items, customItem);
    items = setQuantity(items, items[0].key, 3);
    expect(cartTotalCents(items)).toBe(3 * 4900 + 7300);
    expect(cartCount(items)).toBe(4);
  });

  it("removes items when quantity drops to zero", () => {
    let items = addItem([], productItem);
    items = setQuantity(items, items[0].key, 0);
    expect(items).toHaveLength(0);
  });

  it("removes items by key", () => {
    let items = addItem([], productItem);
    items = removeItem(items, items[0].key);
    expect(items).toHaveLength(0);
  });

  it("caps quantity at 50 (matching the API limit)", () => {
    let items = addItem([], { ...productItem, quantity: 49 });
    items = addItem(items, { ...productItem, quantity: 10 });
    expect(items[0].quantity).toBe(50);
  });

  it("formats prices as USD", () => {
    expect(formatPrice(4900)).toBe("$49.00");
  });
});
