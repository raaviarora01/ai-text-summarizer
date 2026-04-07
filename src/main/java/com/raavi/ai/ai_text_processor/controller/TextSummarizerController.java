package com.raavi.ai.ai_text_processor.controller;

import com.raavi.ai.ai_text_processor.dto.ErrorResponse;
import com.raavi.ai.ai_text_processor.dto.SummarizeRequest;
import com.raavi.ai.ai_text_processor.dto.SummarizeResponse;
import com.raavi.ai.ai_text_processor.exception.GeminiApiException;
import com.raavi.ai.ai_text_processor.exception.RateLimitException;
import com.raavi.ai.ai_text_processor.service.TextSummarizerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/summarizer")
@CrossOrigin("*")
@Tag(name = "Text Summarizer", description = "AI-powered text summarization using Google Gemini")
public class TextSummarizerController {

    private static final Logger logger = LoggerFactory.getLogger(TextSummarizerController.class);

    @Autowired
    private TextSummarizerService textSummarizerService;

    @Operation(
            summary = "Summarize text",
            description = "Submits text to Google Gemini for summarization. Results are cached — " +
                    "identical text + summaryType combinations return instantly from cache. " +
                    "Rate limited to 10 requests per 60 seconds per IP."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary generated successfully",
                    content = @Content(schema = @Schema(implementation = SummarizeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request — text too short/long or blank",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"status\":429,\"errorCode\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Rate limit exceeded. Max 10 requests per 60 seconds.\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error or Gemini API failure",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/summarize")
    public ResponseEntity<?> summarize(@Valid @RequestBody SummarizeRequest request) {
        try {
            logger.info("Received summarization request");
            SummarizeResponse response = textSummarizerService.summarizeText(request);
            logger.info("Successfully generated summary with ID: {}", response.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error: {}", e.getMessage());
            ErrorResponse error = new ErrorResponse(400, e.getMessage(), "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(error);
        } catch (RateLimitException e) {
            logger.error("Rate limit exceeded: {}", e.getMessage());
            ErrorResponse error = new ErrorResponse(429, e.getMessage(), "RATE_LIMIT_EXCEEDED");
            error.setRetryAfterSeconds(e.getRetryAfterSeconds());
            return ResponseEntity.status(429).body(error);
        } catch (GeminiApiException e) {
            logger.error("Gemini API error: {}", e.getMessage());
            ErrorResponse error = new ErrorResponse(e.getHttpStatus(), e.getMessage(), "GEMINI_API_ERROR");
            return ResponseEntity.status(e.getHttpStatus()).body(error);
        } catch (Exception e) {
            logger.error("Unexpected error during summarization", e);
            ErrorResponse error = new ErrorResponse(500, "Internal server error", "INTERNAL_ERROR");
            return ResponseEntity.status(500).body(error);
        }
    }

    @Operation(
            summary = "Get summarization history",
            description = "Returns a paginated list of all previously generated summaries. " +
                    "Results are cached per page. Rate limited to 60 requests per 60 seconds per IP."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History retrieved successfully"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        try {
            logger.info("Fetching summarization history: page={}, size={}", page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<SummarizeResponse> history = textSummarizerService.getHistory(pageable);
            logger.info("Successfully retrieved {} summaries", history.getNumberOfElements());
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error retrieving history", e);
            ErrorResponse error = new ErrorResponse(500, "Failed to retrieve history", "INTERNAL_ERROR");
            return ResponseEntity.status(500).body(error);
        }
    }

    @Operation(
            summary = "Get history by summary type",
            description = "Returns a paginated list of summaries filtered by type. " +
                    "Valid types: concise, detailed, bullet_points, executive. " +
                    "Rate limited to 60 requests per 60 seconds per IP."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid summary type provided",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/history/by-type")
    public ResponseEntity<?> getHistoryByType(
            @Parameter(description = "Summary type to filter by", example = "concise",
                    schema = @Schema(allowableValues = {"concise", "detailed", "bullet_points", "executive"}))
            @RequestParam String summaryType,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        try {
            logger.info("Fetching summarization history by type: type={}, page={}, size={}", summaryType, page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<SummarizeResponse> history = textSummarizerService.getHistoryByType(summaryType, pageable);
            logger.info("Successfully retrieved {} summaries of type {}", history.getNumberOfElements(), summaryType);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid summary type: {}", summaryType);
            ErrorResponse error = new ErrorResponse(400, e.getMessage(), "INVALID_SUMMARY_TYPE");
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            logger.error("Error retrieving history by type", e);
            ErrorResponse error = new ErrorResponse(500, "Failed to retrieve history", "INTERNAL_ERROR");
            return ResponseEntity.status(500).body(error);
        }
    }
}