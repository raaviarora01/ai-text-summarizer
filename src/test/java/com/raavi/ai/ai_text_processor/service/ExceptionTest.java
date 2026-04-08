package com.raavi.ai.ai_text_processor.service;

import com.raavi.ai.ai_text_processor.exception.GeminiApiException;
import com.raavi.ai.ai_text_processor.exception.RateLimitException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for custom exception classes.
 * Verifies correct HTTP status codes, messages, and constructors.
 */
@DisplayName("Exception Classes Tests")
class ExceptionTest {

    // ─── GeminiApiException ───────────────────────────────────────────────────

    @Test
    @DisplayName("GeminiApiException default status is 502")
    void geminiException_defaultStatus() {
        GeminiApiException ex = new GeminiApiException("error");
        assertThat(ex.getHttpStatus()).isEqualTo(502);
    }

    @Test
    @DisplayName("GeminiApiException stores custom HTTP status")
    void geminiException_customStatus() {
        GeminiApiException ex = new GeminiApiException("error", 400);
        assertThat(ex.getHttpStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("GeminiApiException stores message correctly")
    void geminiException_message() {
        GeminiApiException ex = new GeminiApiException("Gemini failed");
        assertThat(ex.getMessage()).isEqualTo("Gemini failed");
    }

    @Test
    @DisplayName("GeminiApiException with cause stores cause")
    void geminiException_withCause() {
        RuntimeException cause = new RuntimeException("root cause");
        GeminiApiException ex = new GeminiApiException("wrapper", cause);
        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex.getHttpStatus()).isEqualTo(502);
    }

    @Test
    @DisplayName("GeminiApiException with status and cause")
    void geminiException_statusAndCause() {
        RuntimeException cause = new RuntimeException("root");
        GeminiApiException ex = new GeminiApiException("msg", 503, cause);
        assertThat(ex.getHttpStatus()).isEqualTo(503);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    // ─── RateLimitException ───────────────────────────────────────────────────

    @Test
    @DisplayName("RateLimitException default retryAfter is 60 seconds")
    void rateLimitException_defaultRetryAfter() {
        RateLimitException ex = new RateLimitException("rate limit");
        assertThat(ex.getRetryAfterSeconds()).isEqualTo(60);
    }

    @Test
    @DisplayName("RateLimitException custom retryAfter is stored correctly")
    void rateLimitException_customRetryAfter() {
        RateLimitException ex = new RateLimitException("rate limit", 120);
        assertThat(ex.getRetryAfterSeconds()).isEqualTo(120);
    }

    @Test
    @DisplayName("RateLimitException HTTP status is always 429")
    void rateLimitException_httpStatus() {
        RateLimitException ex = new RateLimitException("rate limit", 30);
        assertThat(ex.getHttpStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("RateLimitException message is stored correctly")
    void rateLimitException_message() {
        RateLimitException ex = new RateLimitException("Too many requests");
        assertThat(ex.getMessage()).isEqualTo("Too many requests");
    }

    @Test
    @DisplayName("RateLimitException with cause stores all fields")
    void rateLimitException_withCause() {
        RuntimeException cause = new RuntimeException("root");
        RateLimitException ex = new RateLimitException("rate limit", 45, cause);
        assertThat(ex.getRetryAfterSeconds()).isEqualTo(45);
        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex.getHttpStatus()).isEqualTo(429);
    }
}
