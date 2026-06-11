"use client";

import React from "react";
import { pieceColor } from "@/lib/palette";

interface Props {
  widthMm: number;
  heightMm: number;
  piecePaths: string[];
  framePath: string;
  colored?: boolean;
}

/** Renders the generated puzzle as an inline scalable SVG. */
export default function PuzzlePreview({ widthMm, heightMm, piecePaths, framePath, colored = true }: Props) {
  const strokeW = Math.max(widthMm, heightMm) / 400;
  return (
    <svg
      viewBox={`0 0 ${widthMm} ${heightMm}`}
      role="img"
      aria-label={`Puzzle preview with ${piecePaths.length} pieces`}
      style={{ width: "100%", height: "auto", display: "block" }}
    >
      <path d={framePath} fill="#f7f1e3" stroke="#4a3f35" strokeWidth={strokeW * 2} />
      {piecePaths.map((d, i) => (
        <path
          key={i}
          d={d}
          fill={colored ? pieceColor(i) : "none"}
          fillOpacity={colored ? 0.85 : 0}
          stroke="#3b3128"
          strokeWidth={strokeW}
        />
      ))}
    </svg>
  );
}
