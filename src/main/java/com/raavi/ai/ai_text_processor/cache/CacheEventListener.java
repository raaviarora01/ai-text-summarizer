package com.raavi.ai.ai_text_processor.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache Events Listener
 * 
 * Tracks and logs all cache operations (puts, hits, misses, evicts, clears).
 * Provides real-time visibility into cache performance and effectiveness.
 * 
 * Method calls are triggered from TextSummarizerServiceImpl via manual instrumentation.
 */
@Component
public class CacheEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheEventListener.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    // Track cache statistics
    private final AtomicLong totalCachePuts = new AtomicLong(0);
    private final AtomicLong totalCacheEvicts = new AtomicLong(0);
    private final AtomicLong totalCacheHits = new AtomicLong(0);
    private final AtomicLong totalCacheMisses = new AtomicLong(0);
    private final ConcurrentHashMap<String, Long> cacheSizeMap = new ConcurrentHashMap<>();
    
    /**
     * Handle cache write operation (cache put)
     */
    private void onCacheWrite(String cacheName, Object key, Cache.ValueWrapper value) {
        totalCachePuts.incrementAndGet();
        
        String keyStr = key != null ? key.toString() : "null";
        if (keyStr.length() > 50) {
            keyStr = keyStr.substring(0, 47) + "...";
        }
        
        logger.info(
            "[{}] 📝 CACHE WRITE | Cache: {} | Key: {} | " +
            "Total Puts: {} | Timestamp: {}",
            Thread.currentThread().getName(),
            cacheName,
            keyStr,
            totalCachePuts.get(),
            LocalDateTime.now().format(DATE_FORMATTER)
        );
    }
    
    /**
     * Handle cache hit (value retrieved from cache)
     */
    private void onCacheHit(String cacheName, Object key) {
        totalCacheHits.incrementAndGet();
        long totalOps = totalCacheHits.get() + totalCacheMisses.get();
        double hitRate = (totalOps > 0) ? (totalCacheHits.get() * 100.0 / totalOps) : 0.0;
        
        String keyStr = key != null ? key.toString() : "null";
        if (keyStr.length() > 40) {
            keyStr = keyStr.substring(0, 37) + "...";
        }
        
        logger.info(
            "[{}] ✅ CACHE HIT | Cache: {} | Key: {} | " +
            "Hits: {} | Rate: {:.1f}% | Timestamp: {}",
            Thread.currentThread().getName(),
            cacheName,
            keyStr,
            totalCacheHits.get(),
            hitRate,
            LocalDateTime.now().format(DATE_FORMATTER)
        );
    }
    
    /**
     * Handle cache miss (key not found in cache)
     */
    private void onCacheMiss(String cacheName, Object key) {
        totalCacheMisses.incrementAndGet();
        long totalOps = totalCacheHits.get() + totalCacheMisses.get();
        double hitRate = (totalOps > 0) ? (totalCacheHits.get() * 100.0 / totalOps) : 0.0;
        
        String keyStr = key != null ? key.toString() : "null";
        if (keyStr.length() > 40) {
            keyStr = keyStr.substring(0, 37) + "...";
        }
        
        logger.info(
            "[{}] ❌ CACHE MISS | Cache: {} | Key: {} | " +
            "Misses: {} | Rate: {:.1f}% | Timestamp: {}",
            Thread.currentThread().getName(),
            cacheName,
            keyStr,
            totalCacheMisses.get(),
            (totalOps > 0 ? (totalCacheMisses.get() * 100.0 / totalOps) : 0.0),
            LocalDateTime.now().format(DATE_FORMATTER)
        );
    }
    
    /**
     * Handle cache eviction (key removed)
     */
    private void onCacheEvict(String cacheName, Object key) {
        totalCacheEvicts.incrementAndGet();
        
        String keyStr = key != null ? key.toString() : "null";
        if (keyStr.length() > 50) {
            keyStr = keyStr.substring(0, 47) + "...";
        }
        
        logger.warn(
            "[{}] 🗑️  CACHE EVICT | Cache: {} | Key: {} | " +
            "Total Evicts: {} | Timestamp: {}",
            Thread.currentThread().getName(),
            cacheName,
            keyStr,
            totalCacheEvicts.get(),
            LocalDateTime.now().format(DATE_FORMATTER)
        );
    }
    
    /**
     * Handle cache clear (all entries removed)
     */
    private void onCacheClear(String cacheName) {
        logger.warn(
            "🧹 CACHE CLEAR | Cache: {} | All entries cleared | Timestamp: {}",
            cacheName,
            LocalDateTime.now().format(DATE_FORMATTER)
        );
        
        // Reset size tracking for this cache
        cacheSizeMap.put(cacheName, 0L);
    }
    
    /**
     * Get comprehensive cache statistics
     */
    public CacheStatistics getStatistics() {
        long totalOps = totalCacheHits.get() + totalCacheMisses.get();
        double hitRate = (totalOps > 0) ? (totalCacheHits.get() * 100.0 / totalOps) : 0.0;
        
        return new CacheStatistics(
            totalCachePuts.get(),
            totalCacheHits.get(),
            totalCacheMisses.get(),
            totalCacheEvicts.get(),
            hitRate,
            totalOps
        );
    }
    
    /**
     * Statistics DTO
     */
    public static class CacheStatistics {
        public final long totalPuts;
        public final long totalHits;
        public final long totalMisses;
        public final long totalEvicts;
        public final double hitRate;
        public final long totalOperations;
        
        public CacheStatistics(long puts, long hits, long misses, long evicts, double hitRate, long totalOps) {
            this.totalPuts = puts;
            this.totalHits = hits;
            this.totalMisses = misses;
            this.totalEvicts = evicts;
            this.hitRate = hitRate;
            this.totalOperations = totalOps;
        }
        
        @Override
        public String toString() {
            return String.format(
                "CacheStatistics{puts=%d, hits=%d, misses=%d, evicts=%d, hitRate=%.1f%%, totalOps=%d}",
                totalPuts, totalHits, totalMisses, totalEvicts, hitRate, totalOperations
            );
        }
    }
}
