package com.raavi.ai.ai_text_processor;

import com.raavi.ai.ai_text_processor.service.GeminiService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:apptest;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "gemini.api.key=test-api-key-not-real-123456789012345",
        "gemini.model=gemini-2.5-flash",
        "gemini.api.url=https://generativelanguage.googleapis.com/v1/models/",
        "bucket4j.enabled=false"
})
class AiTextProcessorApplicationTests {

    @MockitoBean
    private GeminiService geminiService;

    @Test
    void contextLoads() {
        // Verifies the Spring context starts successfully with H2 and mocked Gemini
    }
}
