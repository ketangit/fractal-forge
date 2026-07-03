package com.fractalforge.puzzle.generator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Procedural "fractal" jigsaw generator. Faithful port of the reference
 * CircleFractalJigsaw: tiles on a square grid are joined diagonally to form
 * pieces, then rendered as chains of quarter-circle arcs (or octagonal / square
 * approximations).
 *
 * <p>
 * The puzzle pieces are independent of the overall {@link PanelShape}: a square
 * (rounded-rectangle) tray and a circular coaster share identical piece
 * geometry. The panel only changes the outer frame outline, the SVG canvas, and
 * — for the circular coaster — a clip applied to the pieces so they are trimmed
 * to the disc.
 */
public final class CircleFractalJigsaw {

	private final int ncols;
	private final int nrows;
	private final int minPieceLen;
	private final int maxPieceLen;
	private final CellGrid grid;
	private final RandomSource rng;
	private final List<List<DiagonalConnection>> pieces = new ArrayList<List<DiagonalConnection>>();

	public CircleFractalJigsaw(int ncols, int nrows, int minPieceLen, int maxPieceLen, RandomSource rng) {
		this.ncols = ncols;
		this.nrows = nrows;
		this.minPieceLen = minPieceLen;
		this.maxPieceLen = maxPieceLen;
		this.grid = new CellGrid(ncols, nrows);
		this.rng = rng;
	}

	/** Runs the full reference pipeline: generate, then fill holes. */
	public void generateAll() {
		while (grid.nUnvisited() > 0) {
			createPiece();
		}
		regenerateGrid();
		while (fillHoles(false)) {
			// repeat until no more holes can be filled strictly
		}
		fillHoles(true);
	}

	private List<DiagonalConnection> possibleConnections(List<TileNode> myTiles, boolean allowPartials) {
		List<DiagonalConnection> pcs = new ArrayList<DiagonalConnection>();
		int[][] neighbors = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
		for (TileNode v : myTiles) {
			if (v.hasConnections || allowPartials) {
				v.hasConnections = false;
				for (int[] n : neighbors) {
					int cx = v.x + n[0];
					int cy = v.y + n[1];
					if (grid.isTileValid(cx, cy) && findTile(myTiles, cx, cy) == null) {
						TileNode cpt = new TileNode(cx, cy);
						boolean visited = grid.isTileVisited(cpt);
						DiagonalConnection dc = new DiagonalConnection(v, cpt, !visited);
						if (grid.isCellEmpty(dc.cellX(), dc.cellY())) {
							if (allowPartials || !visited) {
								pcs.add(dc);
								v.hasConnections = true;
							}
						}
					}
				}
			}
		}
		return pcs;
	}

	private static TileNode findTile(List<TileNode> tiles, int x, int y) {
		for (TileNode t : tiles) {
			if (t.x == x && t.y == y)
				return t;
		}
		return null;
	}

	private void createPiece() {
		List<TileNode> myTiles = new ArrayList<TileNode>();
		List<DiagonalConnection> myConnections = new ArrayList<DiagonalConnection>();
		int targetPieceLen = (int) Math.round(rng.uniform(minPieceLen, maxPieceLen));

		TileNode vi = grid.randomEmptyTile(rng);
		myTiles.add(vi);
		grid.visitTile(vi);

		while (grid.nUnvisited() > 0 && myTiles.size() < targetPieceLen) {
			List<DiagonalConnection> pcs = possibleConnections(myTiles, false);
			if (pcs.isEmpty()) {
				break;
			}
			DiagonalConnection chosen = pcs.get((int) Math.floor(rng.uniform(0, pcs.size())));
			myConnections.add(chosen);
			myTiles.add(chosen.p2());
			grid.occupyCell(chosen.cellX(), chosen.cellY());
			grid.visitTile(chosen.p2());
		}

		if (myTiles.size() >= minPieceLen) {
			pieces.add(myConnections);
		} else {
			for (DiagonalConnection c : myConnections) {
				grid.liberateCell(c.cellX(), c.cellY());
			}
		}
	}

