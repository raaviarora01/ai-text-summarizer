package com.raavi.ai.ai_text_processor.suite;

import com.raavi.ai.ai_text_processor.controller.CacheStatsControllerTest;
import com.raavi.ai.ai_text_processor.controller.TextSummarizerControllerTest;
import com.raavi.ai.ai_text_processor.enums.SummaryTypeTest;
import com.raavi.ai.ai_text_processor.integration.FullIntegrationTest;
import com.raavi.ai.ai_text_processor.integration.RateLimitIntegrationTest;
import com.raavi.ai.ai_text_processor.repository.TextSummaryRepositoryTest;
import com.raavi.ai.ai_text_processor.service.ExceptionTest;
import com.raavi.ai.ai_text_processor.service.TextSummarizerServiceTest;
import com.raavi.ai.ai_text_processor.util.CacheKeyGeneratorTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * AI Text Processor — Full Regression Test Suite
 *
 * Runs all test classes in a single suite for regression testing.
 *
 * To run the full suite:
 *   mvn test -Dtest=AiTextProcessorTestSuite
 *
 * To run individual test classes:
 *   mvn test -Dtest=SummaryTypeTest
 *   mvn test -Dtest=CacheKeyGeneratorTest
 *   mvn test -Dtest=ExceptionTest
 *   mvn test -Dtest=TextSummarizerServiceTest
 *   mvn test -Dtest=TextSummarizerControllerTest
 *   mvn test -Dtest=CacheStatsControllerTest
 *   mvn test -Dtest=TextSummaryRepositoryTest
 *   mvn test -Dtest=FullIntegrationTest
 *   mvn test -Dtest=RateLimitIntegrationTest
 *
 * Suite breakdown:
 *
 *   UNIT TESTS (fast, no Spring context)
 *   ├── SummaryTypeTest            — enum fromString, getType, getPrompt
 *   ├── CacheKeyGeneratorTest      — SHA-256 hashing, key format, collision resistance
 *   ├── ExceptionTest              — GeminiApiException + RateLimitException constructors
 *   └── TextSummarizerServiceTest  — service logic with mocked GeminiService + repository
 *
 *   CONTROLLER SLICE TESTS (@WebMvcTest — only web layer)
 *   ├── TextSummarizerControllerTest — all endpoints, all status codes, validation
 *   └── CacheStatsControllerTest     — /api/cache/stats and /api/cache/info
 *
 *   REPOSITORY TESTS (@DataJpaTest — only JPA layer with H2)
 *   └── TextSummaryRepositoryTest  — CRUD, findBySummaryType, pagination
 *
 *   INTEGRATION TESTS (full Spring context + H2 + mocked Gemini)
 *   ├── FullIntegrationTest        — E2E flow, caching, actuator, Swagger
 *   └── RateLimitIntegrationTest   — Bucket4j headers, 429 trigger, filter isolation
 */
@Suite
@SuiteDisplayName("AI Text Processor — Full Regression Suite")
@SelectClasses({
        // Unit tests
        SummaryTypeTest.class,
        CacheKeyGeneratorTest.class,
        ExceptionTest.class,
        TextSummarizerServiceTest.class,

        // Controller slice tests
        TextSummarizerControllerTest.class,
        CacheStatsControllerTest.class,

        // Repository tests
        TextSummaryRepositoryTest.class,

        // Integration tests
        FullIntegrationTest.class,
        RateLimitIntegrationTest.class
})
public class AiTextProcessorTestSuite {
    // This class is intentionally empty.
    // It exists only as a JUnit Platform Suite entry point.
}
