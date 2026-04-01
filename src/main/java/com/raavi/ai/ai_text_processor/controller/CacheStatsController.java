package com.raavi.ai.ai_text_processor.controller;

import com.raavi.ai.ai_text_processor.aspect.CacheAspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache Statistics Controller
 * 
 * Provides endpoints to monitor cache performance and effectiveness.
 * These endpoints help understand how well the caching strategy is working.
 */
@RestController
@RequestMapping("/api/cache")
@CrossOrigin("*")
public class CacheStatsController {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheStatsController.class);
    
    @Autowired
    private CacheAspect cacheAspect;
    
    /**
     * Get current cache statistics
     * 
     * Returns:
     * - cacheHits: Number of times a cached result was returned
     * - cacheMisses: Number of times cache was empty and API was called
     * - totalOperations: Total cache lookup operations
     * - hitRate: Percentage of cache hits (beneficial for cost)
     * 
     * @return Cache performance metrics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long hits = cacheAspect.getCacheHits();
        long misses = cacheAspect.getCacheMisses();
        long total = cacheAspect.getTotalCacheOperations();
        
        double hitRate = total == 0 ? 0.0 : (hits * 100.0) / total;
        
        stats.put("cacheHits", hits);
        stats.put("cacheMisses", misses);
        stats.put("totalOperations", total);
        stats.put("hitRate", String.format("%.2f%%", hitRate));
        stats.put("message", "Cache Statistics - " + cacheAspect.getCacheStatistics());
        
        logger.info("Cache stats requested: {}", stats);
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get human-readable cache information
     * 
     * @return Formatted cache performance information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> getCacheInfo() {
        Map<String, String> info = new HashMap<>();
        
        info.put("cacheStrategy", "Response Caching for Text Summarization");
        info.put("cacheImplementation", "Spring Cache with ConcurrentMapCacheManager (In-Memory)");
        info.put("cacheNames", "textSummaries, summaryHistory, summaryHistoryByType");
        info.put("", "");
        info.put("Cache 1: textSummaries", "POST /api/summarizer/summarize - Caches API responses");
        info.put("  Key Format", "SHA256(text) + ':' + summaryType");
        info.put("  Benefit", "Eliminates duplicate Gemini API calls, reduces cost");
        info.put("", "");
        info.put("Cache 2: summaryHistory", "GET /api/summarizer/history - Caches paginated results");
        info.put("  Key Format", "page-{pageNumber}-size-{pageSize}");
        info.put("  Benefit", "Eliminates repeated database queries for same pagination");
        info.put("", "");
        info.put("Cache 3: summaryHistoryByType", "GET /api/summarizer/history/{type} - Caches filtered results");
        info.put("  Key Format", "type-{summaryType}-page-{pageNumber}-size-{pageSize}");
        info.put("  Benefit", "Eliminates repeated filtered database queries");
        info.put("", "");
        info.put("note", "For production distributed systems, configure Redis cache instead");
        
        logger.info("Cache info requested");
        
        return ResponseEntity.ok(info);
    }
}
