import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import PuzzlePreview from "@/components/PuzzlePreview";

const piecePaths = ["M18,12 A 6 6 0 0,1 24,18 Z", "M30,12 A 6 6 0 0,1 36,18 Z"];
const innerCircle = "M5,54 A 49 49 0 0,1 103 54 A 49 49 0 0,1 5 54 Z";
const outerCircle = "M-1,54 A 55 55 0 0,1 109 54 A 55 55 0 0,1 -1 54 Z";

describe("PuzzlePreview — circular coaster panel", () => {
  it("honours a custom (centred) viewBox", () => {
    const { container } = render(
      <PuzzlePreview
        widthMm={110}
        heightMm={110}
        piecePaths={piecePaths}
        framePath={outerCircle}
        viewBox="-1 -1 110 110"
        clipPathD={innerCircle}
      />,
    );
    expect(container.querySelector("svg")?.getAttribute("viewBox")).toBe("-1 -1 110 110");
  });

  it("clips pieces to the inner disc via a clipPath", () => {
    const { container } = render(
      <PuzzlePreview
        widthMm={110}
        heightMm={110}
        piecePaths={piecePaths}
        framePath={outerCircle}
        clipPathD={innerCircle}
      />,
    );
    expect(container.querySelector("clipPath")).not.toBeNull();
    expect(container.querySelector("g[clip-path]")).not.toBeNull();
  });

  it("adds no clipPath for the default square panel", () => {
    const { container } = render(
      <PuzzlePreview widthMm={108} heightMm={108} piecePaths={piecePaths} framePath="M4,0 H 104 V 104 H 4 Z" />,
    );
    expect(container.querySelector("clipPath")).toBeNull();
    // frame + one path per piece, nothing extra
    expect(container.querySelectorAll("path")).toHaveLength(piecePaths.length + 1);
  });
});
