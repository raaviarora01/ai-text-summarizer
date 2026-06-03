package com.raavi.ai.ai_text_processor.serviceImpl;

import com.raavi.ai.ai_text_processor.aspect.CacheAspect;
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
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TextSummarizerServiceImpl implements TextSummarizerService {

    private static final Logger logger = LoggerFactory.getLogger(TextSummarizerServiceImpl.class);

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private TextSummaryRepository textSummaryRepository;

    @Autowired
    private CacheAspect cacheAspect;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${gemini.model}")
    private String geminiModel;

    /**
     * Returns the Spring-proxied version of this service so @Cacheable fires correctly.
     * Direct this.method() calls bypass the Spring AOP proxy and skip caching entirely.
     */
    private TextSummarizerServiceImpl self() {
        return applicationContext.getBean(TextSummarizerServiceImpl.class);
    }

    // ─── summarizeText ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SummarizeResponse summarizeText(SummarizeRequest request) {
        validateRequest(request);

        AtomicBoolean methodExecuted = new AtomicBoolean(false);
        try {
            SummarizeResponse response = self().summarizeTextCached(request, methodExecuted);
            if (!methodExecuted.get()) {
                cacheAspect.recordCacheHit("summarizeText");
                logger.info("✅ CACHE HIT — summarizeText");
            }
            return response;
        } catch (IllegalArgumentException | GeminiApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to summarize text: " + e.getMessage(), e);
        }
    }

    @Cacheable(
            value = "textSummaries",
            key = "T(com.raavi.ai.ai_text_processor.util.CacheKeyGenerator).generateCacheKey(#request.text, #request.summaryType != null ? #request.summaryType : 'concise')",
            unless = "#result == null"
    )
    public SummarizeResponse summarizeTextCached(SummarizeRequest request, AtomicBoolean methodExecuted) {
        methodExecuted.set(true);

        String summaryTypeStr = request.getSummaryType() != null ?
                request.getSummaryType() : SummaryType.CONCISE.getType();
        SummaryType summaryType = SummaryType.fromString(summaryTypeStr);

        String cacheKey = CacheKeyGenerator.generateCacheKey(request.getText(), summaryTypeStr);
        logger.info("❌ CACHE MISS — key: {}...", cacheKey.substring(0, Math.min(16, cacheKey.length())));

        long apiStart = System.currentTimeMillis();
        GeminiService.GeminiResponse geminiResponse = geminiService.summarizeText(
                request.getText(), summaryType.getPrompt());
        logger.info("✓ Gemini API call completed in {}ms | Tokens: {}",
                System.currentTimeMillis() - apiStart, geminiResponse.getTokensUsed());

        TextSummary textSummary = new TextSummary();
        textSummary.setOriginalText(request.getText());
        textSummary.setSummarizedText(geminiResponse.getSummary());
        textSummary.setSummaryType(summaryType);
        textSummary.setTokensUsed(geminiResponse.getTokensUsed());
        textSummary.setModelUsed(geminiModel);

        TextSummary saved = textSummaryRepository.save(textSummary);
        logger.info("✓ Summary saved to DB with ID: {} | Now cached", saved.getId());
        return mapToResponse(saved);
    }

    // ─── getHistory ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<SummarizeResponse> getHistory(Pageable pageable) {
        AtomicBoolean methodExecuted = new AtomicBoolean(false);
        Page<SummarizeResponse> result = self().getHistoryCached(pageable, methodExecuted);
        if (!methodExecuted.get()) {
            cacheAspect.recordCacheHit("getHistory");
            logger.info("✅ CACHE HIT — getHistory page={}", pageable.getPageNumber());
        }
        return result;
    }

    @Cacheable(
            value = "summaryHistory",
            key = "'page-' + #pageable.getPageNumber() + '-size-' + #pageable.getPageSize()",
            unless = "#result == null"
    )
    public Page<SummarizeResponse> getHistoryCached(Pageable pageable, AtomicBoolean methodExecuted) {
        methodExecuted.set(true);
        logger.info("❌ CACHE MISS — getHistory page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        return textSummaryRepository.findAll(pageable).map(this::mapToResponse);
    }

    // ─── getHistoryByType ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<SummarizeResponse> getHistoryByType(String summaryType, Pageable pageable) {
        AtomicBoolean methodExecuted = new AtomicBoolean(false);
        Page<SummarizeResponse> result = self().getHistoryByTypeCached(summaryType, pageable, methodExecuted);
        if (!methodExecuted.get()) {
            cacheAspect.recordCacheHit("getHistoryByType");
            logger.info("✅ CACHE HIT — getHistoryByType type={}", summaryType);
        }
        return result;
    }

    @Cacheable(
            value = "summaryHistoryByType",
            key = "'type-' + #summaryType + '-page-' + #pageable.getPageNumber() + '-size-' + #pageable.getPageSize()",
            unless = "#result == null"
    )
    public Page<SummarizeResponse> getHistoryByTypeCached(String summaryType, Pageable pageable,
                                                          AtomicBoolean methodExecuted) {
        methodExecuted.set(true);
        SummaryType type = SummaryType.fromString(summaryType);
        logger.info("❌ CACHE MISS — getHistoryByType type={}, page={}", type, pageable.getPageNumber());
        return textSummaryRepository.findBySummaryType(type, pageable).map(this::mapToResponse);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private void validateRequest(SummarizeRequest request) {
        if (request == null)
            throw new IllegalArgumentException("Request cannot be null");
        if (request.getText() == null || request.getText().trim().isEmpty())
            throw new IllegalArgumentException("Text cannot be null or empty");
        if (request.getText().length() < 10)
            throw new IllegalArgumentException("Text must be at least 10 characters long");
        if (request.getText().length() > 50000)
            throw new IllegalArgumentException("Text must not exceed 50000 characters");
    }

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
