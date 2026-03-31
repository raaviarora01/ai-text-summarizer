package com.raavi.ai.ai_text_processor.exception;

/**
 * Exception thrown when Gemini API call fails
 */
public class GeminiApiException extends RuntimeException {
    private final int httpStatus;

    public GeminiApiException(String message) {
        super(message);
        this.httpStatus = 502; // Bad Gateway
    }

    public GeminiApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = 502;
    }

    public GeminiApiException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public GeminiApiException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
