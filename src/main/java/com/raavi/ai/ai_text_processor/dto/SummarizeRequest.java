package com.raavi.ai.ai_text_processor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for text summarization")
public class SummarizeRequest {

    @NotBlank(message = "Text cannot be blank")
    @Size(min = 10, max = 50000, message = "Text length must be between 10 and 50000 characters")
    @Schema(
            description = "The text to summarize. Must be between 10 and 50,000 characters.",
            example = "Artificial intelligence is transforming industries worldwide. From healthcare to finance, AI systems are automating complex tasks, improving accuracy, and enabling new capabilities that were previously impossible.",
            minLength = 10,
            maxLength = 50000
    )
    private String text;

    @Schema(
            description = "The style of summary to generate. Defaults to 'concise' if not provided.",
            example = "concise",
            allowableValues = {"concise", "detailed", "bullet_points", "executive"}
    )
    private String summaryType;
}