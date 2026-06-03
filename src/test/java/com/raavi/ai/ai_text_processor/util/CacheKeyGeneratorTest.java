package com.raavi.ai.ai_text_processor.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CacheKeyGenerator.
 * Verifies SHA-256 hashing, key format, consistency, and collision resistance.
 */
@DisplayName("CacheKeyGenerator Tests")
public class CacheKeyGeneratorTest {

    @Test
    @DisplayName("generateCacheKey returns non-null key")
    void generateCacheKey_notNull() {
        String key = CacheKeyGenerator.generateCacheKey("some text here", "concise");
        assertThat(key).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("generateCacheKey key contains a colon separator")
    void generateCacheKey_containsColon() {
        String key = CacheKeyGenerator.generateCacheKey("some text here", "concise");
        assertThat(key).contains(":");
    }

    @Test
    @DisplayName("generateCacheKey key ends with summary type")
    void generateCacheKey_endsWithSummaryType() {
        String key = CacheKeyGenerator.generateCacheKey("some text here", "detailed");
        assertThat(key).endsWith(":detailed");
    }

    // ─── consistency ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("same text and type always produce the same key (idempotent)")
    void generateCacheKey_idempotent() {
        String text = "Artificial intelligence is transforming the world.";
        String key1 = CacheKeyGenerator.generateCacheKey(text, "concise");
        String key2 = CacheKeyGenerator.generateCacheKey(text, "concise");
        assertThat(key1).isEqualTo(key2);
    }

    // ─── different inputs produce different keys ───────────────────────────────

    @Test
    @DisplayName("different texts produce different keys")
    void generateCacheKey_differentTexts_differentKeys() {
        String key1 = CacheKeyGenerator.generateCacheKey("text one here", "concise");
        String key2 = CacheKeyGenerator.generateCacheKey("text two here", "concise");
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("same text with different summary types produce different keys")
    void generateCacheKey_differentTypes_differentKeys() {
        String text = "Same text for testing cache key generation behavior.";
        String key1 = CacheKeyGenerator.generateCacheKey(text, "concise");
        String key2 = CacheKeyGenerator.generateCacheKey(text, "detailed");
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("all four summary types produce distinct keys for same text")
    void generateCacheKey_allTypes_distinct() {
        String text = "Testing all four summary type cache keys with same text body.";
        String k1 = CacheKeyGenerator.generateCacheKey(text, "concise");
        String k2 = CacheKeyGenerator.generateCacheKey(text, "detailed");
        String k3 = CacheKeyGenerator.generateCacheKey(text, "bullet_points");
        String k4 = CacheKeyGenerator.generateCacheKey(text, "executive");

        assertThat(java.util.Set.of(k1, k2, k3, k4)).hasSize(4);
    }

    // ─── SHA-256 hash length ──────────────────────────────────────────────────

    @Test
    @DisplayName("hash portion is 64 hex characters (SHA-256 = 256 bits = 64 hex chars)")
    void generateCacheKey_hashLength() {
        String key = CacheKeyGenerator.generateCacheKey("test text for hashing", "concise");
        String hashPart = key.split(":")[0];
        assertThat(hashPart).hasSize(64);
    }

    @Test
    @DisplayName("hash portion contains only hex characters")
    void generateCacheKey_hashIsHex() {
        String key = CacheKeyGenerator.generateCacheKey("test text for hashing", "concise");
        String hashPart = key.split(":")[0];
        assertThat(hashPart).matches("[0-9a-f]{64}");
    }

    // ─── edge cases ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("minimum length text (10 chars) produces valid key")
    void generateCacheKey_minLengthText() {
        String key = CacheKeyGenerator.generateCacheKey("1234567890", "concise");
        assertThat(key).isNotBlank().contains(":concise");
    }

    @Test
    @DisplayName("long text (50000 chars) produces valid key without error")
    void generateCacheKey_longText() {
        String longText = "a".repeat(50000);
        String key = CacheKeyGenerator.generateCacheKey(longText, "detailed");
        assertThat(key).isNotBlank().endsWith(":detailed");
    }

    @Test
    @DisplayName("text with special characters produces valid key")
    void generateCacheKey_specialCharacters() {
        String text = "Text with special chars: @#$%^&*() and unicode: \u00e9\u00e0\u00fc";
        assertThatCode(() -> CacheKeyGenerator.generateCacheKey(text, "concise"))
                .doesNotThrowAnyException();
    }
}
