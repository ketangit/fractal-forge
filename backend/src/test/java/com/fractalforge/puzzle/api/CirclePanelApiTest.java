package com.fractalforge.puzzle.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API tests for the circular ("coaster") panel option. */
@SpringBootTest
class CirclePanelApiTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@BeforeEach
	void setup() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	private static final String CIRCLE = """
			{"seed":42,"nonDeterministic":false,"ncols":8,"nrows":8,
			 "tileRadius":6.0,"frame":6.0,"frameCorner":4.0,
			 "minPieceSize":4,"maxPieceSize":12,"shape":"CIRCULAR",
			 "panelShape":"CIRCLE","panelDiameter":110.0}
			""";

	@Test
	void circlePanelReportsDiameterAsCanvasSize() throws Exception {
		mvc.perform(post("/api/puzzle/generate").contentType(MediaType.APPLICATION_JSON).content(CIRCLE))
				.andExpect(status().isOk()).andExpect(jsonPath("$.widthMm").value(110.0))
				.andExpect(jsonPath("$.heightMm").value(110.0))
				.andExpect(jsonPath("$.priceCentsByMaterial.BIRCH_PLY").value(greaterThan(0)));
	}

	@Test
	void circlePanelExportsSvgWithDiscFrame() throws Exception {
		String svg = mvc
				.perform(post("/api/puzzle/export?mode=NON_OVERLAP").contentType(MediaType.APPLICATION_JSON)
						.content(CIRCLE))
				.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("image/svg+xml")).andReturn()
				.getResponse().getContentAsString();
		org.junit.jupiter.api.Assertions.assertTrue(svg.contains("width=\"110mm\""));
		org.junit.jupiter.api.Assertions.assertTrue(svg.contains("clip-path=\"url(#panel-clip)\""));
	}

	@Test
	void omittedPanelDefaultsToSquare() throws Exception {
		String square = """
				{"seed":42,"nonDeterministic":false,"ncols":8,"nrows":8,
				 "tileRadius":6.0,"frame":6.0,"frameCorner":4.0,
				 "minPieceSize":4,"maxPieceSize":12,"shape":"CIRCULAR"}
				""";
		mvc.perform(post("/api/puzzle/generate").contentType(MediaType.APPLICATION_JSON).content(square))
				.andExpect(status().isOk()).andExpect(jsonPath("$.widthMm").value(108.0))
				.andExpect(jsonPath("$.heightMm").value(108.0));
	}
}