	private boolean fillHoles(boolean allowPartials) {
		boolean filled = false;
		pieces.sort(new java.util.Comparator<List<DiagonalConnection>>() {
			@Override
			public int compare(List<DiagonalConnection> a, List<DiagonalConnection> b) {
				return a.size() - b.size();
			}
		});
		for (List<DiagonalConnection> p : pieces) {
			List<TileNode> tiles = new ArrayList<TileNode>();
			tiles.add(p.get(0).p1());
			for (DiagonalConnection con : p) {
				tiles.add(con.p2());
			}
			// Reference used Array.forEach, which does not visit elements
			// appended during iteration: iterate over the initial range only.
			int initialCount = tiles.size();
			for (int i = 0; i < initialCount; i++) {
				TileNode v = tiles.get(i);
				List<DiagonalConnection> pcs = possibleConnections(java.util.Collections.singletonList(v),
						allowPartials);
				if (!pcs.isEmpty()) {
					for (DiagonalConnection pc : pcs) {
						if (findTile(tiles, pc.p2().x, pc.p2().y) != null) {
							continue;
						}
						p.add(pc);
						tiles.add(pc.p2());
						filled = true;
						grid.occupyCell(pc.cellX(), pc.cellY());
						grid.visitTile(pc.p2());
					}
				}
			}
		}
		return filled;
	}

	private void regenerateGrid() {
		grid.reset();
		for (List<DiagonalConnection> p : pieces) {
			for (DiagonalConnection c : p) {
				if (!grid.isTileVisited(c.p1())) {
					grid.visitTile(c.p1());
				}
				if (c.p2Taken() && !grid.isTileVisited(c.p2())) {
					grid.visitTile(c.p2());
				}
				grid.occupyCell(c.cellX(), c.cellY());
			}
		}
	}

	static void addArcs(DiagonalConnection con, List<DiagonalConnection> connections, List<Arc> arcs, double rad,
			double frame, boolean first) {
		Arc newArc;
		switch (con.quad()) {
			case 0 :
				newArc = new Arc(con.p1().x + 1, con.p1().y, rad, frame, 1, 1);
				break;
			case 1 :
				newArc = new Arc(con.p1().x, con.p1().y - 1, rad, frame, 2, 1);
				break;
			case 2 :
				newArc = new Arc(con.p1().x - 1, con.p1().y, rad, frame, 3, 1);
				break;
			default :
				newArc = new Arc(con.p1().x, con.p1().y + 1, rad, frame, 0, 1);
				break;
		}
		arcs.add(newArc);

		if (con.p2Taken()) {
			int[] p2quads = {(con.quad() + 3) % 4, (con.quad() + 4) % 4, (con.quad() + 5) % 4};
			for (int q : p2quads) {
				DiagonalConnection pct = DiagonalConnection.fromPointAndQuad(con.p2(), q, true);
				DiagonalConnection pcnt = DiagonalConnection.fromPointAndQuad(con.p2(), q, false);
				DiagonalConnection foundTaken = findConnection(connections, pct);
				if (foundTaken != null) {
					addArcs(foundTaken, connections, arcs, rad, frame, false);
				} else {
					DiagonalConnection foundNotTaken = findConnection(connections, pcnt);
					if (foundNotTaken != null) {
						addArcs(foundNotTaken, connections, arcs, rad, frame, false);
					} else {
						arcs.add(new Arc(con.p2().x, con.p2().y, rad, frame, q, 0));
					}
				}
			}
		} else {
			arcs.add(new Arc(con.p2().x, con.p2().y, rad, frame, (con.quad() + 2) % 4, 1));
		}

		switch (con.quad()) {
			case 0 :
				newArc = new Arc(con.p1().x, con.p1().y - 1, rad, frame, 3, 1);
				break;
			case 1 :
				newArc = new Arc(con.p1().x - 1, con.p1().y, rad, frame, 0, 1);
				break;
			case 2 :
				newArc = new Arc(con.p1().x, con.p1().y + 1, rad, frame, 1, 1);
				break;
			default :
				newArc = new Arc(con.p1().x + 1, con.p1().y, rad, frame, 2, 1);
				break;
		}
		arcs.add(newArc);

		if (first) {
			int[] p1quads = {(con.quad() + 1) % 4, (con.quad() + 2) % 4, (con.quad() + 3) % 4};
			for (int q : p1quads) {
				DiagonalConnection pct = DiagonalConnection.fromPointAndQuad(con.p1(), q, true);
				DiagonalConnection pcnt = DiagonalConnection.fromPointAndQuad(con.p1(), q, false);
				DiagonalConnection foundTaken = findConnection(connections, pct);
				if (foundTaken != null) {
					addArcs(foundTaken, connections, arcs, rad, frame, false);
				} else {
					DiagonalConnection foundNotTaken = findConnection(connections, pcnt);
					if (foundNotTaken != null) {
						addArcs(foundNotTaken, connections, arcs, rad, frame, false);
					} else {
						arcs.add(new Arc(con.p1().x, con.p1().y, rad, frame, q, 0));
					}
				}
			}
		}
	}

