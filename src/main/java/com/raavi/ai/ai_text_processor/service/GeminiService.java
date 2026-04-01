package com.raavi.ai.ai_text_processor.service;

import com.raavi.ai.ai_text_processor.exception.GeminiApiException;
import com.raavi.ai.ai_text_processor.exception.RateLimitException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model}")
    private String geminiModel;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Validates that API key is properly loaded from environment
     * This runs after all properties are injected
     */
    @PostConstruct
    private void validateApiKey() {
        logger.info("========== GEMINI SERVICE INITIALIZATION ==========");
        
        // Try to load from environment variable directly
        String envApiKey = System.getenv("GEMINI_API_KEY");
        logger.info("GEMINI_API_KEY environment variable present: {}", envApiKey != null);
        
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            logger.error("❌ CRITICAL: Gemini API key is NOT SET!");
            logger.error("   Please set the environment variable: GEMINI_API_KEY");
            logger.error("   Current value from properties: {}", geminiApiKey);
            throw new IllegalStateException("Gemini API key is not configured. Set GEMINI_API_KEY environment variable.");
        }
        
        // Log masked key for debugging (show first 10 and last 5 chars only)
        String maskedKey = maskApiKey(geminiApiKey);
        logger.info("✓ API Key loaded successfully: {}", maskedKey);
        logger.info("✓ Model configured: {}", geminiModel);
        logger.info("✓ API URL configured: {}", geminiApiUrl);
        logger.info("===================================================");
    }
    
    /**
     * Masks API key for secure logging
     */
    private String maskApiKey(String key) {
        if (key == null || key.length() < 15) {
            return "***INVALID***";
        }
        return key.substring(0, 10) + "..." + key.substring(key.length() - 5);
    }

    /**
     * Calls Gemini API to generate a summary
     * @param text The text to summarize
     * @param prompt The summary type prompt
     * @return A wrapper object containing summary and token usage
     * @throws GeminiApiException if API call fails
     * @throws RateLimitException if rate limit is exceeded
     */
    public GeminiResponse summarizeText(String text, String prompt) {
        try {
            logger.debug("=== GEMINI API CALL START ===");
            logger.debug("Text length: {} characters", text.length());
            logger.debug("Prompt type: {}", prompt.substring(0, Math.min(50, prompt.length())));
            
            String fullPrompt = prompt + "\n\nText to summarize:\n" + text;
            String requestBody = buildRequestBody(fullPrompt);
            
            String url = geminiApiUrl + geminiModel + ":generateContent?key=" + geminiApiKey;
            logger.debug("API Endpoint: {}", geminiApiUrl + geminiModel + ":generateContent");
            logger.debug("API Key being used: {}", maskApiKey(geminiApiKey));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            logger.debug("Headers set. Content-Type: application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            logger.info("Calling Gemini API...");
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            logger.info("✓ Gemini API call successful. Status: {}", response.getStatusCode());
            logger.debug("Response received. Body length: {} characters", response.getBody().length());
            
            GeminiResponse result = parseGeminiResponse(response.getBody());
            logger.info("✓ Response parsed successfully. Summary generated.");
            logger.debug("=== GEMINI API CALL END (SUCCESS) ===");
            
            return result;
            
        } catch (HttpClientErrorException.TooManyRequests e) {
            logger.error("❌ Rate limit exceeded from Gemini API", e);
            throw new RateLimitException("Gemini API rate limit exceeded. Please try again later.", 60);
        } catch (HttpClientErrorException e) {
            logger.error("❌ Gemini API error - Status: {}, Message: {}", e.getStatusCode(), e.getMessage());
            
            // Check for API key issue
            if (e.getStatusCode().value() == 400 && e.getMessage().contains("API key")) {
                logger.error("   ⚠️  API KEY ISSUE DETECTED!");
                logger.error("   Please verify your GEMINI_API_KEY environment variable is correct and valid.");
                logger.error("   Currently using: {}", maskApiKey(geminiApiKey));
            }
            
            throw new GeminiApiException("Gemini API error: " + e.getMessage(), e.getStatusCode().value(), e);
        } catch (Exception e) {
            logger.error("❌ Unexpected error calling Gemini API: {}", e.getMessage(), e);
            throw new GeminiApiException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the request body for Gemini API
     */
    private String buildRequestBody(String prompt) {
        String json = "{\n" +
                "  \"contents\": [\n" +
                "    {\n" +
                "      \"parts\": [\n" +
                "        {\n" +
                "          \"text\": \"" + escapeJson(prompt) + "\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"generationConfig\": {\n" +
                "    \"temperature\": 0.7,\n" +
                "    \"topP\": 0.95,\n" +
                "    \"topK\": 64,\n" +
                "    \"maxOutputTokens\": 2048\n" +
                "  }\n" +
                "}";
        return json;
    }

    /**
     * Parses the Gemini API response
     */
    private GeminiResponse parseGeminiResponse(String responseBody) {
        try {
            logger.debug("Parsing Gemini API response");
            
            JsonNode root = objectMapper.readTree(responseBody);
            
            String summary = root
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
            
            if (summary == null || summary.isEmpty()) {
                throw new GeminiApiException("Empty response from Gemini API");
            }
            
            // Extract token usage if available in the response
            int tokensUsed = extractTokenUsage(root);
            
            logger.info("Successfully parsed Gemini response. Tokens used: {}", tokensUsed);
            
            return new GeminiResponse(summary, tokensUsed);
            
        } catch (Exception e) {
            logger.error("Error parsing Gemini API response", e);
            throw new GeminiApiException("Failed to parse Gemini API response: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts token usage from the API response
     * Gemini API may include usage metadata in the response
     */
    private int extractTokenUsage(JsonNode root) {
        try {
            // Try to get token usage from usageMetadata if present
            JsonNode usageMetadata = root.path("usageMetadata");
            if (usageMetadata.has("totalTokens")) {
                return usageMetadata.path("totalTokens").asInt(0);
            }
            
            // Estimate based on content if no metadata available
            JsonNode content = root.path("candidates").get(0).path("content");
            if (content.has("parts")) {
                String text = content.path("parts").get(0).path("text").asText("");
                // Rough estimation: ~4 characters per token
                return Math.max((text.length() / 4) + 1, 1);
            }
            
            return 0;
        } catch (Exception e) {
            logger.warn("Could not extract token usage from response", e);
            return 0;
        }
    }

    /**
     * Escapes special characters for JSON
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Response wrapper containing summary and token usage
     */
    public static class GeminiResponse {
        private final String summary;
        private final int tokensUsed;

        public GeminiResponse(String summary, int tokensUsed) {
            this.summary = summary;
            this.tokensUsed = tokensUsed;
        }

        public String getSummary() {
            return summary;
        }

        public int getTokensUsed() {
            return tokensUsed;
        }
    }
}
