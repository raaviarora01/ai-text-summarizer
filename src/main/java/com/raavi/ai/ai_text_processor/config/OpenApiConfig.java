package com.raavi.ai.ai_text_processor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;

import java.util.List;

public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AI Text Processor API")
                .description(
                    "REST API for AI-powered text summarization using Google Gemini. " +
                        "Supports multiple summary styles: Concise, Detailed, Bullet Points, and Executive. " +
                        "Responses are cached to avoid redundant Gemini API calls. " +
                        "Rate limited to 10 requests/min on summarize and 60 requests/min globally per IP."
                )
                .version("1.0.0")
                .contact(new Contact()
                    .name("Raavi")
                    .email("raavi@example.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Development")
            ));
    }
}
