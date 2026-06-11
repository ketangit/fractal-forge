/** Pleasant deterministic palette for piece previews. */
const PALETTE = [
  "#e8b04b", "#c96f4a", "#7da87b", "#5b8aa6", "#a3719a",
  "#d9985f", "#6aa39a", "#b5667a", "#8a9a5b", "#7a7fb5",
  "#caa472", "#5f9ea0", "#bc8f8f", "#9aab7c", "#937dac",
];

export function pieceColor(index: number): string {
  return PALETTE[index % PALETTE.length];
}
