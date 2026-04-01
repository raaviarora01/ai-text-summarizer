# Response Caching

## Overview
Eliminates duplicate Gemini API calls when the same text is summarized multiple times.

## Cache Keys
- **textSummaries**: SHA256(text) + summaryType
- **summaryHistory**: page-{pageNumber}-size-{pageSize}
- **summaryHistoryByType**: type-{summaryType}-page-{pageNumber}-size-{pageSize}

## How It Works
- **1st Request**: Cache miss → Gemini API called (~1200ms)
- **2nd Request** (same text/type): Cache hit → Instant response (<10ms)

## Files
- **CacheConfig.java** - Cache manager configuration
- **CacheAspect.java** - Tracks hits/misses with logging
- **CacheStatsController.java** - API endpoints for stats
- **TextSummarizerServiceImpl.java** - Added @Cacheable annotations

## API Endpoints

**Check cache statistics:**
```bash
GET http://localhost:8080/api/cache/stats
```

**Get cache info:**
```bash
GET http://localhost:8080/api/cache/info
```

## Testing

```bash
# Request 1 (Cache Miss)
curl -X POST http://localhost:8080/api/summarizer/summarize \
  -H "Content-Type: application/json" \
  -d '{"text": "Your text here", "summaryType": "CONCISE"}'

# Request 2 (Cache Hit - same text/type)
curl -X POST http://localhost:8080/api/summarizer/summarize \
  -H "Content-Type: application/json" \
  -d '{"text": "Your text here", "summaryType": "CONCISE"}'
```

**Watch console logs:**
- ❌ CACHE MISS: API call made
- ✅ CACHE HIT: Served from cache

## Configuration

```properties
# application.properties
spring.cache.type=simple
spring.cache.cache-names=textSummaries,summaryHistory,summaryHistoryByType
logging.level.com.raavi.ai.ai_text_processor.aspect.CacheAspect=INFO
```

## Note
Currently uses in-memory cache (ConcurrentMapCache). For production with multiple instances, switch to Redis.

---

## Logging Configuration

To see detailed cache operations, configure logging in application.properties:

```properties
# For very detailed cache key generation logs
logging.level.com.raavi.ai.ai_text_processor.util.CacheKeyGenerator=DEBUG

# For cache hit/miss information
logging.level.com.raavi.ai.ai_text_processor.aspect.CacheAspect=INFO

# For full application debug
logging.level.com.raavi.ai.ai_text_processor=DEBUG
```

---

## Production Deployment

### Current Setup (Development/Single Instance):
- Uses EBC**ConcurrentMapCacheManager** (in-memory)
- All cache is lost on application restart
- Perfect for single instance deployments

## Performance Metrics

Expected improvements with caching:
- **Response Time**: 90% faster for cache hits (1500ms → 50ms)
- **API Calls**: Reduces by 80-95% with typical usage patterns
- **Cost**: Can reduce API costs by 85-95%
- **Database Load**: Slight increase from saving summaries, but network savings exceed this