package com.raavi.ai.ai_text_processor.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration for Response Caching
 *
 * Uses Caffeine as the in-memory cache provider.
 * Caffeine is required by the Bucket4j rate-limiting starter, and is a
 * drop-in upgrade over ConcurrentMapCacheManager — all existing @Cacheable
 * annotations (textSummaries, summaryHistory, summaryHistoryByType) continue
 * to work unchanged.
 *
 * Cache Names:
 * - textSummaries          : Summarized text responses (text hash + summaryType key)
 * - summaryHistory         : Paginated history queries (page + size key)
 * - summaryHistoryByType   : Paginated history by type (type + page + size key)
 * - rate-limit-summarize   : Bucket4j per-IP buckets for /summarize endpoint
 * - rate-limit-global      : Bucket4j per-IP buckets for all /api/** routes
 *
 * For distributed/production environments, switch to Redis CacheManager.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public CacheManager cacheManager() {
        logger.info("========================================");
        logger.info("Initializing CacheManager with caches:");
        logger.info("  ✓ textSummaries          (POST API caching)");
        logger.info("  ✓ summaryHistory          (GET history pagination caching)");
        logger.info("  ✓ summaryHistoryByType    (GET history by type caching)");
        logger.info("  ✓ rate-limit-summarize    (Bucket4j – /summarize rate limit)");
        logger.info("  ✓ rate-limit-global       (Bucket4j – global /api/** rate limit)");
        logger.info("========================================");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "textSummaries",
                "summaryHistory",
                "summaryHistoryByType",
                "rate-limit-summarize",
                "rate-limit-global"
        );

        // Default Caffeine spec for application caches:
        // - max 10,000 entries (prevents unbounded memory growth)
        // - entries expire 1 hour after last access
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterAccess(1, TimeUnit.HOURS)
        );

        logger.info("✓ Cache configuration complete - Using Caffeine in-memory cache");
        logger.info("⚠️  Note: For distributed/production environments, configure Redis cache instead");

        return cacheManager;
    }
}