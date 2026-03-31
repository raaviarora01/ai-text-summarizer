package com.raavi.ai.ai_text_processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AiTextProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiTextProcessorApplication.class, args);
	}

}