	private static DiagonalConnection findConnection(List<DiagonalConnection> connections, DiagonalConnection target) {
		for (DiagonalConnection c : connections) {
			if (c.eq(target))
				return c;
		}
		return null;
	}

	public int npieces() {
		return pieces.size();
	}

	/** Distinct tile count per piece (for stats, pricing and tests). */
	public List<Integer> pieceTileCounts() {
		List<Integer> counts = new ArrayList<Integer>();
		for (List<DiagonalConnection> p : pieces) {
			Set<String> tiles = new LinkedHashSet<String>();
			for (DiagonalConnection c : p) {
				tiles.add(c.p1().x + "," + c.p1().y);
				tiles.add(c.p2().x + "," + c.p2().y);
			}
			counts.add(tiles.size());
		}
		return counts;
	}

	/** Closed outline path ("d" attribute) for every piece. */
	public List<String> multipaths(double frame, double rad, TileShape shape) {
		List<String> paths = new ArrayList<String>();
		for (List<DiagonalConnection> p : pieces) {
			List<Arc> arcs = new ArrayList<Arc>();
			addArcs(p.get(0), p, arcs, rad, frame, true);
			StringBuilder d = new StringBuilder();
			d.append("M").append(Svg.fmt(arcs.get(0).spx())).append(",").append(Svg.fmt(arcs.get(0).spy())).append(" ");
			for (Arc a : arcs) {
				d.append(a.svg(shape));
			}
			d.append("Z");
			paths.add(d.toString());
		}
		return paths;
	}

	private String svgHeader(double frame, double rad) {
		double width = ncols * 2 * rad + 2 * frame;
		double height = nrows * 2 * rad + 2 * frame;
		return "<?xml version=\"1.0\" encoding=\"utf-8\" ?><svg baseProfile=\"full\" height=\"" + Svg.fmt(height)
				+ "mm\" version=\"1.1\" viewBox=\"0 0 " + Svg.fmt(width) + " " + Svg.fmt(height) + "\" width=\""
				+ Svg.fmt(width) + "mm\" xmlns=\"http://www.w3.org/2000/svg\""
				+ " xmlns:ev=\"http://www.w3.org/2001/xml-events\""
				+ " xmlns:xlink=\"http://www.w3.org/1999/xlink\"><defs />";
	}

	private static String framePathElement(String frameD) {
		return "<path fill=\"none\" stroke=\"black\" stroke-width=\"0.1\" d=\"" + frameD + "\"></path>";
	}

