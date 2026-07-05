package com.fractalforge.puzzle.generator;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the round ("coaster") panel option added alongside the square tray.
 */
class CirclePanelTest {

	private CircleFractalJigsaw generate(double seed, int ncols, int nrows, int minP, int maxP) {
		CircleFractalJigsaw jig = new CircleFractalJigsaw(ncols, nrows, minP, maxP, new SinRandom(seed));
		jig.generateAll();
		return jig;
	}

	@Test
	void squarePanelArgIsIdenticalToLegacyOutput() {
		CircleFractalJigsaw jig = generate(42, 8, 8, 4, 12);
		// Passing SQUARE (with any diameter) must reproduce the legacy export
		// byte-for-byte.
		assertEquals(jig.exportSvg(6, 6, TileShape.CIRCULAR, 4),
				jig.exportSvg(6, 6, TileShape.CIRCULAR, 4, PanelShape.SQUARE, 110));
		assertEquals(jig.exportSvgNoOverlap(6, 6, TileShape.CIRCULAR, 4),
				jig.exportSvgNoOverlap(6, 6, TileShape.CIRCULAR, 4, PanelShape.SQUARE, 110));
		assertEquals(jig.exportSvgNoOverlapSinglePath(6, 6, TileShape.CIRCULAR, 4),
				jig.exportSvgNoOverlapSinglePath(6, 6, TileShape.CIRCULAR, 4, PanelShape.SQUARE, 110));
		assertEquals(jig.exportSvgColored(6, 6, TileShape.CIRCULAR, 4, 4242),
				jig.exportSvgColored(6, 6, TileShape.CIRCULAR, 4, 4242, PanelShape.SQUARE, 110));
		assertEquals(jig.createFrame(6, 6, 4), jig.createFrame(6, 6, 4, PanelShape.SQUARE, 110));
	}

	@Test
	void circlePanelDiffersFromSquarePanel() {
		CircleFractalJigsaw jig = generate(42, 8, 8, 4, 12);
		String square = jig.exportSvg(6, 6, TileShape.CIRCULAR, 4, PanelShape.SQUARE, 110);
		String circle = jig.exportSvg(6, 6, TileShape.CIRCULAR, 4, PanelShape.CIRCLE, 110);
		assertNotEquals(square, circle);
	}

	@Test
	void circlePanelCanvasMatchesDiameterAndTrimsPieces() {
		CircleFractalJigsaw jig = generate(42, 8, 8, 4, 12);
		String svg = jig.exportSvg(6, 6, TileShape.CIRCULAR, 4, PanelShape.CIRCLE, 110);
		assertTrue(svg.contains("width=\"110mm\""), "coaster canvas should be the requested diameter");
		assertTrue(svg.contains("height=\"110mm\""), "coaster canvas should be the requested diameter");
		assertFalse(svg.contains("clipPath"), "geometry is trimmed, not masked with a clip path");
		assertFalse(svg.contains("clip-path"), "geometry is trimmed, not masked with a clip path");
	}

	/**
	 * Every coordinate of every path in the export must lie within the disc:
	 * trimmed piece geometry never extends past the inner puzzle circle, and
	 * nothing at all extends past the outer coaster edge. (Kept sub-arcs cannot
	 * bulge outside between endpoints — a crossing would have split them — so
	 * endpoint checks suffice.)
	 */
	@Test
	void trimmedExportsContainNoGeometryOutsideTheDisc() {
		CircleFractalJigsaw jig = generate(42, 8, 8, 4, 12);
		double cx = 54, cy = 54, outerR = 55, innerR = 49; // frame=6, diameter=110
		TileShape[] shapes = {TileShape.CIRCULAR, TileShape.SQUARE, TileShape.OCTAGONAL};
		for (TileShape shape : shapes) {
			String[] svgs = {jig.exportSvg(6, 6, shape, 4, PanelShape.CIRCLE, 110),
					jig.exportSvgNoOverlap(6, 6, shape, 4, PanelShape.CIRCLE, 110),
					jig.exportSvgNoOverlapSinglePath(6, 6, shape, 4, PanelShape.CIRCLE, 110),
					jig.exportSvgColored(6, 6, shape, 4, 4242, PanelShape.CIRCLE, 110)};
			for (String svg : svgs) {
				String outerFrameD = "M-1,54 A 55 55 0 0,1 109 54 A 55 55 0 0,1 -1 54 Z";
				for (String d : extractPathData(svg)) {
					double limit = d.equals(outerFrameD) ? outerR : innerR;
					for (double[] pt : pathCoords(d)) {
						double dist = Math.hypot(pt[0] - cx, pt[1] - cy);
						assertTrue(dist <= limit + 1e-6, shape + ": point (" + pt[0] + "," + pt[1]
								+ ") lies outside radius " + limit + " (dist=" + dist + ")");
					}
				}
			}
		}
	}

