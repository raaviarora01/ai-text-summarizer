package com.raavi.ai.ai_text_processor.controller;

import com.raavi.ai.ai_text_processor.aspect.CacheAspect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller slice tests for CacheStatsController.
 * Verifies the /api/cache/stats and /api/cache/info endpoints.
 */
@WebMvcTest(CacheStatsController.class)
@DisplayName("CacheStatsController Tests")
class CacheStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CacheAspect cacheAspect;

    // ─── GET /api/cache/stats ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/cache/stats returns 200 with all fields")
    void getCacheStats_returns200() throws Exception {
        when(cacheAspect.getCacheHits()).thenReturn(42L);
        when(cacheAspect.getCacheMisses()).thenReturn(8L);
        when(cacheAspect.getTotalCacheOperations()).thenReturn(50L);
        when(cacheAspect.getCacheStatistics()).thenReturn("Cache Statistics - Hits: 42 | Misses: 8");

        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheHits").value(42))
                .andExpect(jsonPath("$.cacheMisses").value(8))
                .andExpect(jsonPath("$.totalOperations").value(50))
                .andExpect(jsonPath("$.hitRate").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/cache/stats returns 0% hitRate when total operations is 0")
    void getCacheStats_zeroOperations_returns0HitRate() throws Exception {
        when(cacheAspect.getCacheHits()).thenReturn(0L);
        when(cacheAspect.getCacheMisses()).thenReturn(0L);
        when(cacheAspect.getTotalCacheOperations()).thenReturn(0L);
        when(cacheAspect.getCacheStatistics()).thenReturn("Cache Statistics - Hits: 0 | Misses: 0");

        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hitRate").value("0.00%"));
    }

    @Test
    @DisplayName("GET /api/cache/stats calculates hitRate correctly")
    void getCacheStats_calculatesHitRateCorrectly() throws Exception {
        when(cacheAspect.getCacheHits()).thenReturn(75L);
        when(cacheAspect.getCacheMisses()).thenReturn(25L);
        when(cacheAspect.getTotalCacheOperations()).thenReturn(100L);
        when(cacheAspect.getCacheStatistics()).thenReturn("stats");

        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hitRate").value("75.00%"));
    }

    @Test
    @DisplayName("GET /api/cache/stats returns JSON content type")
    void getCacheStats_returnsJson() throws Exception {
        when(cacheAspect.getCacheHits()).thenReturn(0L);
        when(cacheAspect.getCacheMisses()).thenReturn(0L);
        when(cacheAspect.getTotalCacheOperations()).thenReturn(0L);
        when(cacheAspect.getCacheStatistics()).thenReturn("stats");

        mockMvc.perform(get("/api/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_JSON));
    }

    // ─── GET /api/cache/info ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/cache/info returns 200")
    void getCacheInfo_returns200() throws Exception {
        mockMvc.perform(get("/api/cache/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/cache/info contains cache strategy field")
    void getCacheInfo_containsCacheStrategy() throws Exception {
        mockMvc.perform(get("/api/cache/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheStrategy").exists());
    }

    @Test
    @DisplayName("GET /api/cache/info contains cache names")
    void getCacheInfo_containsCacheNames() throws Exception {
        mockMvc.perform(get("/api/cache/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheNames").exists());
    }

    @Test
    @DisplayName("GET /api/cache/info contains note about production Redis")
    void getCacheInfo_containsProductionNote() throws Exception {
        mockMvc.perform(get("/api/cache/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").exists());
    }
}