	/**
	 * Rounded-rectangle frame path. Port of reference createframe().
	 */
	public String createFrame(double frame, double rad, double frameCorner) {
		double width = ncols * 2 * rad + 2 * frame;
		double height = nrows * 2 * rad + 2 * frame;
		StringBuilder d = new StringBuilder();
		d.append("M").append(Svg.fmt(frameCorner)).append(",0 ");
		d.append("H ").append(Svg.fmt(width - frameCorner));
		if (frameCorner > 0) {
			d.append("A ").append(Svg.fmt(frameCorner)).append(" ").append(Svg.fmt(frameCorner)).append(" 0 0,1 ")
					.append(Svg.fmt(width)).append(" ").append(Svg.fmt(frameCorner)).append(" ");
		}
		d.append("V ").append(Svg.fmt(height - frameCorner));
		if (frameCorner > 0) {
			d.append("A ").append(Svg.fmt(frameCorner)).append(" ").append(Svg.fmt(frameCorner)).append(" 0 0,1 ")
					.append(Svg.fmt(width - frameCorner)).append(" ").append(Svg.fmt(height)).append(" ");
		}
		d.append("H ").append(Svg.fmt(frameCorner));
		if (frameCorner > 0) {
			d.append("A ").append(Svg.fmt(frameCorner)).append(" ").append(Svg.fmt(frameCorner)).append(" 0 0,1 0 ")
					.append(Svg.fmt(height - frameCorner)).append(" ");
		}
		d.append("V ").append(Svg.fmt(frameCorner));
		if (frameCorner > 0) {
			d.append("A ").append(Svg.fmt(frameCorner)).append(" ").append(Svg.fmt(frameCorner)).append(" 0 0,1 ")
					.append(Svg.fmt(frameCorner)).append(" 0 ");
		}
		d.append("Z");
		return d.toString();
	}

	/**
	 * Overlapping export: every piece is an individually contoured closed path
	 * (shared edges drawn twice). Good for CNC milling and for cutting replacement
	 * pieces.
	 */
	public String exportSvg(double frame, double rad, TileShape shape, double frameCorner) {
		StringBuilder data = new StringBuilder(svgHeader(frame, rad));
		for (List<DiagonalConnection> p : pieces) {
			List<Arc> arcs = new ArrayList<Arc>();
			addArcs(p.get(0), p, arcs, rad, frame, true);
			data.append("<path fill=\"none\" stroke=\"black\" stroke-width=\"0.1\" d=\"M")
					.append(Svg.fmt(arcs.get(0).spx())).append(",").append(Svg.fmt(arcs.get(0).spy())).append(" ");
			for (Arc a : arcs) {
				data.append(a.svg(shape));
			}
			data.append("Z\"></path>");
		}
		data.append(framePathElement(createFrame(frame, rad, frameCorner)));
		data.append("</svg>");
		return data.toString();
	}

	/**
	 * Non-overlapping export: each shared edge appears exactly once, so the file
	 * can be laser-cut directly without double-cutting.
	 */
	public String exportSvgNoOverlap(double frame, double rad, TileShape shape, double frameCorner) {
		StringBuilder data = new StringBuilder(svgHeader(frame, rad));
		Set<String> allArcs = new LinkedHashSet<String>();
		for (List<DiagonalConnection> p : pieces) {
			boolean inPath = false;
			StringBuilder path = new StringBuilder();
			List<Arc> arcs = new ArrayList<Arc>();
			addArcs(p.get(0), p, arcs, rad, frame, true);
			for (Arc a : arcs) {
				if (allArcs.contains(a.key())) {
					if (inPath) {
						path.append("\"></path>");
						data.append(path);
						inPath = false;
					}
				} else {
					allArcs.add(a.key());
					if (!inPath) {
						path = new StringBuilder("<path fill=\"none\" stroke=\"black\" stroke-width=\"0.1\" d=\"M")
								.append(Svg.fmt(a.spx())).append(",").append(Svg.fmt(a.spy())).append(" ");
						inPath = true;
					}
					path.append(a.svg(shape));
				}
			}
			if (inPath) {
				path.append("\"></path>");
				data.append(path);
			}
		}
		data.append(framePathElement(createFrame(frame, rad, frameCorner)));
		data.append("</svg>");
		return data.toString();
	}

	/**
	 * Non-overlapping export as one single SVG path element (works better with some
	 * laser controllers, e.g. Trotec).
	 */
	public String exportSvgNoOverlapSinglePath(double frame, double rad, TileShape shape, double frameCorner) {
		StringBuilder data = new StringBuilder(svgHeader(frame, rad));
		Set<String> allArcs = new LinkedHashSet<String>();
		data.append("<path fill=\"none\" stroke=\"black\" stroke-width=\"0.1\" d=\"");
		double curX = -1, curY = -1;
		for (List<DiagonalConnection> p : pieces) {
			List<Arc> arcs = new ArrayList<Arc>();
			addArcs(p.get(0), p, arcs, rad, frame, true);
			for (Arc a : arcs) {
				if (!allArcs.contains(a.key())) {
					allArcs.add(a.key());
					if (!a.spEquals(curX, curY)) {
						data.append("M").append(Svg.fmt(a.spx())).append(",").append(Svg.fmt(a.spy())).append(" ");
					}
					data.append(a.svg(shape));
					curX = a.epx();
					curY = a.epy();
				}
			}
		}
		data.append("\"></path>");
		data.append(framePathElement(createFrame(frame, rad, frameCorner)));
		data.append("</svg>");
		return data.toString();
	}

