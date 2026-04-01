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
 * Logs cache hits, misses, and operations for monitoring and debugging.
 * This provides visibility into cache effectiveness and performance.
 */
@Aspect
@Component
public class CacheAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheAspect.class);
    
    private volatile long cacheHits = 0;
    private volatile long cacheMisses = 0;
    private volatile long totalCacheOperations = 0;
    
    /**
     * Monitor @Cacheable method execution to determine cache hits/misses
     * A cache hit means the method returns immediately without execution (cached result)
     * A cache miss means the method executes and stores result in cache
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object monitorCacheOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // Get the method start time
        long startTime = System.currentTimeMillis();
        totalCacheOperations++;
        
        // Get parameter values for logging
        String textParam = args.length > 0 ? args[0].toString() : "N/A";
        String summaryTypeParam = args.length > 1 ? args[1].toString() : "N/A";
        
        try {
            Object result = joinPoint.proceed();
            
            // If we got a result, it could be from cache or freshly computed
            // We infer cache HIT if execution time is very fast (< 100ms)
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime < 100) {
                // Likely a cache hit (fast retrieval)
                cacheHits++;
                logger.info(
                    "✅ CACHE HIT | Method: {} | SummaryType: {} | " +
                    "ExecutionTime: {}ms | TotalHits: {} | HitRate: {:.2f}%",
                    methodName, summaryTypeParam, executionTime, cacheHits,
                    calculateHitRate()
                );
            } else {
                // Cache miss - method executed (likely called Gemini API)
                cacheMisses++;
                logger.info(
                    "❌ CACHE MISS | Method: {} | SummaryType: {} | " +
                    "ExecutionTime: {}ms | TotalMisses: {} | HitRate: {:.2f}%",
                    methodName, summaryTypeParam, executionTime, cacheMisses,
                    calculateHitRate()
                );
            }
            
            return result;
            
        } catch (Throwable e) {
            logger.error("Error in cache operation for method: {}", methodName, e);
            throw e;
        }
    }
    
    /**
     * Calculate cache hit rate percentage
     */
    private double calculateHitRate() {
        if (totalCacheOperations == 0) return 0.0;
        return (cacheHits * 100.0) / totalCacheOperations;
    }
    
    /**
     * Get cache statistics
     */
    public String getCacheStatistics() {
        return String.format(
            "Cache Statistics - Hits: %d | Misses: %d | Total: %d | HitRate: %.2f%%",
            cacheHits, cacheMisses, totalCacheOperations, calculateHitRate()
        );
    }
    
    public long getCacheHits() {
        return cacheHits;
    }
    
    public long getCacheMisses() {
        return cacheMisses;
    }
    
    public long getTotalCacheOperations() {
        return totalCacheOperations;
    }
}
