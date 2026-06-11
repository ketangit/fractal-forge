import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import PuzzlePreview from "@/components/PuzzlePreview";

const piecePaths = [
  "M18,12 A 6 6 0 0,1 24,18 Z",
  "M30,12 A 6 6 0 0,1 36,18 Z",
];

describe("PuzzlePreview", () => {
  it("renders one path per piece plus the frame", () => {
    const { container } = render(
      <PuzzlePreview
        widthMm={108}
        heightMm={108}
        piecePaths={piecePaths}
        framePath="M4,0 H 104 V 104 H 4 Z"
      />,
    );
    expect(container.querySelectorAll("path")).toHaveLength(piecePaths.length + 1);
  });

  it("uses the puzzle dimensions as viewBox", () => {
    const { container } = render(
      <PuzzlePreview widthMm={108} heightMm={252} piecePaths={piecePaths} framePath="M0,0 Z" />,
    );
    expect(container.querySelector("svg")?.getAttribute("viewBox")).toBe("0 0 108 252");
  });

  it("is labelled for screen readers", () => {
    render(
      <PuzzlePreview widthMm={10} heightMm={10} piecePaths={piecePaths} framePath="M0,0 Z" />,
    );
    expect(screen.getByRole("img", { name: /2 pieces/i })).toBeDefined();
  });
});