	/**
	 * Folkfirt Bark Brown (2907CA) — always the outer edge / frame on coloured
	 * sheets.
	 */
	private static final String BARK_BROWN = "#4A3526";

	/**
	 * Vivid acrylic interior colours (matches the frontend palette), high adjacent
	 * contrast.
	 */
	private static final String[] ACRYLIC_PIECE_COLORS = {"#F7831A", "#2EB24A", "#2350B5", "#FFD21A", "#7B2D8E",
			"#1BB6C1", "#D61F3A", "#A6CE39", "#D43F7C", "#1487A6", "#A6477F", "#4FD1C5", "#8A2F4A", "#20308F",
			"#E22128"};

	/**
	 * Quantised vertex key so shared boundary points between pieces compare equal.
	 */
	private static String vertexKey(double x, double y) {
		return Math.round(x * 1000) + ":" + Math.round(y * 1000);
	}

	/**
	 * Assigns a bright colour to every piece so that adjacent pieces (those that
	 * share a boundary vertex) never share a colour — a greedy map-colouring over
	 * the piece adjacency graph, biased toward the least-used colour. Mirrors the
	 * frontend's {@code assignPieceColors} exactly, so the downloaded solution
	 * sheet matches the on-screen preview.
	 */
	private List<String> assignPieceColors(double frame, double rad) {
		int n = pieces.size();
		List<Set<String>> verts = new ArrayList<Set<String>>();
		for (List<DiagonalConnection> p : pieces) {
			List<Arc> arcs = new ArrayList<Arc>();
			addArcs(p.get(0), p, arcs, rad, frame, true);
			Set<String> vs = new LinkedHashSet<String>();
			for (Arc a : arcs) {
				vs.add(vertexKey(a.spx(), a.spy()));
				vs.add(vertexKey(a.epx(), a.epy()));
			}
			verts.add(vs);
		}
		Map<String, List<Integer>> byVertex = new LinkedHashMap<String, List<Integer>>();
		for (int i = 0; i < n; i++) {
			for (String v : verts.get(i)) {
				byVertex.computeIfAbsent(v, k -> new ArrayList<Integer>()).add(i);
			}
		}
		List<Set<Integer>> adj = new ArrayList<Set<Integer>>();
		for (int i = 0; i < n; i++) {
			adj.add(new LinkedHashSet<Integer>());
		}
		for (List<Integer> arr : byVertex.values()) {
			if (arr.size() < 2)
				continue;
			for (int a = 0; a < arr.size(); a++) {
				for (int b = a + 1; b < arr.size(); b++) {
					adj.get(arr.get(a)).add(arr.get(b));
					adj.get(arr.get(b)).add(arr.get(a));
				}
			}
		}
		int c = ACRYLIC_PIECE_COLORS.length;
		int[] colorIdx = new int[n];
		java.util.Arrays.fill(colorIdx, -1);
		int[] usage = new int[c];
		for (int i = 0; i < n; i++) {
			Set<Integer> banned = new HashSet<Integer>();
			for (int nb : adj.get(i)) {
				if (colorIdx[nb] >= 0)
					banned.add(colorIdx[nb]);
			}
			int best = -1;
			for (int k = 0; k < c; k++) {
				if (banned.contains(k))
					continue;
				if (best == -1 || usage[k] < usage[best])
					best = k;
			}
			if (best == -1) {
				best = 0;
				for (int k = 1; k < c; k++) {
					if (usage[k] < usage[best])
						best = k;
				}
			}
			colorIdx[i] = best;
			usage[best]++;
		}
		List<String> out = new ArrayList<String>();
		for (int i = 0; i < n; i++) {
			out.add(ACRYLIC_PIECE_COLORS[colorIdx[i]]);
		}
		return out;
	}

