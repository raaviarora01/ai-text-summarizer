package com.raavi.ai.ai_text_processor.serviceImpl;

import com.raavi.ai.ai_text_processor.dao.TextSummaryRepository;
import com.raavi.ai.ai_text_processor.dto.SummarizeRequest;
import com.raavi.ai.ai_text_processor.dto.SummarizeResponse;
import com.raavi.ai.ai_text_processor.entity.TextSummary;
import com.raavi.ai.ai_text_processor.enums.SummaryType;
import com.raavi.ai.ai_text_processor.exception.GeminiApiException;
import com.raavi.ai.ai_text_processor.service.GeminiService;
import com.raavi.ai.ai_text_processor.service.TextSummarizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TextSummarizerServiceImpl implements TextSummarizerService {

    private static final Logger logger = LoggerFactory.getLogger(TextSummarizerServiceImpl.class);

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private TextSummaryRepository textSummaryRepository;

    @Value("${gemini.model}")
    private String geminiModel;

    @Override
    @Transactional
    public SummarizeResponse summarizeText(SummarizeRequest request) {
        try {
            // Validate request
            validateRequest(request);
            
            // Parse summary type (default to CONCISE if not provided)
            String summaryTypeStr = request.getSummaryType() != null ? 
                    request.getSummaryType() : 
                    SummaryType.CONCISE.getType();
            
            SummaryType summaryType = SummaryType.fromString(summaryTypeStr);
            
            logger.info("Processing summarization request with type: {}", summaryType);
            
            // Get the prompt for this summary type
            String prompt = summaryType.getPrompt();
            
            // Call Gemini API via GeminiService
            GeminiService.GeminiResponse geminiResponse = geminiService.summarizeText(
                    request.getText(), 
                    prompt
            );
            
            // Create and save TextSummary entity
            TextSummary textSummary = new TextSummary();
            textSummary.setOriginalText(request.getText());
            textSummary.setSummarizedText(geminiResponse.getSummary());
            textSummary.setSummaryType(summaryType);
            textSummary.setTokensUsed(geminiResponse.getTokensUsed());
            textSummary.setModelUsed(geminiModel);
            
            TextSummary savedSummary = textSummaryRepository.save(textSummary);
            
            logger.info("Successfully saved summary with ID: {}", savedSummary.getId());
            
            // Convert to response DTO
            return mapToResponse(savedSummary);
            
        } catch (IllegalArgumentException e) {
            logger.error("Validation error: {}", e.getMessage());
            throw e;
        } catch (GeminiApiException e) {
            logger.error("Gemini API error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during summarization", e);
            throw new RuntimeException("Failed to summarize text: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SummarizeResponse> getHistory(Pageable pageable) {
        logger.info("Fetching summarization history with pagination: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        Page<TextSummary> summaries = textSummaryRepository.findAll(pageable);
        
        return summaries.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SummarizeResponse> getHistoryByType(String summaryType, Pageable pageable) {
        try {
            SummaryType type = SummaryType.fromString(summaryType);
            
            logger.info("Fetching summarization history by type: type={}, page={}, size={}", 
                    type, pageable.getPageNumber(), pageable.getPageSize());
            
            Page<TextSummary> summaries = textSummaryRepository.findBySummaryType(type, pageable);
            
            return summaries.map(this::mapToResponse);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid summary type provided: {}", summaryType);
            throw e;
        }
    }

    /**
     * Validates the summarization request
     */
    private void validateRequest(SummarizeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        if (request.getText().length() < 10) {
            throw new IllegalArgumentException("Text must be at least 10 characters long");
        }
        
        if (request.getText().length() > 50000) {
            throw new IllegalArgumentException("Text must not exceed 50000 characters");
        }
    }

    /**
     * Maps TextSummary entity to SummarizeResponse DTO
     */
    private SummarizeResponse mapToResponse(TextSummary entity) {
        SummarizeResponse response = new SummarizeResponse();
        response.setId(entity.getId());
        response.setOriginalText(entity.getOriginalText());
        response.setSummarizedText(entity.getSummarizedText());
        response.setSummaryType(entity.getSummaryType().getType());
        response.setTokensUsed(entity.getTokensUsed());
        response.setModelUsed(entity.getModelUsed());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
