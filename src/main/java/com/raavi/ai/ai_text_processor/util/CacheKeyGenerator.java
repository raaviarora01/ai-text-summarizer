package com.raavi.ai.ai_text_processor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.MessageDigest;

/**
 * Cache Key Generator for Text Summarization
 * 
 * Generates composite cache keys combining text hash and summary type.
 * This ensures different summary types for the same text get cached separately.
 */
public class CacheKeyGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheKeyGenerator.class);
    
    /**
     * Generate a cache key combining text hash and summary type
     * 
     * @param text The original text to be summarized
     * @param summaryType The type of summary (CONCISE, DETAILED, BULLET_POINTS, etc.)
     * @return A composite cache key string
     */
    public static String generateCacheKey(String text, String summaryType) {
        try {
            // Create SHA-256 hash of the text for brevity and consistency
            String textHash = generateHash(text);
            String cacheKey = textHash + ":" + summaryType;
            
            logger.debug("Generated cache key: {}:{} (text length: {} chars)", 
                    textHash.substring(0, 8), summaryType, text.length());
            
            return cacheKey;
            
        } catch (Exception e) {
            logger.warn("Error generating cache key using hash, falling back to text hash code", e);
            // Fallback: use hashCode if SHA-256 fails
            return Math.abs(text.hashCode()) + ":" + summaryType;
        }
    }
    
    /**
     * Generate SHA-256 hash of text
     * @param text The text to hash
     * @return Hex string representation of the hash
     */
    private static String generateHash(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes("UTF-8"));
        
        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
}
