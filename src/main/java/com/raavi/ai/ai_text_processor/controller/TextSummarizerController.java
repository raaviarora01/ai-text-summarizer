package com.raavi.ai.ai_text_processor.controller;

import com.raavi.ai.ai_text_processor.dto.ErrorResponse;
import com.raavi.ai.ai_text_processor.dto.SummarizeRequest;
import com.raavi.ai.ai_text_processor.dto.SummarizeResponse;
import com.raavi.ai.ai_text_processor.exception.GeminiApiException;
import com.raavi.ai.ai_text_processor.exception.RateLimitException;
import com.raavi.ai.ai_text_processor.service.TextSummarizerService;
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
public class TextSummarizerController {

    private static final Logger logger = LoggerFactory.getLogger(TextSummarizerController.class);

    @Autowired
    private TextSummarizerService textSummarizerService;

    /**
     * Summarizes the provided text
     * 
     * @param request The summarization request containing text and optional summary type
     * @return The generated summary with metadata
     */
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

    /**
     * Retrieves history of all summaries with pagination
     * 
     * @param page The page number (0-indexed)
     * @param size The page size
     * @return Paginated list of summaries
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam(defaultValue = "0") int page,
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

    /**
     * Retrieves history of summaries filtered by summary type
     * 
     * @param summaryType The type of summary to filter by
     * @param page The page number (0-indexed)
     * @param size The page size
     * @return Paginated list of summaries of the specified type
     */
    @GetMapping("/history/by-type")
    public ResponseEntity<?> getHistoryByType(
            @RequestParam String summaryType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            logger.info("Fetching summarization history by type: type={}, page={}, size={}", 
                    summaryType, page, size);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<SummarizeResponse> history = textSummarizerService.getHistoryByType(summaryType, pageable);
            
            logger.info("Successfully retrieved {} summaries of type {}", 
                    history.getNumberOfElements(), summaryType);
            
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