	/** Frame path drawn as a Bark Brown edge (for the coloured solution sheet). */
	private static String coloredFrameElement(String frameD) {
		return "<path fill=\"none\" stroke=\"" + BARK_BROWN + "\" stroke-width=\"0.6\" d=\"" + frameD + "\"></path>";
	}

	/**
	 * Colored export: filled piece shapes for the box cover / solution sheet. Each
	 * piece gets a bright acrylic colour, assigned so neighbouring pieces never
	 * match; the outer edge is Bark Brown.
	 */
	public String exportSvgColored(double frame, double rad, TileShape shape, double frameCorner, double coloringSeed) {
		StringBuilder data = new StringBuilder(svgHeader(frame, rad));
		List<String> colors = assignPieceColors(frame, rad);
		List<String> paths = multipaths(frame, rad, shape);
		for (int i = 0; i < paths.size(); i++) {
			data.append("<path fill=\"").append(colors.get(i)).append("\" stroke=\"").append(BARK_BROWN)
					.append("\" stroke-width=\"").append(Svg.fmt(rad / 20.0)).append("\" d=\"").append(paths.get(i))
					.append("\"></path>");
		}
		data.append(coloredFrameElement(createFrame(frame, rad, frameCorner)));
		data.append("</svg>");
		return data.toString();
	}

	// ---------------------------------------------------------------------------
	// Circular ("coaster") panel support.
	//
	// The piece geometry above is panel-agnostic. The overloads below add a second
	// panel shape: a round disc. When the requested panel is SQUARE they delegate
	// to the original methods verbatim, so the square output (and the golden-file
	// tests that pin it) is unaffected. When the panel is CIRCLE they emit a round
	// frame, a square SVG canvas sized to the diameter and centred on the disc,
	// and clip the pieces to the disc so the coaster has a clean round edge.
	// ---------------------------------------------------------------------------

	private static final String PANEL_CLIP_ID = "panel-clip";

	/**
	 * Grid centre X in millimetres (pieces are inset from the canvas by
	 * {@code frame}).
	 */
	private double gridCenterX(double frame, double rad) {
		return frame + ncols * rad;
	}

	/** Grid centre Y in millimetres. */
	private double gridCenterY(double frame, double rad) {
		return frame + nrows * rad;
	}

	/**
	 * Frame cut lines for a circular panel: the inner edge of the frame ring (only
	 * when {@code frame > 0}) plus the outer disc edge. Both are real cut paths so
	 * the frame is a solid annulus around the clipped puzzle.
	 */
	private String circlePanelFrameElements(double cx, double cy, double outerR, double innerR, double frame) {
		StringBuilder sb = new StringBuilder();
		if (frame > 0) {
			sb.append(framePathElement(circleFramePath(cx, cy, innerR)));
		}
		sb.append(framePathElement(circleFramePath(cx, cy, outerR)));
		return sb.toString();
	}

	/** Closed circle outline as an SVG path "d" string, centred on (cx, cy). */
	private static String circleFramePath(double cx, double cy, double r) {
		StringBuilder d = new StringBuilder();
		d.append("M").append(Svg.fmt(cx - r)).append(",").append(Svg.fmt(cy)).append(" ");
		d.append("A ").append(Svg.fmt(r)).append(" ").append(Svg.fmt(r)).append(" 0 0,1 ").append(Svg.fmt(cx + r))
				.append(" ").append(Svg.fmt(cy)).append(" ");
		d.append("A ").append(Svg.fmt(r)).append(" ").append(Svg.fmt(r)).append(" 0 0,1 ").append(Svg.fmt(cx - r))
				.append(" ").append(Svg.fmt(cy)).append(" ");
		d.append("Z");
		return d.toString();
	}

