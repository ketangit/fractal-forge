package com.fractalforge.puzzle.generator;

/**
 * Overall outline ("panel") the puzzle is cut into.
 *
 * <ul>
 * <li>{@link #SQUARE} — the original rounded-rectangle tray. Default; preserves
 * the historical geometry and golden-file output byte-for-byte.</li>
 * <li>{@link #CIRCLE} — a round disc (e.g. a 110&nbsp;mm coaster). The puzzle
 * grid is centred on the disc and clipped to it.</li>
 * </ul>
 */
public enum PanelShape {
	SQUARE, CIRCLE
}
