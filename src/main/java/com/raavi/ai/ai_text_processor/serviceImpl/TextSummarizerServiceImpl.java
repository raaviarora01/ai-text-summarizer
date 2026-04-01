package com.raavi.ai.ai_text_processor.serviceImpl;

import com.raavi.ai.ai_text_processor.dao.TextSummaryRepository;
import com.raavi.ai.ai_text_processor.dto.SummarizeRequest;
import com.raavi.ai.ai_text_processor.dto.SummarizeResponse;
import com.raavi.ai.ai_text_processor.entity.TextSummary;
import com.raavi.ai.ai_text_processor.enums.SummaryType;
import com.raavi.ai.ai_text_processor.exception.GeminiApiException;
import com.raavi.ai.ai_text_processor.service.GeminiService;
import com.raavi.ai.ai_text_processor.service.TextSummarizerService;
import com.raavi.ai.ai_text_processor.util.CacheKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(
        value = "textSummaries",
        key = "T(com.raavi.ai.ai_text_processor.util.CacheKeyGenerator).generateCacheKey(#request.text, #request.summaryType != null ? #request.summaryType : 'CONCISE')",
        unless = "#result == null"
    )
    public SummarizeResponse summarizeText(SummarizeRequest request) {
        try {
            // Validate request
            validateRequest(request);
            
            // Parse summary type (default to CONCISE if not provided)
            String summaryTypeStr = request.getSummaryType() != null ? 
                    request.getSummaryType() : 
                    SummaryType.CONCISE.getType();
            
            SummaryType summaryType = SummaryType.fromString(summaryTypeStr);
            
            // Log cache entry point with detailed information
            String cacheKey = CacheKeyGenerator.generateCacheKey(request.getText(), summaryTypeStr);
            logger.info(
                "=== CACHE LOOKUP ===\n" +
                "  Cache Key: {}\n" +
                "  Text Length: {} characters\n" +
                "  Summary Type: {}\n" +
                "  Status: Checking cache...",
                cacheKey.substring(0, Math.min(16, cacheKey.length())) + "...", 
                request.getText().length(), 
                summaryType
            );
            
            logger.debug("Processing summarization request with type: {}", summaryType);
            
            // Get the prompt for this summary type
            String prompt = summaryType.getPrompt();
            
            // Call Gemini API via GeminiService
            logger.info("⏳ CACHE MISS DETECTED - Calling Gemini API to generate new summary...");
            long apiStartTime = System.currentTimeMillis();
            
            GeminiService.GeminiResponse geminiResponse = geminiService.summarizeText(
                    request.getText(), 
                    prompt
            );
            
            long apiExecutionTime = System.currentTimeMillis() - apiStartTime;
            logger.info("✓ Gemini API call completed in {}ms | Tokens Used: {}", 
                    apiExecutionTime, geminiResponse.getTokensUsed());
            
            // Create and save TextSummary entity
            TextSummary textSummary = new TextSummary();
            textSummary.setOriginalText(request.getText());
            textSummary.setSummarizedText(geminiResponse.getSummary());
            textSummary.setSummaryType(summaryType);
            textSummary.setTokensUsed(geminiResponse.getTokensUsed());
            textSummary.setModelUsed(geminiModel);
            
            TextSummary savedSummary = textSummaryRepository.save(textSummary);
            
            logger.info("✓ Summary saved to database with ID: {} | This result is now CACHED", savedSummary.getId());
            
            // Convert to response DTO
            SummarizeResponse response = mapToResponse(savedSummary);
            
            logger.info(
                "=== CACHE STORAGE ===\n" +
                "  Summary ID: {}\n" +
                "  Status: Result cached successfully\n" +
                "  Next identical request will use cached result",
                savedSummary.getId()
            );
            
            return response;
            
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
    @Cacheable(
        value = "summaryHistory",
        key = "'page-' + #pageable.getPageNumber() + '-size-' + #pageable.getPageSize()",
        unless = "#result == null"
    )
    public Page<SummarizeResponse> getHistory(Pageable pageable) {
        logger.info(
            "=== CACHE LOOKUP (GET HISTORY) ===\n" +
            "  Cache Key: page-{}-size-{}\n" +
            "  Status: Checking cache...",
            pageable.getPageNumber(), pageable.getPageSize()
        );
        
        logger.info("⏳ CACHE MISS DETECTED - Fetching from database: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        Page<TextSummary> summaries = textSummaryRepository.findAll(pageable);
        
        logger.info("✓ Retrieved {} summaries from database | Result is now CACHED", 
                summaries.getNumberOfElements());
        
        return summaries.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
        value = "summaryHistoryByType",
        key = "'type-' + #summaryType + '-page-' + #pageable.getPageNumber() + '-size-' + #pageable.getPageSize()",
        unless = "#result == null"
    )
    public Page<SummarizeResponse> getHistoryByType(String summaryType, Pageable pageable) {
        try {
            SummaryType type = SummaryType.fromString(summaryType);
            
            logger.info(
                "=== CACHE LOOKUP (GET HISTORY BY TYPE) ===\n" +
                "  Cache Key: type-{}-page-{}-size-{}\n" +
                "  Status: Checking cache...",
                summaryType, pageable.getPageNumber(), pageable.getPageSize()
            );
            
            logger.info("⏳ CACHE MISS DETECTED - Fetching from database: type={}, page={}, size={}", 
                    type, pageable.getPageNumber(), pageable.getPageSize());
            
            Page<TextSummary> summaries = textSummaryRepository.findBySummaryType(type, pageable);
            
            logger.info("✓ Retrieved {} summaries by type '{}' from database | Result is now CACHED", 
                    summaries.getNumberOfElements(), summaryType);
            
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
