package com.raavi.ai.ai_text_processor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SummarizeResponse {
    private Long id;
    private String originalText;
    private String summarizedText;
    private String summaryType;
    private Integer tokensUsed;
    private String modelUsed;
    private LocalDateTime createdAt;
}
