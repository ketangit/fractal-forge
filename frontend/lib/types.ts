export type TileShape = "CIRCULAR" | "OCTAGONAL" | "SQUARE";

export type Material = "BIRCH_PLY" | "WALNUT" | "ACRYLIC_CLEAR" | "ACRYLIC_BLACK";

export const MATERIAL_LABELS: Record<Material, string> = {
  BIRCH_PLY: "Birch plywood 3mm",
  WALNUT: "Walnut hardwood 4mm",
  ACRYLIC_CLEAR: "Clear acrylic 3mm",
  ACRYLIC_BLACK: "Black acrylic 3mm",
};

export interface PuzzleParams {
  seed: number;
  nonDeterministic: boolean;
  ncols: number;
  nrows: number;
  tileRadius: number;
  frame: number;
  frameCorner: number;
  minPieceSize: number;
  maxPieceSize: number;
  shape: TileShape;
}

export interface GenerateResponse {
  seedUsed: number;
  widthMm: number;
  heightMm: number;
  pieceCount: number;
  piecePaths: string[];
  framePath: string;
  priceCentsByMaterial: Record<Material, number>;
}

export type ExportMode =
  | "OVERLAP"
  | "NON_OVERLAP"
  | "NON_OVERLAP_SINGLE_PATH"
  | "COLORED";

export interface Product {
  id: number;
  slug: string;
  name: string;
  description: string;
  material: Material;
  priceCents: number;
  seed: number;
  ncols: number;
  nrows: number;
  tileRadius: number;
  frame: number;
  frameCorner: number;
  minPieceSize: number;
  maxPieceSize: number;
  shape: TileShape;
}

export interface CartItem {
  key: string;
  kind: "product" | "custom";
  productId?: number;
  name: string;
  material: Material;
  unitPriceCents: number;
  quantity: number;
  params?: PuzzleParams; // custom puzzles: exact spec (seed pinned)
  pieceCount?: number;
}

export interface OrderConfirmation {
  orderRef: string;
  status: string;
  totalCents: number;
  itemCount: number;
}

export const DEFAULT_PARAMS: PuzzleParams = {
  seed: 4242,
  nonDeterministic: false,
  ncols: 20,
  nrows: 20,
  tileRadius: 6.0,
  frame: 6.0,
  frameCorner: 4.0,
  minPieceSize: 4,
  maxPieceSize: 50,
  shape: "CIRCULAR",
};

export function productToParams(p: Product): PuzzleParams {
  return {
    seed: p.seed,
    nonDeterministic: false,
    ncols: p.ncols,
    nrows: p.nrows,
    tileRadius: p.tileRadius,
    frame: p.frame,
    frameCorner: p.frameCorner,
    minPieceSize: p.minPieceSize,
    maxPieceSize: p.maxPieceSize,
    shape: p.shape,
  };
}