	/**
	 * SVG header for a circular panel: square canvas of {@code diameter}, centred
	 * on the disc.
	 */
	private String svgHeaderCircle(double cx, double cy, double r, double diameter, String circleD) {
		double minX = cx - r;
		double minY = cy - r;
		return "<?xml version=\"1.0\" encoding=\"utf-8\" ?><svg baseProfile=\"full\" height=\"" + Svg.fmt(diameter)
				+ "mm\" version=\"1.1\" viewBox=\"" + Svg.fmt(minX) + " " + Svg.fmt(minY) + " " + Svg.fmt(diameter)
				+ " " + Svg.fmt(diameter) + "\" width=\"" + Svg.fmt(diameter)
				+ "mm\" xmlns=\"http://www.w3.org/2000/svg\"" + " xmlns:ev=\"http://www.w3.org/2001/xml-events\""
				+ " xmlns:xlink=\"http://www.w3.org/1999/xlink\"><defs><clipPath id=\"" + PANEL_CLIP_ID
				+ "\"><path d=\"" + circleD + "\"></path></clipPath></defs>";
	}

	/**
	 * Frame outline path for either panel shape. SQUARE returns the original
	 * rounded-rectangle; CIRCLE returns a disc of the given diameter centred on the
	 * puzzle grid.
	 */
	public String createFrame(double frame, double rad, double frameCorner, PanelShape panel, double diameter) {
		if (panel != PanelShape.CIRCLE) {
			return createFrame(frame, rad, frameCorner);
		}
		double r = diameter / 2.0;
		return circleFramePath(gridCenterX(frame, rad), gridCenterY(frame, rad), r);
	}

	/** Overlapping export, panel-aware. */
	public String exportSvg(double frame, double rad, TileShape shape, double frameCorner, PanelShape panel,
			double diameter) {
		if (panel != PanelShape.CIRCLE) {
			return exportSvg(frame, rad, shape, frameCorner);
		}
		double outerR = diameter / 2.0;
		double innerR = Math.max(0, outerR - frame);
		double cx = gridCenterX(frame, rad);
		double cy = gridCenterY(frame, rad);
		String clipD = circleFramePath(cx, cy, innerR);
		StringBuilder data = new StringBuilder(svgHeaderCircle(cx, cy, outerR, diameter, clipD));
		data.append("<g clip-path=\"url(#").append(PANEL_CLIP_ID).append(")\">");
		for (List<DiagonalConnection> p : pieces) {
			List<Arc> arcs = new ArrayList<Arc>();
			addArcs(p.get(0), p, arcs, rad, frame, true);
			data.append("<path fill=\"none\" stroke=\"black\" stroke-width=\"0.1\" d=\"M")
					.append(Svg.fmt(arcs.get(0).spx())).append(",").append(Svg.fmt(arcs.get(0).spy())).append(" ");
			for (Arc a : arcs) {
				data.append(a.svg(shape));
			}
			data.append("Z\"></path>");
		}
		data.append("</g>");
		data.append(circlePanelFrameElements(cx, cy, outerR, innerR, frame));
		data.append("</svg>");
		return data.toString();
	}

	/** Non-overlapping export, panel-aware. */
	public String exportSvgNoOverlap(double frame, double rad, TileShape shape, double frameCorner, PanelShape panel,
			double diameter) {
		if (panel != PanelShape.CIRCLE) {
			return exportSvgNoOverlap(frame, rad, shape, frameCorner);
		}
		double outerR = diameter / 2.0;
		double innerR = Math.max(0, outerR - frame);
		double cx = gridCenterX(frame, rad);
		double cy = gridCenterY(frame, rad);
		String clipD = circleFramePath(cx, cy, innerR);
		StringBuilder data = new StringBuilder(svgHeaderCircle(cx, cy, outerR, diameter, clipD));
		data.append("<g clip-path=\"url(#").append(PANEL_CLIP_ID).append(")\">");
		Set<String> allArcs = new LinkedHashSet<String>();
		for (List<DiagonalConnection> p : pieces) {
			boolean inPath = false;
			StringBuilder path = new StringBuilder();
			List<Arc> arcs = new ArrayList<Arc>();
			addArcs(p.get(0), p, arcs, rad, frame, true);
			for (Arc a : arcs) {
				if (allArcs.contains(a.key())) {
					if (inPath) {
						path.append("\"></path>");
						data.append(path);
						inPath = false;
					}
				} else {
					allArcs.add(a.key());
					if (!inPath) {
						path = new StringBuilder("<path fill=\"none\" stroke=\"black\" stroke-width=\"0.1\" d=\"M")
								.append(Svg.fmt(a.spx())).append(",").append(Svg.fmt(a.spy())).append(" ");
						inPath = true;
					}
					path.append(a.svg(shape));
				}
			}
			if (inPath) {
				path.append("\"></path>");
				data.append(path);
			}
		}
		data.append("</g>");
		data.append(circlePanelFrameElements(cx, cy, outerR, innerR, frame));
		data.append("</svg>");
		return data.toString();
	}

