package com.raavi.ai.ai_text_processor.integration;

import com.raavi.ai.ai_text_processor.dao.TextSummaryRepository;
import com.raavi.ai.ai_text_processor.dto.SummarizeRequest;
import com.raavi.ai.ai_text_processor.service.GeminiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Rate limiting integration tests using TestRestTemplate (real HTTP requests).
 *
 * IMPORTANT: Bucket4j is a servlet filter. MockMvc with @AutoConfigureMockMvc
 * bypasses the servlet filter chain entirely — rate limit headers never appear
 * and limits are never enforced. Real HTTP via TestRestTemplate goes through
 * the full servlet chain including Bucket4j filters.
 *
 * @DirtiesContext resets the Spring context (and Caffeine cache) after each
 * test class, preventing bucket state from leaking between tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ratelimitdb;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.generate-ddl=true",
        "gemini.api.key=test-api-key-not-real-123456789012345",
        "gemini.model=gemini-2.5-flash",
        "gemini.api.url=https://generativelanguage.googleapis.com/v1/models/",
        "bucket4j.enabled=true",
        "bucket4j.cache-to-use=none",
        // Filter 0: POST /summarize — capacity=3 for fast testing
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
        // Filter 1: global /api/** — capacity=10
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
@AutoConfigureTestRestTemplate
@DisplayName("Rate Limiting Integration Tests")
public class RateLimitIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TextSummaryRepository repository;

    @MockitoBean
    private GeminiService geminiService;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        repository.deleteAll();
        when(geminiService.summarizeText(anyString(), anyString()))
                .thenReturn(new GeminiService.GeminiResponse("Rate limit test summary.", 50));
    }

    private HttpEntity<SummarizeRequest> summarizeRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(
                new SummarizeRequest("Valid text for rate limit testing purposes here.", "concise"),
                headers);
    }

    // ─── headers present ─────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("[RATE LIMIT] Response includes X-RateLimit-Limit header")
    void rateLimitHeader_presentOnSuccessfulRequest() {
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/summarizer/summarize",
                HttpMethod.POST, summarizeRequest(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-RateLimit-Limit")).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("[RATE LIMIT] X-RateLimit-Limit header value is 3 on summarize endpoint")
    void rateLimitHeader_correctValue() {
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/summarizer/summarize",
                HttpMethod.POST, summarizeRequest(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("3");
    }

    // ─── rate limit trigger ───────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("[RATE LIMIT] 4th request returns 429 after 3 allowed")
    void summarize_rateLimitTriggered_after3Requests() {
        // First 3 should succeed
        for (int i = 0; i < 3; i++) {
            ResponseEntity<String> r = restTemplate.exchange(
                    baseUrl + "/api/summarizer/summarize",
                    HttpMethod.POST, summarizeRequest(), String.class);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        // 4th should be rate limited
        ResponseEntity<String> blocked = restTemplate.exchange(
                baseUrl + "/api/summarizer/summarize",
                HttpMethod.POST, summarizeRequest(), String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getBody()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    @Order(4)
    @DisplayName("[RATE LIMIT] GET /history is not blocked by summarize rate limit")
    void history_notBlockedBySummarizeRateLimit() {
        // Exhaust /summarize limit
        for (int i = 0; i < 3; i++) {
            restTemplate.exchange(baseUrl + "/api/summarizer/summarize",
                    HttpMethod.POST, summarizeRequest(), String.class);
        }

        // /history should still work
        ResponseEntity<String> r = restTemplate.getForEntity(
                baseUrl + "/api/summarizer/history", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(5)
    @DisplayName("[RATE LIMIT] GET /history has X-RateLimit-Limit of 10 (global filter)")
    void history_hasGlobalRateLimitHeader() {
        ResponseEntity<String> r = restTemplate.getForEntity(
                baseUrl + "/api/summarizer/history", String.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("10");
    }
}
