package com.raavi.ai.ai_text_processor.config;

import com.giffing.bucket4j.spring.boot.starter.config.cache.SyncCacheResolver;
import com.giffing.bucket4j.spring.boot.starter.config.cache.jcache.JCacheCacheResolver;
import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.time.Duration;
import java.util.OptionalLong;

/**
 * Provides Bucket4j with a JCacheCacheResolver backed by Caffeine's JCache provider.
 *
 * We use the JCache (JSR-107) path because JCacheCacheResolver correctly implements
 * SyncCacheResolver returning ProxyManagerWrapper — the type the 0.14.x starter expects.
 *
 * bucket4j.cache-to-use=none in application.properties disables auto-detection so
 * this bean is the sole resolver Bucket4j uses.
 *
 * Application caches (textSummaries, summaryHistory, summaryHistoryByType) remain
 * managed by CacheConfig.java (CaffeineCacheManager) and are completely unaffected.
 */
@Configuration
public class Bucket4jCacheResolver {

    private static final Logger logger = LoggerFactory.getLogger(Bucket4jCacheResolver.class);

    @Bean
    @Primary
    public SyncCacheResolver caffeineSyncCacheResolver() {
        CachingProvider cachingProvider = Caching.getCachingProvider(
                "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider");
        CacheManager jCacheManager = cachingProvider.getCacheManager();

        for (String cacheName : new String[]{"rate-limit-summarize", "rate-limit-global"}) {
            if (jCacheManager.getCache(cacheName) == null) {
                CaffeineConfiguration<Object, Object> config = new CaffeineConfiguration<>();
                config.setMaximumSize(OptionalLong.of(100_000));
                config.setExpireAfterWrite(OptionalLong.of(Duration.ofHours(1).toNanos()));
                jCacheManager.createCache(cacheName, config);
                logger.info("Created Bucket4j JCache: {}", cacheName);
            }
        }

        return new JCacheCacheResolver(jCacheManager);
    }
}