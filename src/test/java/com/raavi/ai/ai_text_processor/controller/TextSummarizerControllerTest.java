package com.raavi.ai.ai_text_processor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raavi.ai.ai_text_processor.dto.SummarizeRequest;
import com.raavi.ai.ai_text_processor.dto.SummarizeResponse;
import com.raavi.ai.ai_text_processor.exception.GeminiApiException;
import com.raavi.ai.ai_text_processor.exception.RateLimitException;
import com.raavi.ai.ai_text_processor.service.TextSummarizerService;
import com.raavi.ai.ai_text_processor.service.GeminiService;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Controller slice tests for TextSummarizerController.
 * Uses @WebMvcTest — only the web layer is loaded, no DB or Gemini.
 * Tests all HTTP methods, status codes, request/response bodies,
 * and exception-to-status-code mappings.
 */
@WebMvcTest(TextSummarizerController.class)
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = "spring.cache.type=none")
@DisplayName("TextSummarizerController Tests")
public class TextSummarizerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TextSummarizerService textSummarizerService;

    // GeminiService must be mocked — it throws IllegalStateException at startup
    // if GEMINI_API_KEY env var is missing, even in @WebMvcTest slice tests
    @MockitoBean
    private GeminiService geminiService;

    // ─── POST /api/summarizer/summarize — success ─────────────────────────────

    @Test
    @DisplayName("POST /summarize returns 200 with valid request")
    void summarize_validRequest_returns200() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "This is a valid text to summarize for testing purposes.", "concise");

        SummarizeResponse response = buildResponse(1L, "concise", "Short summary.");

        when(textSummarizerService.summarizeText(any())).thenReturn(response);

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.summaryType").value("concise"))
                .andExpect(jsonPath("$.summarizedText").value("Short summary."));
    }

    @Test
    @DisplayName("POST /summarize without summaryType defaults to concise")
    void summarize_noSummaryType_returns200() throws Exception {
        String json = "{\"text\":\"This is a valid text to summarize for testing purposes.\"}";
        SummarizeResponse response = buildResponse(2L, "concise", "Default summary.");
        when(textSummarizerService.summarizeText(any())).thenReturn(response);

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    @DisplayName("POST /summarize returns all expected response fields")
    void summarize_responseContainsAllFields() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "This is a valid text to summarize for testing purposes.", "detailed");

        SummarizeResponse response = buildResponse(5L, "detailed", "Detailed summary text.");
        response.setTokensUsed(250);
        response.setModelUsed("gemini-2.5-flash");

        when(textSummarizerService.summarizeText(any())).thenReturn(response);

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.summarizedText").exists())
                .andExpect(jsonPath("$.summaryType").exists())
                .andExpect(jsonPath("$.tokensUsed").value(250))
                .andExpect(jsonPath("$.modelUsed").value("gemini-2.5-flash"));
    }

    // ─── POST /api/summarizer/summarize — validation failures ─────────────────

    @Test
    @DisplayName("POST /summarize returns 400 when text is blank")
    void summarize_blankText_returns400() throws Exception {
        String json = "{\"text\":\"\",\"summaryType\":\"concise\"}";

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /summarize returns 400 when text is missing")
    void summarize_missingText_returns400() throws Exception {
        String json = "{\"summaryType\":\"concise\"}";

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /summarize returns 400 when text is too short (< 10 chars)")
    void summarize_textTooShort_returns400() throws Exception {
        String json = "{\"text\":\"short\",\"summaryType\":\"concise\"}";

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /summarize returns 400 when text exceeds 50000 characters")
    void summarize_textTooLong_returns400() throws Exception {
        String tooLong = "a".repeat(50001);
        String json = "{\"text\":\"" + tooLong + "\",\"summaryType\":\"concise\"}";

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /summarize returns 400 when service throws IllegalArgumentException")
    void summarize_serviceThrowsIllegal_returns400() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "Valid length text for testing error handling flow.", "invalid_type");

        when(textSummarizerService.summarizeText(any()))
                .thenThrow(new IllegalArgumentException("Invalid summary type: invalid_type"));

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /summarize returns 400 response body has correct structure")
    void summarize_400_responseBodyStructure() throws Exception {
        when(textSummarizerService.summarizeText(any()))
                .thenThrow(new IllegalArgumentException("Text too short"));

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Valid text for error test here\",\"summaryType\":\"concise\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errorCode").exists());
    }

    // ─── POST /api/summarizer/summarize — error codes ─────────────────────────

    @Test
    @DisplayName("POST /summarize returns 429 when rate limit exception thrown by service")
    void summarize_rateLimitException_returns429() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "Valid text for testing rate limit error scenario here.", "concise");

        when(textSummarizerService.summarizeText(any()))
                .thenThrow(new RateLimitException("Rate limit exceeded", 60));

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(60));
    }

    @Test
    @DisplayName("POST /summarize returns 500+ when GeminiApiException is thrown")
    void summarize_geminiApiException_returns5xx() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "Valid text for testing Gemini API failure scenario.", "concise");

        when(textSummarizerService.summarizeText(any()))
                .thenThrow(new GeminiApiException("Gemini unavailable", 503));

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.errorCode").value("GEMINI_API_ERROR"));
    }

    @Test
    @DisplayName("POST /summarize returns 500 on unexpected exception")
    void summarize_unexpectedException_returns500() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "Valid text for testing unexpected exception handling.", "concise");

        when(textSummarizerService.summarizeText(any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }

    // ─── POST — content type ──────────────────────────────────────────────────

    @Test
    @DisplayName("POST /summarize returns 415 when wrong content type sent")
    void summarize_wrongContentType_returns415() throws Exception {
        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("some plain text"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("POST /summarize returns 400 for malformed JSON")
    void summarize_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    // ─── GET /api/summarizer/history — success ────────────────────────────────

    @Test
@   DisplayName("GET /history returns 200 with default pagination")
    void getHistory_defaultPagination_returns200() throws Exception {
        SummarizeResponse r = buildResponse(1L, "concise", "Summary");
        Page<SummarizeResponse> page = new PageImpl<>(List.of(r));
        when(textSummarizerService.getHistory(any())).thenReturn(page);

        mockMvc.perform(get("/api/summarizer/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /history respects page and size parameters")
    void getHistory_customPagination_returns200() throws Exception {
        when(textSummarizerService.getHistory(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/summarizer/history")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /history returns 200 with empty list when no data")
    void getHistory_empty_returns200() throws Exception {
        when(textSummarizerService.getHistory(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/summarizer/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @DisplayName("GET /history returns 500 on service exception")
    void getHistory_serviceException_returns500() throws Exception {
        when(textSummarizerService.getHistory(any()))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/summarizer/history"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }

    // ─── GET /api/summarizer/history/by-type ─────────────────────────────────

    @Test
    @DisplayName("GET /history/by-type returns 200 for valid type")
    void getHistoryByType_validType_returns200() throws Exception {
        Page<SummarizeResponse> page = new PageImpl<>(List.of(
                buildResponse(1L, "concise", "Summary")));
        when(textSummarizerService.getHistoryByType(anyString(), any())).thenReturn(page);

        mockMvc.perform(get("/api/summarizer/history/by-type")
                        .param("summaryType", "concise"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /history/by-type returns 400 for invalid type")
    void getHistoryByType_invalidType_returns400() throws Exception {
        when(textSummarizerService.getHistoryByType(anyString(), any()))
                .thenThrow(new IllegalArgumentException("Invalid summary type: invalid_type"));

        mockMvc.perform(get("/api/summarizer/history/by-type")
                        .param("summaryType", "invalid_type"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SUMMARY_TYPE"));
    }

    @Test
    @DisplayName("GET /history/by-type returns 400 when summaryType param is missing")
    void getHistoryByType_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/api/summarizer/history/by-type"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /history/by-type returns 500 on unexpected service exception")
    void getHistoryByType_serviceException_returns500() throws Exception {
        when(textSummarizerService.getHistoryByType(anyString(), any()))
                .thenThrow(new RuntimeException("DB failure"));

        mockMvc.perform(get("/api/summarizer/history/by-type")
                        .param("summaryType", "concise"))
                .andExpect(status().isInternalServerError());
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private SummarizeResponse buildResponse(Long id, String type, String summary) {
        SummarizeResponse r = new SummarizeResponse();
        r.setId(id);
        r.setOriginalText("Original text");
        r.setSummarizedText(summary);
        r.setSummaryType(type);
        r.setTokensUsed(100);
        r.setModelUsed("gemini-2.5-flash");
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }
}
