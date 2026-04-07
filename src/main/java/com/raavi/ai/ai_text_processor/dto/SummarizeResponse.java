package com.raavi.ai.ai_text_processor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing the generated summary and metadata")
public class SummarizeResponse {

    @Schema(description = "Unique identifier of the saved summary", example = "42")
    private Long id;

    @Schema(description = "The original text that was submitted for summarization")
    private String originalText;

    @Schema(description = "The AI-generated summary", example = "AI is revolutionizing industries by automating tasks and improving accuracy across healthcare, finance, and other sectors.")
    private String summarizedText;

    @Schema(description = "The summary style used", example = "concise",
            allowableValues = {"concise", "detailed", "bullet_points", "executive"})
    private String summaryType;

    @Schema(description = "Number of tokens consumed by the Gemini API call", example = "256")
    private Integer tokensUsed;

    @Schema(description = "The Gemini model used to generate the summary", example = "gemini-2.5-flash")
    private String modelUsed;

    @Schema(description = "Timestamp when the summary was created", example = "2026-04-06T12:00:00")
    private LocalDateTime createdAt;
}