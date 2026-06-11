package com.fractalforge.puzzle.api;

import com.fractalforge.puzzle.generator.PuzzleSpec;
import com.fractalforge.puzzle.service.ExportMode;
import com.fractalforge.puzzle.service.GeneratedPuzzle;
import com.fractalforge.puzzle.service.PricingService;
import com.fractalforge.puzzle.service.PuzzleService;
import com.fractalforge.puzzle.shop.Material;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/puzzle")
public class PuzzleController {

	private final PuzzleService puzzleService;
	private final PricingService pricingService;

	public PuzzleController(PuzzleService puzzleService, PricingService pricingService) {
		this.puzzleService = puzzleService;
		this.pricingService = pricingService;
	}

	@PostMapping("/generate")
	public PuzzleResponse generate(@Valid @RequestBody PuzzleRequest request) {
		PuzzleSpec spec = request.toSpec();
		GeneratedPuzzle puzzle = puzzleService.generate(spec);
		Map<String, Long> quotes = new LinkedHashMap<>();
		pricingService.quoteAll(spec, puzzle.jigsaw().npieces())
				.forEach((Material m, Long cents) -> quotes.put(m.name(), cents));
		return new PuzzleResponse(puzzle.seedUsed(), spec.widthMm(), spec.heightMm(), puzzle.jigsaw().npieces(),
				puzzle.piecePaths(), puzzle.framePath(), quotes);
	}

	/**
	 * Exports the puzzle as an SVG file. mode=OVERLAP — individually contoured
	 * pieces (CNC); mode=NON_OVERLAP / NON_OVERLAP_SINGLE_PATH — laser-ready;
	 * mode=COLORED — solution/box-cover sheet.
	 */
	@PostMapping("/export")
	public ResponseEntity<String> export(@Valid @RequestBody PuzzleRequest request,
			@RequestParam(name = "mode", defaultValue = "NON_OVERLAP") ExportMode mode) {
		GeneratedPuzzle puzzle = puzzleService.generate(request.toSpec());
		String filename = "jigsaw-" + mode.name().toLowerCase() + "-seed" + (long) puzzle.seedUsed() + ".svg";
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(MediaType.valueOf("image/svg+xml")).body(puzzle.export(mode));
	}
}