	private static java.util.List<String> extractPathData(String svg) {
		java.util.List<String> out = new java.util.ArrayList<>();
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("d=\"([^\"]*)\"").matcher(svg);
		while (m.find()) {
			out.add(m.group(1));
		}
		return out;
	}

	/**
	 * Extracts the on-path coordinates (M/L targets and arc endpoints) from a path
	 * "d" string as emitted by the generator (M, L, A, Z commands only).
	 */
	private static java.util.List<double[]> pathCoords(String d) {
		java.util.List<double[]> pts = new java.util.ArrayList<>();
		String[] tok = d.trim().split("\\s+");
		for (int i = 0; i < tok.length; i++) {
			String t = tok[i];
			if (t.startsWith("M")) {
				String[] xy = t.substring(1).split(",");
				pts.add(new double[]{Double.parseDouble(xy[0]), Double.parseDouble(xy[1])});
			} else if (t.equals("A")) {
				// A rx ry xrot largeArc,sweep x y
				pts.add(new double[]{Double.parseDouble(tok[i + 5]), Double.parseDouble(tok[i + 6])});
				i += 6;
			} else if (t.equals("L")) {
				pts.add(new double[]{Double.parseDouble(tok[i + 1]), Double.parseDouble(tok[i + 2])});
				i += 2;
			}
			// "Z" carries no coordinates
		}
		return pts;
	}

	@Test
	void circleFrameIsArcBasedNotRoundedRectangle() {
		CircleFractalJigsaw jig = generate(42, 8, 8, 4, 12);
		String frame = jig.createFrame(6, 6, 4, PanelShape.CIRCLE, 110);
		// Disc is the grid centre (6 + 8*6 = 54, 54) with radius 55.
		assertTrue(frame.startsWith("M-1,54 "), "frame should start at the disc's left edge: " + frame);
		assertTrue(frame.contains("A 55 55 0 0,1"), "frame should be drawn from circular arcs: " + frame);
	}

	@Test
	void circlePanelHasInnerRingAndOuterFrameEdge() {
		CircleFractalJigsaw jig = generate(42, 8, 8, 4, 12);
		// frame=6, diameter=110 => outerR=55, innerR=49, disc centre (54,54).
		String svg = jig.exportSvgNoOverlap(6, 6, TileShape.CIRCULAR, 4, PanelShape.CIRCLE, 110);
		// Both the inner frame edge (radius 55 - 6 = 49) and the outer coaster
		// edge are real cut paths.
		assertTrue(svg.contains("d=\"M5,54 A 49 49 0 0,1"), "inner frame edge missing: " + svg);
		assertTrue(svg.contains("A 55 55 0 0,1"), "outer coaster edge missing");
	}

	@Test
	void frameRingRadiusTracksFrameSize() {
		CircleFractalJigsaw jig = generate(42, 8, 8, 4, 12);
		// frame=10 => disc centre (10+48, 10+48) = (58,58), outerR=55, innerR=45.
		String svg = jig.exportSvgNoOverlap(10, 6, TileShape.CIRCULAR, 4, PanelShape.CIRCLE, 110);
		assertTrue(svg.contains("d=\"M13,58 A 45 45 0 0,1"), "inner frame edge should follow the frame size: " + svg);
		assertTrue(svg.contains("A 55 55 0 0,1"), "outer coaster edge missing");
	}

	@Test
	void allFourCircleExportsAreWellFormedXml() throws Exception {
		CircleFractalJigsaw jig = generate(7, 10, 10, 4, 25);
		double coloringSeed = 7 + 1000;
		String[] svgs = {jig.exportSvg(6, 6, TileShape.OCTAGONAL, 4, PanelShape.CIRCLE, 120),
				jig.exportSvgNoOverlap(6, 6, TileShape.SQUARE, 4, PanelShape.CIRCLE, 120),
				jig.exportSvgNoOverlapSinglePath(6, 6, TileShape.CIRCULAR, 4, PanelShape.CIRCLE, 120),
				jig.exportSvgColored(6, 6, TileShape.CIRCULAR, 4, coloringSeed, PanelShape.CIRCLE, 120),};
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		for (String svg : svgs) {
			Document doc = factory.newDocumentBuilder()
					.parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
			assertEquals("svg", doc.getDocumentElement().getLocalName());
		}
	}
}
