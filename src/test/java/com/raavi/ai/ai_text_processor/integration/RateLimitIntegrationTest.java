package com.raavi.ai.ai_text_processor.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raavi.ai.ai_text_processor.dao.TextSummaryRepository;
import com.raavi.ai.ai_text_processor.dto.SummarizeRequest;
import com.raavi.ai.ai_text_processor.service.GeminiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Rate limiting integration tests.
 * Bucket4j is enabled with a very low limit (3 req / 60s)
 * so tests can trigger the 429 quickly without sending 10+ real requests.
 *
 * These tests are isolated to their own Spring context via @TestPropertySource
 * so they don't affect other test classes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ratelimitdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "gemini.api.key=test-api-key-not-real-123456789012345",
        "gemini.model=gemini-2.5-flash",
        "gemini.api.url=https://generativelanguage.googleapis.com/v1/models/",
        // Enable Bucket4j with tiny limit for testing
        "bucket4j.enabled=true",
        "bucket4j.cache-to-use=none",
        // Filter 0: POST /summarize — 3 req / 60s
        "bucket4j.filters[0].cache-name=rate-limit-summarize",
        "bucket4j.filters[0].url=/api/summarizer/summarize",
        "bucket4j.filters[0].http-response-body={\"status\":429,\"errorCode\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Rate limit exceeded.\"}",
        "bucket4j.filters[0].http-content-type=application/json",
        "bucket4j.filters[0].http-status-code=TOO_MANY_REQUESTS",
        "bucket4j.filters[0].http-response-headers[X-RateLimit-Limit]=3",
        "bucket4j.filters[0].http-response-headers[X-RateLimit-Policy]=3 requests per 60 seconds",
        "bucket4j.filters[0].rate-limits[0].cache-key=getRemoteAddr()",
        "bucket4j.filters[0].rate-limits[0].bandwidths[0].capacity=3",
        "bucket4j.filters[0].rate-limits[0].bandwidths[0].time=60",
        "bucket4j.filters[0].rate-limits[0].bandwidths[0].unit=seconds",
        "bucket4j.filters[0].rate-limits[0].bandwidths[0].refill-speed=greedy",
        // Filter 1: global /api/** — 10 req / 60s
        "bucket4j.filters[1].cache-name=rate-limit-global",
        "bucket4j.filters[1].url=^/api/.*",
        "bucket4j.filters[1].http-response-body={\"status\":429,\"errorCode\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Global rate limit exceeded.\"}",
        "bucket4j.filters[1].http-content-type=application/json",
        "bucket4j.filters[1].http-status-code=TOO_MANY_REQUESTS",
        "bucket4j.filters[1].http-response-headers[X-RateLimit-Limit]=10",
        "bucket4j.filters[1].rate-limits[0].cache-key=getRemoteAddr()",
        "bucket4j.filters[1].rate-limits[0].bandwidths[0].capacity=10",
        "bucket4j.filters[1].rate-limits[0].bandwidths[0].time=60",
        "bucket4j.filters[1].rate-limits[0].bandwidths[0].unit=seconds",
        "bucket4j.filters[1].rate-limits[0].bandwidths[0].refill-speed=greedy"
})
@DisplayName("Rate Limiting Integration Tests")
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TextSummaryRepository repository;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });

        when(geminiService.summarizeText(anyString(), anyString()))
                .thenReturn(new GeminiService.GeminiResponse("Rate limit test summary.", 50));
    }

    // ─── rate limit headers present ───────────────────────────────────────────

    @Test
    @DisplayName("[RATE LIMIT] Response includes X-RateLimit-Limit header")
    void rateLimitHeader_presentOnSuccessfulRequest() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "Valid text for rate limit header test.", "concise");

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"));
    }

    @Test
    @DisplayName("[RATE LIMIT] X-RateLimit-Limit header value is 3 on summarize endpoint")
    void rateLimitHeader_correctValue() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "Valid text to check rate limit header value.", "concise");

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "3"));
    }

    // ─── rate limit trigger ───────────────────────────────────────────────────

    @Test
    @DisplayName("[RATE LIMIT] 4th request to /summarize returns 429 after 3 allowed")
    void summarize_rateLimitTriggered_after3Requests() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "Text to trigger rate limit after three allowed requests.", "concise");
        String body = objectMapper.writeValueAsString(request);

        List<Integer> statusCodes = new ArrayList<>();

        // Send 4 requests
        for (int i = 0; i < 4; i++) {
            MvcResult result = mockMvc.perform(post("/api/summarizer/summarize")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn();
            statusCodes.add(result.getResponse().getStatus());
        }

        // First 3 should succeed
        assertThat(statusCodes.subList(0, 3))
                .allSatisfy(s -> assertThat(s).isEqualTo(200));

        // 4th should be rate limited
        assertThat(statusCodes.get(3)).isEqualTo(429);
    }

    @Test
    @DisplayName("[RATE LIMIT] 429 response body has correct errorCode")
    void rateLimitResponse_hasCorrectBody() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "Text used to exhaust rate limit and check 429 response body.", "concise");
        String body = objectMapper.writeValueAsString(request);

        // Exhaust the limit
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/summarizer/summarize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andReturn();
        }

        // 4th request — check 429 response
        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("[RATE LIMIT] GET /history is NOT blocked by summarize rate limit")
    void history_notBlockedBySummarizeRateLimit() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "Text to exhaust summarize limit then test history.", "concise");
        String body = objectMapper.writeValueAsString(request);

        // Exhaust /summarize rate limit
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/summarizer/summarize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andReturn();
        }

        // /history should still work (governed by global filter only)
        mockMvc.perform(get("/api/summarizer/history"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[RATE LIMIT] GET /history has X-RateLimit-Limit header of 10 (global filter)")
    void history_hasGlobalRateLimitHeader() throws Exception {
        mockMvc.perform(get("/api/summarizer/history"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "10"));
    }
}
