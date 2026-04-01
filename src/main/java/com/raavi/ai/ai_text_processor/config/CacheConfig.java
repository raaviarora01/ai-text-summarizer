package com.raavi.ai.ai_text_processor.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache Configuration for Response Caching
 * 
 * This configuration sets up in-memory caching for text summarization results.
 * Production can be extended to use Redis instead of ConcurrentMapCacheManager.
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);
    
    /**
     * Configure the Cache Manager for text summaries
     * Using ConcurrentMapCacheManager for in-memory caching (suitable for single instance)
     * For distributed systems, consider switching to Redis CacheManager
     * 
     * Cache Names:
     * - textSummaries: Stores summarized text responses with composite key (text hash + summaryType)
     * - summaryHistory: Stores paginated history queries with key (page + size)
     * - summaryHistoryByType: Stores paginated history by type queries with key (type + page + size)
     */
    @Bean
    public CacheManager cacheManager() {
        logger.info("========================================");
        logger.info("Initializing CacheManager with caches:");
        logger.info("  ✓ textSummaries (POST API caching)");
        logger.info("  ✓ summaryHistory (GET history pagination caching)");
        logger.info("  ✓ summaryHistoryByType (GET history by type caching)");
        logger.info("========================================");
        
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
            "textSummaries",
            "summaryHistory",
            "summaryHistoryByType"
        );
        
        logger.info("✓ Cache configuration complete - Using in-memory cache (ConcurrentMapCacheManager)");
        logger.info("⚠️  Note: For distributed/production environments, configure Redis cache instead");
        
        return cacheManager;
    }
}
