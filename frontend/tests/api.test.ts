import { afterEach, describe, expect, it, vi } from "vitest";
import { createOrder, generatePuzzle } from "@/lib/api";
import { DEFAULT_PARAMS } from "@/lib/types";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("api client", () => {
  it("POSTs generation params as JSON", async () => {
    const mock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ seedUsed: 42, piecePaths: [] }), { status: 200 }),
    );
    await generatePuzzle(DEFAULT_PARAMS);
    expect(mock).toHaveBeenCalledWith(
      "/api/puzzle/generate",
      expect.objectContaining({
        method: "POST",
        headers: { "Content-Type": "application/json" },
      }),
    );
    const body = JSON.parse((mock.mock.calls[0][1] as RequestInit).body as string);
    expect(body.ncols).toBe(DEFAULT_PARAMS.ncols);
  });

  it("surfaces backend error messages", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ error: "ncols must be 2..250" }), { status: 400 }),
    );
    await expect(generatePuzzle(DEFAULT_PARAMS)).rejects.toThrow("ncols must be 2..250");
  });

  it("returns the order confirmation", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({ orderRef: "ref-7", status: "PAID", totalCents: 9800, itemCount: 1 }),
        { status: 200 },
      ),
    );
    const confirmation = await createOrder({
      customerName: "Ada",
      email: "ada@example.com",
      addressLine: "1 Way",
      city: "London",
      postalCode: "N1",
      country: "GB",
      items: [{ productId: 1, quantity: 2 }],
    });
    expect(confirmation.orderRef).toBe("ref-7");
    expect(confirmation.status).toBe("PAID");
  });
});
