package com.raavi.ai.ai_text_processor.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raavi.ai.ai_text_processor.dao.TextSummaryRepository;
import com.raavi.ai.ai_text_processor.dto.SummarizeRequest;
import com.raavi.ai.ai_text_processor.entity.TextSummary;
import com.raavi.ai.ai_text_processor.enums.SummaryType;
import com.raavi.ai.ai_text_processor.service.GeminiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full Spring integration tests.
 * Loads the complete application context with H2 in-memory database.
 * GeminiService is mocked to avoid real API calls.
 *
 * Covers:
 * - End-to-end API flow (request → service → DB → response)
 * - Caching behavior (second request served from cache, Gemini not called)
 * - Actuator endpoints
 * - Swagger/OpenAPI endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:integrationtest;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "gemini.api.key=test-api-key-not-real-123456789012345",
        "gemini.model=gemini-2.5-flash",
        "gemini.api.url=https://generativelanguage.googleapis.com/v1/models/",
        "bucket4j.enabled=false",
        "management.endpoints.web.exposure.include=health,info,metrics,caches",
        "management.endpoint.health.show-details=always"
})
@DisplayName("Integration Tests — Full Spring Context")
public class FullIntegrationTest {

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
        // Clear all caches between tests
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });

        when(geminiService.summarizeText(anyString(), anyString()))
                .thenReturn(new GeminiService.GeminiResponse("Integration test summary.", 150));
    }

    // ─── End-to-end: summarize flow ───────────────────────────────────────────

    @Test
    @DisplayName("[E2E] POST /summarize creates summary and returns 200")
    void summarize_e2e_creates_and_returns200() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "Artificial intelligence is transforming every industry worldwide today.", "concise");

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.summarizedText").value("Integration test summary."))
                .andExpect(jsonPath("$.summaryType").value("concise"))
                .andExpect(jsonPath("$.modelUsed").value("gemini-2.5-flash"));
    }

    @Test
    @DisplayName("[E2E] POST /summarize persists entity to database")
    void summarize_e2e_persistsToDatabase() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "This text should be persisted to the H2 database during testing.", "detailed");

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(repository.count()).isEqualTo(1);
        TextSummary saved = repository.findAll().get(0);
        assertThat(saved.getSummarizedText()).isEqualTo("Integration test summary.");
        assertThat(saved.getSummaryType()).isEqualTo(SummaryType.DETAILED);
    }

    @Test
    @DisplayName("[E2E] GET /history returns persisted summaries")
    void getHistory_e2e_returnsSavedData() throws Exception {
        // Persist data directly
        seedDatabase(3);

        mockMvc.perform(get("/api/summarizer/history")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("[E2E] GET /history/by-type filters correctly")
    void getHistoryByType_e2e_filtersCorrectly() throws Exception {
        // Seed 2 CONCISE and 1 DETAILED
        seedEntity("Text one.", SummaryType.CONCISE);
        seedEntity("Text two.", SummaryType.CONCISE);
        seedEntity("Text three.", SummaryType.DETAILED);

        mockMvc.perform(get("/api/summarizer/history/by-type")
                        .param("summaryType", "concise"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/summarizer/history/by-type")
                        .param("summaryType", "detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ─── Caching behavior ─────────────────────────────────────────────────────

    @Test
    @DisplayName("[CACHE] Second identical request is served from cache — Gemini called only once")
    void summarize_caching_geminiCalledOnce() throws Exception {
        SummarizeRequest request = new SummarizeRequest(
                "Caching test text that is identical on both requests here.", "concise");

        String body = objectMapper.writeValueAsString(request);

        // First call — cache miss
        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Second call — should be cache hit
        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Gemini should only have been called ONCE despite two requests
        verify(geminiService, times(1)).summarizeText(anyString(), anyString());
    }

    @Test
    @DisplayName("[CACHE] Different text produces two Gemini calls")
    void summarize_differentTexts_twoCalls() throws Exception {
        SummarizeRequest req1 = new SummarizeRequest(
                "First unique text for caching differentiation test one.", "concise");
        SummarizeRequest req2 = new SummarizeRequest(
                "Second unique text for caching differentiation test two.", "concise");

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk());

        verify(geminiService, times(2)).summarizeText(anyString(), anyString());
    }

    @Test
    @DisplayName("[CACHE] Same text but different summaryType produces two Gemini calls")
    void summarize_sameTextDifferentType_twoCalls() throws Exception {
        String text = "Same text but requesting different summary type for cache key test.";

        SummarizeRequest req1 = new SummarizeRequest(text, "concise");
        SummarizeRequest req2 = new SummarizeRequest(text, "detailed");

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        // Need to save a separate entity for detailed type
        TextSummary detailedEntity = seedEntity(text, SummaryType.DETAILED);
        when(geminiService.summarizeText(anyString(), anyString()))
                .thenReturn(new GeminiService.GeminiResponse("Detailed summary.", 200));

        mockMvc.perform(post("/api/summarizer/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk());

        // Two distinct cache keys = two Gemini calls
        verify(geminiService, times(2)).summarizeText(anyString(), anyString());
    }

    @Test
    @DisplayName("[CACHE] Cache stats endpoint reflects operations")
    void cacheStats_reflectsOperations() throws Exception {
        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheHits").isNumber())
                .andExpect(jsonPath("$.cacheMisses").isNumber())
                .andExpect(jsonPath("$.totalOperations").isNumber())
                .andExpect(jsonPath("$.hitRate").exists());
    }

    // ─── Actuator endpoints ───────────────────────────────────────────────────

    @Test
    @DisplayName("[ACTUATOR] GET /actuator/health returns UP status")
    void actuator_health_returnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("[ACTUATOR] GET /actuator/health shows components")
    void actuator_health_showsComponents() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").exists());
    }

    @Test
    @DisplayName("[ACTUATOR] GET /actuator/info returns 200")
    void actuator_info_returns200() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[ACTUATOR] GET /actuator/metrics returns available metrics")
    void actuator_metrics_returns200() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    @DisplayName("[ACTUATOR] GET /actuator/metrics/jvm.memory.used returns metric data")
    void actuator_jvmMemory_returns200() throws Exception {
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("jvm.memory.used"))
                .andExpect(jsonPath("$.measurements").isArray());
    }

    @Test
    @DisplayName("[ACTUATOR] GET /actuator/caches returns cache names")
    void actuator_caches_returns200() throws Exception {
        mockMvc.perform(get("/actuator/caches"))
                .andExpect(status().isOk());
    }

    // ─── Swagger / OpenAPI endpoints ──────────────────────────────────────────

    @Test
    @DisplayName("[SWAGGER] GET /v3/api-docs returns OpenAPI spec")
    void swagger_apiDocs_returns200() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("AI Text Processor API"));
    }

    @Test
    @DisplayName("[SWAGGER] GET /v3/api-docs contains all API paths")
    void swagger_apiDocs_containsAllPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/summarizer/summarize']").exists())
                .andExpect(jsonPath("$.paths['/api/summarizer/history']").exists())
                .andExpect(jsonPath("$.paths['/api/summarizer/history/by-type']").exists());
    }

    @Test
    @DisplayName("[SWAGGER] GET /swagger-ui.html redirects to Swagger UI")
    void swagger_ui_redirects() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("[SWAGGER] GET /v3/api-docs contains schema definitions")
    void swagger_apiDocs_containsSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas").exists());
    }

    // ─── helper methods ───────────────────────────────────────────────────────

    private void seedDatabase(int count) {
        for (int i = 0; i < count; i++) {
            seedEntity("Seeded text number " + i + " for integration testing.", SummaryType.CONCISE);
        }
    }

    private TextSummary seedEntity(String text, SummaryType type) {
        TextSummary entity = new TextSummary();
        entity.setOriginalText(text);
        entity.setSummarizedText("Summary of: " + text);
        entity.setSummaryType(type);
        entity.setTokensUsed(100);
        entity.setModelUsed("gemini-2.5-flash");
        return repository.save(entity);
    }
}
