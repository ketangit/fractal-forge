package com.fractalforge.puzzle.generator;

/** Input parameters for puzzle generation (dimensions in millimetres). */
public final class PuzzleSpec {

	/**
	 * Default coaster diameter (millimetres) when a circular panel is requested.
	 */
	public static final double DEFAULT_PANEL_DIAMETER = 110.0;

	public final double seed;
	public final boolean nonDeterministic;
	public final int ncols;
	public final int nrows;
	public final double tileRadius;
	public final double frame;
	public final double frameCorner;
	public final int minPieceSize;
	public final int maxPieceSize;
	public final TileShape shape;
	public final PanelShape panelShape;
	public final double panelDiameter;

	/**
	 * Back-compatible constructor: produces the original square (rounded-rectangle)
	 * panel. Retained so existing callers and tests keep working unchanged.
	 */
	public PuzzleSpec(double seed, boolean nonDeterministic, int ncols, int nrows, double tileRadius, double frame,
			double frameCorner, int minPieceSize, int maxPieceSize, TileShape shape) {
		this(seed, nonDeterministic, ncols, nrows, tileRadius, frame, frameCorner, minPieceSize, maxPieceSize, shape,
				PanelShape.SQUARE, DEFAULT_PANEL_DIAMETER);
	}

	public PuzzleSpec(double seed, boolean nonDeterministic, int ncols, int nrows, double tileRadius, double frame,
			double frameCorner, int minPieceSize, int maxPieceSize, TileShape shape, PanelShape panelShape,
			double panelDiameter) {
		if (ncols < 2 || ncols > 250)
			throw new IllegalArgumentException("ncols must be 2..250");
		if (nrows < 2 || nrows > 250)
			throw new IllegalArgumentException("nrows must be 2..250");
		if (tileRadius <= 0 || tileRadius > 1000)
			throw new IllegalArgumentException("tileRadius must be in (0, 1000]");
		if (frame < 0 || frame > 1000)
			throw new IllegalArgumentException("frame must be in [0, 1000]");
		if (frameCorner < 0 || frameCorner > 1000)
			throw new IllegalArgumentException("frameCorner must be in [0, 1000]");
		if (minPieceSize < 2)
			throw new IllegalArgumentException("minPieceSize must be >= 2");
		if (maxPieceSize < minPieceSize)
			throw new IllegalArgumentException("maxPieceSize must be >= minPieceSize");
		if (maxPieceSize > ncols * nrows)
			throw new IllegalArgumentException("maxPieceSize must be <= ncols * nrows");
		PanelShape panel = panelShape == null ? PanelShape.SQUARE : panelShape;
		if (panel == PanelShape.CIRCLE && (panelDiameter <= 0 || panelDiameter > 2000))
			throw new IllegalArgumentException("panelDiameter must be in (0, 2000] for a circular panel");
		this.seed = seed;
		this.nonDeterministic = nonDeterministic;
		this.ncols = ncols;
		this.nrows = nrows;
		this.tileRadius = tileRadius;
		this.frame = frame;
		this.frameCorner = frameCorner;
		this.minPieceSize = minPieceSize;
		this.maxPieceSize = maxPieceSize;
		this.shape = shape == null ? TileShape.CIRCULAR : shape;
		this.panelShape = panel;
		this.panelDiameter = panelDiameter;
	}

	public double widthMm() {
		return panelShape == PanelShape.CIRCLE ? panelDiameter : ncols * 2 * tileRadius + 2 * frame;
	}

	public double heightMm() {
		return panelShape == PanelShape.CIRCLE ? panelDiameter : nrows * 2 * tileRadius + 2 * frame;
	}
}
