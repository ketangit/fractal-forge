package com.fractalforge.puzzle.api;

import com.fractalforge.puzzle.generator.PanelShape;
import com.fractalforge.puzzle.generator.PuzzleSpec;
import com.fractalforge.puzzle.generator.TileShape;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Generation request; dimensions in millimetres. Double-typed fields are
 * range-checked by the PuzzleSpec constructor (Bean Validation does not
 * support @DecimalMin on primitive doubles).
 *
 * <p>
 * {@code panelShape} selects the overall outline: {@code SQUARE} (default,
 * rounded-rectangle tray) or {@code CIRCLE} (round coaster). When omitted in
 * the request body it defaults to {@code SQUARE}, so existing clients are
 * unaffected. {@code panelDiameter} (millimetres) applies only to circular
 * panels; when omitted it defaults to 110&nbsp;mm.
 */
public record PuzzleRequest(double seed, boolean nonDeterministic, @Min(2) @Max(250) int ncols,
		@Min(2) @Max(250) int nrows, double tileRadius, double frame, double frameCorner,
		@Min(2) @Max(62500) int minPieceSize, @Min(2) @Max(62500) int maxPieceSize, @NotNull TileShape shape,
		PanelShape panelShape, Double panelDiameter) {

	public PuzzleSpec toSpec() {
		PanelShape panel = panelShape == null ? PanelShape.SQUARE : panelShape;
		double diameter = panelDiameter == null || panelDiameter <= 0
				? PuzzleSpec.DEFAULT_PANEL_DIAMETER
				: panelDiameter;
		return new PuzzleSpec(seed, nonDeterministic, ncols, nrows, tileRadius, frame, frameCorner, minPieceSize,
				maxPieceSize, shape, panel, diameter);
	}
}
