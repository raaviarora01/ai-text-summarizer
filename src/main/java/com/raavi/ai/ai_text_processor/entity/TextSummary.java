package com.raavi.ai.ai_text_processor.entity;

import com.raavi.ai.ai_text_processor.enums.SummaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "text_summaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_text", nullable = false, length = 50000)
    private String originalText;

    @Column(name = "summarized_text", length = 50000)
    private String summarizedText;

    @Column(name = "summary_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SummaryType summaryType;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "model_used", nullable = false, length = 100)
    private String modelUsed;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        if (summaryType == null) {
            summaryType = SummaryType.CONCISE;
        }
    }
}
