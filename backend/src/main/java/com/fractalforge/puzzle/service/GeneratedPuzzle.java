package com.fractalforge.puzzle.service;

import com.fractalforge.puzzle.generator.CircleFractalJigsaw;
import com.fractalforge.puzzle.generator.PuzzleSpec;

import java.util.List;

/** A generated puzzle plus everything needed to export or re-render it. */
public record GeneratedPuzzle(PuzzleSpec spec, CircleFractalJigsaw jigsaw, double seedUsed, double coloringSeed) {

	public List<String> piecePaths() {
		return jigsaw.multipaths(spec.frame, spec.tileRadius, spec.shape);
	}

	public String framePath() {
		return jigsaw.createFrame(spec.frame, spec.tileRadius, spec.frameCorner, spec.panelShape, spec.panelDiameter);
	}

	public String export(ExportMode mode) {
		return switch (mode) {
			case OVERLAP -> jigsaw.exportSvg(spec.frame, spec.tileRadius, spec.shape, spec.frameCorner, spec.panelShape,
					spec.panelDiameter);
			case NON_OVERLAP -> jigsaw.exportSvgNoOverlap(spec.frame, spec.tileRadius, spec.shape, spec.frameCorner,
					spec.panelShape, spec.panelDiameter);
			case NON_OVERLAP_SINGLE_PATH -> jigsaw.exportSvgNoOverlapSinglePath(spec.frame, spec.tileRadius, spec.shape,
					spec.frameCorner, spec.panelShape, spec.panelDiameter);
			case COLORED -> jigsaw.exportSvgColored(spec.frame, spec.tileRadius, spec.shape, spec.frameCorner,
					coloringSeed, spec.panelShape, spec.panelDiameter);
		};
	}
}
