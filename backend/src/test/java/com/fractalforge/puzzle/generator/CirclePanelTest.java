package com.fractalforge.puzzle.generator;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	void circlePanelCanvasMatchesDiameterAndClipsPieces() {
		CircleFractalJigsaw jig = generate(42, 8, 8, 4, 12);
		String svg = jig.exportSvg(6, 6, TileShape.CIRCULAR, 4, PanelShape.CIRCLE, 110);
		assertTrue(svg.contains("width=\"110mm\""), "coaster canvas should be the requested diameter");
		assertTrue(svg.contains("height=\"110mm\""), "coaster canvas should be the requested diameter");
		assertTrue(svg.contains("<clipPath id=\"panel-clip\">"), "pieces must be clipped to the disc");
		assertTrue(svg.contains("clip-path=\"url(#panel-clip)\""), "pieces must reference the clip");
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
	void circlePanelHasInnerClipRingAndOuterFrameEdge() {
		CircleFractalJigsaw jig = generate(42, 8, 8, 4, 12);
		// frame=6, diameter=110 => outerR=55, innerR=49, disc centre (54,54).
		String svg = jig.exportSvgNoOverlap(6, 6, TileShape.CIRCULAR, 4, PanelShape.CIRCLE, 110);
		// Pieces are clipped to the inner puzzle circle (radius 55 - 6 = 49)...
		assertTrue(svg.contains("<clipPath id=\"panel-clip\"><path d=\"M5,54 A 49 49 0 0,1"),
				"clip should use the inner radius (outerR - frame): " + svg);
		// ...and both the inner frame edge and the outer coaster edge are cut.
		assertTrue(svg.contains("A 49 49 0 0,1"), "inner frame edge missing");
		assertTrue(svg.contains("A 55 55 0 0,1"), "outer coaster edge missing");
	}

	@Test
	void frameRingRadiusTracksFrameSize() {
		CircleFractalJigsaw jig = generate(42, 8, 8, 4, 12);
		// frame=10 => disc centre (10+48, 10+48) = (58,58), outerR=55, innerR=45.
		String svg = jig.exportSvgNoOverlap(10, 6, TileShape.CIRCULAR, 4, PanelShape.CIRCLE, 110);
		assertTrue(svg.contains("<clipPath id=\"panel-clip\"><path d=\"M13,58 A 45 45 0 0,1"),
				"inner clip radius should follow the frame size: " + svg);
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