	/** Single-path non-overlapping export, panel-aware. */
	public String exportSvgNoOverlapSinglePath(double frame, double rad, TileShape shape, double frameCorner,
			PanelShape panel, double diameter) {
		if (panel != PanelShape.CIRCLE) {
			return exportSvgNoOverlapSinglePath(frame, rad, shape, frameCorner);
		}
		double outerR = diameter / 2.0;
		double innerR = Math.max(0, outerR - frame);
		double cx = gridCenterX(frame, rad);
		double cy = gridCenterY(frame, rad);
		String clipD = circleFramePath(cx, cy, innerR);
		StringBuilder data = new StringBuilder(svgHeaderCircle(cx, cy, outerR, diameter, clipD));
		data.append("<g clip-path=\"url(#").append(PANEL_CLIP_ID).append(")\">");
		Set<String> allArcs = new LinkedHashSet<String>();
		data.append("<path fill=\"none\" stroke=\"black\" stroke-width=\"0.1\" d=\"");
		double curX = -1, curY = -1;
		for (List<DiagonalConnection> p : pieces) {
			List<Arc> arcs = new ArrayList<Arc>();
			addArcs(p.get(0), p, arcs, rad, frame, true);
			for (Arc a : arcs) {
				if (!allArcs.contains(a.key())) {
					allArcs.add(a.key());
					if (!a.spEquals(curX, curY)) {
						data.append("M").append(Svg.fmt(a.spx())).append(",").append(Svg.fmt(a.spy())).append(" ");
					}
					data.append(a.svg(shape));
					curX = a.epx();
					curY = a.epy();
				}
			}
		}
		data.append("\"></path>");
		data.append("</g>");
		data.append(circlePanelFrameElements(cx, cy, outerR, innerR, frame));
		data.append("</svg>");
		return data.toString();
	}

	/**
	 * Bark Brown inner + outer ring edges for the coloured circular solution sheet.
	 */
	private String coloredCirclePanelFrameElements(double cx, double cy, double outerR, double innerR, double frame) {
		StringBuilder sb = new StringBuilder();
		if (frame > 0) {
			sb.append(coloredFrameElement(circleFramePath(cx, cy, innerR)));
		}
		sb.append(coloredFrameElement(circleFramePath(cx, cy, outerR)));
		return sb.toString();
	}

	/** Colored export, panel-aware. */
	public String exportSvgColored(double frame, double rad, TileShape shape, double frameCorner, double coloringSeed,
			PanelShape panel, double diameter) {
		if (panel != PanelShape.CIRCLE) {
			return exportSvgColored(frame, rad, shape, frameCorner, coloringSeed);
		}
		double outerR = diameter / 2.0;
		double innerR = Math.max(0, outerR - frame);
		double cx = gridCenterX(frame, rad);
		double cy = gridCenterY(frame, rad);
		String clipD = circleFramePath(cx, cy, innerR);
		StringBuilder data = new StringBuilder(svgHeaderCircle(cx, cy, outerR, diameter, clipD));
		data.append("<g clip-path=\"url(#").append(PANEL_CLIP_ID).append(")\">");
		List<String> colors = assignPieceColors(frame, rad);
		List<String> paths = multipaths(frame, rad, shape);
		for (int i = 0; i < paths.size(); i++) {
			data.append("<path fill=\"").append(colors.get(i)).append("\" stroke=\"").append(BARK_BROWN)
					.append("\" stroke-width=\"").append(Svg.fmt(rad / 20.0)).append("\" d=\"").append(paths.get(i))
					.append("\"></path>");
		}
		data.append("</g>");
		data.append(coloredCirclePanelFrameElements(cx, cy, outerR, innerR, frame));
		data.append("</svg>");
		return data.toString();
	}
}
