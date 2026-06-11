package com.fractalforge.puzzle.shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ShopApiTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@BeforeEach
	void setup() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	void catalogIsSeeded() throws Exception {
		mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)))
				.andExpect(jsonPath("$[0].name").isString())
				.andExpect(jsonPath("$[0].priceCents").value(greaterThan(0)));
	}

	@Test
	void unknownProductIs404() throws Exception {
		mvc.perform(get("/api/products/999")).andExpect(status().isNotFound());
	}

	@Test
	void createsOrderForCatalogProduct() throws Exception {
		String order = """
				{"customerName":"Ada Lovelace","email":"ada@example.com",
				 "addressLine":"1 Analytical Way","city":"London","postalCode":"N1 9GU","country":"GB",
				 "items":[{"productId":1,"quantity":2}]}
				""";
		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order))
				.andExpect(status().isOk()).andExpect(jsonPath("$.orderRef").isString())
				.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
				.andExpect(jsonPath("$.totalCents").value(9800));
	}

	@Test
	void pricesCustomPuzzleServerSide() throws Exception {
		String order = """
				{"customerName":"Ada Lovelace","email":"ada@example.com",
				 "addressLine":"1 Analytical Way","city":"London","postalCode":"N1 9GU","country":"GB",
				 "items":[{"customSpec":{"seed":42,"nonDeterministic":false,"ncols":8,"nrows":8,
				   "tileRadius":6.0,"frame":6.0,"frameCorner":4.0,"minPieceSize":4,"maxPieceSize":12,
				   "shape":"CIRCULAR"},"material":"WALNUT","quantity":1}]}
				""";
		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalCents").value(greaterThan(0)));
	}

	@Test
	void rejectsOrderWithoutEmail() throws Exception {
		String order = """
				{"customerName":"X","email":"not-an-email",
				 "addressLine":"1","city":"Y","postalCode":"1","country":"GB",
				 "items":[{"productId":1,"quantity":1}]}
				""";
		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsItemWithNeitherProductNorSpec() throws Exception {
		String order = """
				{"customerName":"X","email":"x@example.com",
				 "addressLine":"1","city":"Y","postalCode":"1","country":"GB",
				 "items":[{"quantity":1}]}
				""";
		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order))
				.andExpect(status().isBadRequest());
	}
}
