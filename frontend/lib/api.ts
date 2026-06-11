import type {
  ExportMode,
  GenerateResponse,
  OrderConfirmation,
  Product,
  PuzzleParams,
} from "./types";

// Same-origin in the single-container deployment; set
// NEXT_PUBLIC_API_BASE=http://localhost:8080 when running `next dev`.
const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "";

async function jsonOrThrow<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body?.error) message = body.error;
    } catch {
      /* keep default message */
    }
    throw new Error(message);
  }
  return res.json() as Promise<T>;
}

export async function generatePuzzle(params: PuzzleParams): Promise<GenerateResponse> {
  const res = await fetch(`${API_BASE}/api/puzzle/generate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(params),
  });
  return jsonOrThrow<GenerateResponse>(res);
}

export async function exportPuzzle(params: PuzzleParams, mode: ExportMode): Promise<Blob> {
  const res = await fetch(`${API_BASE}/api/puzzle/export?mode=${mode}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(params),
  });
  if (!res.ok) throw new Error(`Export failed (${res.status})`);
  return res.blob();
}

export async function fetchProducts(): Promise<Product[]> {
  return jsonOrThrow<Product[]>(await fetch(`${API_BASE}/api/products`));
}

export async function fetchProduct(id: number): Promise<Product> {
  return jsonOrThrow<Product>(await fetch(`${API_BASE}/api/products/${id}`));
}

export interface OrderPayload {
  customerName: string;
  email: string;
  addressLine: string;
  city: string;
  postalCode: string;
  country: string;
  items: Array<{
    productId?: number;
    customSpec?: PuzzleParams;
    material?: string;
    quantity: number;
  }>;
}

export async function createOrder(payload: OrderPayload): Promise<OrderConfirmation> {
  const res = await fetch(`${API_BASE}/api/orders`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return jsonOrThrow<OrderConfirmation>(res);
}

export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
