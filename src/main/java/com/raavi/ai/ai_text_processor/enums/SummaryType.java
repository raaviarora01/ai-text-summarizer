package com.raavi.ai.ai_text_processor.enums;

public enum SummaryType {
    CONCISE(
        "concise",
        "Summarize in 2-3 sentences. Capture only the essential points."
    ),
    DETAILED(
        "detailed",
        "Write a thorough summary covering all major points, context, and nuances." +
        "Use multiple paragraphs where appropriate."
    ),
    BULLET_POINTS(
        "bullet_points",
        "Summarize as a clean bulleted list. Each bullet must be one clear, self-contained idea."
    ),
    EXECUTIVE(
        "executive",
        "Write a business executive summary. " +
        "Structure it as: Situation, Key Findings, Recommended Actions."
    );

    private final String type;
    private final String prompt;

    SummaryType(String type, String prompt) {
        this.type = type;
        this.prompt = prompt;
    }

    public String getType() {
        return type;
    }

    public String getPrompt() {
        return prompt;
    }

    public static SummaryType fromString(String type) {
        if (type == null || type.isEmpty()) {
            return CONCISE; // default
        }
        for (SummaryType summaryType : SummaryType.values()) {
            if (summaryType.type.equalsIgnoreCase(type)) {
                return summaryType;
            }
        }
        throw new IllegalArgumentException("Invalid summary type: " + type);
    }
}
