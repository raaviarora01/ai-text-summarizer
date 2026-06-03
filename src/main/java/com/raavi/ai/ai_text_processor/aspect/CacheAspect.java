package com.raavi.ai.ai_text_processor.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cache Operations Aspect
 *
 * Tracks cache hits and misses for @Cacheable methods.
 *
 * WHY THE OLD APPROACH WAS WRONG:
 * Spring's @Cacheable intercepts method calls BEFORE this @Around advice runs.
 * On a cache HIT, Spring returns the cached value immediately and the method
 * body never executes — and critically, the @Around advice is ALSO skipped.
 * This meant the aspect only ever ran on cache misses, so hits were never counted.
 *
 * THE FIX:
 * - Cache MISSES are counted here (this advice only runs when the method executes)
 * - Cache HITS are counted via recordCacheHit(), called explicitly from
 *   TextSummarizerServiceImpl after detecting the @Cacheable method was skipped
 */
@Aspect
@Component
public class CacheAspect {

    private static final Logger logger = LoggerFactory.getLogger(CacheAspect.class);

    private volatile long cacheHits = 0;
    private volatile long cacheMisses = 0;

    /**
     * Intercepts @Cacheable methods. Only runs on cache misses —
     * Spring bypasses this advice entirely when returning a cached result.
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object monitorCacheOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - startTime;
        cacheMisses++;

        logger.info("❌ CACHE MISS | Method: {} | ExecutionTime: {}ms | TotalMisses: {}",
                methodName, executionTime, cacheMisses);

        return result;
    }

    /**
     * Called explicitly from the service when a cache hit is detected.
     * Since @Around is bypassed on cache hits, the service must call this directly.
     */
    public void recordCacheHit(String methodName) {
        cacheHits++;
        logger.info("✅ CACHE HIT  | Method: {} | TotalHits: {} | HitRate: {:.2f}%",
                methodName, cacheHits, calculateHitRate());
    }

    private double calculateHitRate() {
        long total = cacheHits + cacheMisses;
        if (total == 0) return 0.0;
        return (cacheHits * 100.0) / total;
    }

    public String getCacheStatistics() {
        long total = cacheHits + cacheMisses;
        return String.format(
                "Cache Statistics - Hits: %d | Misses: %d | Total: %d | HitRate: %.2f%%",
                cacheHits, cacheMisses, total, calculateHitRate());
    }

    public long getCacheHits()            { return cacheHits; }
    public long getCacheMisses()          { return cacheMisses; }
    public long getTotalCacheOperations() { return cacheHits + cacheMisses; }
}
