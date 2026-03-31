package com.raavi.ai.ai_text_processor.exception;

/**
 * Exception thrown when Gemini API rate limit is exceeded
 */
public class RateLimitException extends RuntimeException {
    private final int retryAfterSeconds;

    public RateLimitException(String message) {
        super(message);
        this.retryAfterSeconds = 60; // default 1 minute
    }

    public RateLimitException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitException(String message, int retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public int getHttpStatus() {
        return 429; // Too Many Requests
    }
}
