"use client";

import React from "react";
import { BARK_BROWN, pieceColor } from "@/lib/palette";

interface Props {
  widthMm: number;
  heightMm: number;
  piecePaths: string[];
  framePath: string;
  colored?: boolean;
  /** Per-piece colours (adjacency-aware). Falls back to the index palette. */
  pieceColors?: string[];
  /** Override the default `0 0 width height` viewBox (used for centred discs). */
  viewBox?: string;
  /**
   * Clip the pieces to this path (the inner edge of a circular panel's frame
   * ring). When set, the pieces are masked to it and it is stroked as the inner
   * frame edge. Omit for the square tray.
   */
  clipPathD?: string;
}

/** Renders the generated puzzle as an inline scalable SVG. */
export default function PuzzlePreview({
  widthMm,
  heightMm,
  piecePaths,
  framePath,
  colored = true,
  pieceColors,
  viewBox,
  clipPathD,
}: Props) {
  const strokeW = Math.max(widthMm, heightMm) / 400;
  const clipId = React.useId();
  const pieces = piecePaths.map((d, i) => (
    <path
      key={i}
      d={d}
      fill={colored ? pieceColors?.[i] ?? pieceColor(i) : "none"}
      fillOpacity={colored ? 1 : 0}
      stroke={BARK_BROWN}
      strokeWidth={strokeW}
    />
  ));
  return (
    <svg
      viewBox={viewBox ?? `0 0 ${widthMm} ${heightMm}`}
      role="img"
      aria-label={`Puzzle preview with ${piecePaths.length} pieces`}
      style={{ width: "100%", height: "auto", display: "block" }}
    >
      {clipPathD && (
        <defs>
          <clipPath id={clipId}>
            <path d={clipPathD} />
          </clipPath>
        </defs>
      )}
      {/* Frame / backing: the outer edge is always Bark Brown. */}
      <path d={framePath} fill={BARK_BROWN} stroke={BARK_BROWN} strokeWidth={strokeW * 2} />
      {clipPathD ? <g clipPath={`url(#${clipId})`}>{pieces}</g> : pieces}
      {/* Inner frame edge (circular panel only). */}
      {clipPathD && <path d={clipPathD} fill="none" stroke={BARK_BROWN} strokeWidth={strokeW * 2} />}
    </svg>
  );
}
