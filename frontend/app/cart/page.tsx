"use client";

import Link from "next/link";
import { useState } from "react";
import { useCart } from "@/components/CartContext";
import { createOrder } from "@/lib/api";
import { formatPrice } from "@/lib/cart";
import { MATERIAL_LABELS, type OrderConfirmation } from "@/lib/types";

export default function CartPage() {
  const cart = useCart();
  const [form, setForm] = useState({
    customerName: "",
    email: "",
    addressLine: "",
    city: "",
    postalCode: "",
    country: "",
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmation, setConfirmation] = useState<OrderConfirmation | null>(null);

  const setField = (key: keyof typeof form, value: string) =>
    setForm((f) => ({ ...f, [key]: value }));

  const checkout = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const order = await createOrder({
        ...form,
        items: cart.items.map((i) =>
          i.kind === "product"
            ? { productId: i.productId, quantity: i.quantity }
            : { customSpec: i.params, material: i.material, quantity: i.quantity },
        ),
      });
      setConfirmation(order);
      cart.clear();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  if (confirmation) {
    return (
      <div className="success" style={{ maxWidth: 560, margin: "40px auto" }}>
        <h2 style={{ marginTop: 0 }}>Order placed 🎉</h2>
        <p>
          Order <strong>{confirmation.orderRef}</strong> — {confirmation.itemCount} item(s),{" "}
          <strong>{formatPrice(confirmation.totalCents)}</strong>. Status: {confirmation.status}.
        </p>
        <p className="muted">
          This demo has no payment provider, so your order is recorded as awaiting payment — nothing
          is charged or sent to the laser cutter until payment is confirmed.
        </p>
        <Link href="/shop" className="button">
          Keep browsing
        </Link>
      </div>
    );
  }

  if (cart.items.length === 0) {
    return (
      <div style={{ textAlign: "center", padding: "60px 0" }}>
        <h1>Your cart is empty</h1>
        <p className="muted">Find something fiendish in the shop, or design your own puzzle.</p>
        <div className="row" style={{ justifyContent: "center" }}>
          <Link href="/shop" className="button">Shop puzzles</Link>
          <Link href="/designer" className="button secondary">Open the designer</Link>
        </div>
      </div>
    );
  }

  return (
    <>
      <h1>Cart</h1>
      <div className="designer-layout">
        <div className="card" style={{ alignSelf: "start" }}>
          {cart.items.map((item) => (
            <div className="cart-line" key={item.key}>
              <div className="grow">
                <strong>{item.name}</strong>
                <div className="muted">
                  {MATERIAL_LABELS[item.material]}
                  {item.pieceCount ? ` · ${item.pieceCount} pieces` : ""}
                </div>
              </div>
              <input
                className="qty-input"
                type="number"
                min={0}
                max={50}
                value={item.quantity}
                aria-label={`Quantity for ${item.name}`}
                onChange={(e) => cart.setQty(item.key, Number(e.target.value))}
              />
              <div className="price">{formatPrice(item.unitPriceCents * item.quantity)}</div>
              <button
                className="button secondary"
                onClick={() => cart.remove(item.key)}
                aria-label={`Remove ${item.name}`}
              >
                ✕
              </button>
            </div>
          ))}
          <div className="row" style={{ justifyContent: "space-between", paddingTop: 14 }}>
            <strong>Total</strong>
            <span className="price">{formatPrice(cart.totalCents)}</span>
          </div>
        </div>

        <form className="card form" onSubmit={checkout}>
          <h3 style={{ margin: 0 }}>Delivery details</h3>
          <label>
            Full name
            <input required value={form.customerName}
              onChange={(e) => setField("customerName", e.target.value)} />
          </label>
          <label>
            Email
            <input required type="email" value={form.email}
              onChange={(e) => setField("email", e.target.value)} />
          </label>
          <label>
            Address
            <input required value={form.addressLine}
              onChange={(e) => setField("addressLine", e.target.value)} />
          </label>
          <div className="row">
            <label style={{ flex: 1 }}>
              City
              <input required value={form.city}
                onChange={(e) => setField("city", e.target.value)} />
            </label>
            <label style={{ width: 130 }}>
              Postal code
              <input required value={form.postalCode}
                onChange={(e) => setField("postalCode", e.target.value)} />
            </label>
          </div>
          <label>
            Country
            <input required value={form.country}
              onChange={(e) => setField("country", e.target.value)} />
          </label>
          {error && <div className="error">{error}</div>}
          <button className="button" type="submit" disabled={submitting}>
            {submitting ? "Placing order…" : `Place order · ${formatPrice(cart.totalCents)}`}
          </button>
          <p className="muted" style={{ margin: 0 }}>
            Demo checkout — no payment is taken. Prices are recalculated server-side.
          </p>
        </form>
      </div>
    </>
  );
}
