package com.raavi.ai.ai_text_processor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummarizeRequest {
    @NotBlank(message = "Text cannot be blank")
    @Size(min = 10, max = 50000, message = "Text length must be between 10 and 50000 characters")
    private String text;

    private String summaryType; // Will default to CONCISE if not provided
}
