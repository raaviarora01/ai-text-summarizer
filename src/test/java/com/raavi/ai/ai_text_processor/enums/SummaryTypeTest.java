package com.raavi.ai.ai_text_processor.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SummaryType enum.
 * Tests all valid types, case-insensitivity, null/empty handling, and invalid input.
 */
@DisplayName("SummaryType Enum Tests")
public class SummaryTypeTest {

    // ─── fromString: valid values ─────────────────────────────────────────────

    @Test
    @DisplayName("fromString returns CONCISE for 'concise'")
    void fromString_concise_lowercase() {
        assertThat(SummaryType.fromString("concise")).isEqualTo(SummaryType.CONCISE);
    }

    @Test
    @DisplayName("fromString returns DETAILED for 'detailed'")
    void fromString_detailed() {
        assertThat(SummaryType.fromString("detailed")).isEqualTo(SummaryType.DETAILED);
    }

    @Test
    @DisplayName("fromString returns BULLET_POINTS for 'bullet_points'")
    void fromString_bulletPoints() {
        assertThat(SummaryType.fromString("bullet_points")).isEqualTo(SummaryType.BULLET_POINTS);
    }

    @Test
    @DisplayName("fromString returns EXECUTIVE for 'executive'")
    void fromString_executive() {
        assertThat(SummaryType.fromString("executive")).isEqualTo(SummaryType.EXECUTIVE);
    }

    // ─── fromString: case-insensitivity ───────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"CONCISE", "Concise", "cOnCiSe"})
    @DisplayName("fromString is case-insensitive for CONCISE")
    void fromString_caseInsensitive(String input) {
        assertThat(SummaryType.fromString(input)).isEqualTo(SummaryType.CONCISE);
    }

    // ─── fromString: null/empty defaults to CONCISE ───────────────────────────

    @Test
    @DisplayName("fromString returns CONCISE for null")
    void fromString_null_returnsDefault() {
        assertThat(SummaryType.fromString(null)).isEqualTo(SummaryType.CONCISE);
    }

    @Test
    @DisplayName("fromString returns CONCISE for empty string")
    void fromString_empty_returnsDefault() {
        assertThat(SummaryType.fromString("")).isEqualTo(SummaryType.CONCISE);
    }

    // ─── fromString: invalid value ────────────────────────────────────────────

    @Test
    @DisplayName("fromString throws IllegalArgumentException for invalid type")
    void fromString_invalid_throwsException() {
        assertThatThrownBy(() -> SummaryType.fromString("invalid_type"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid summary type: invalid_type");
    }

    @Test
    @DisplayName("fromString throws IllegalArgumentException for 'random'")
    void fromString_random_throwsException() {
        assertThatThrownBy(() -> SummaryType.fromString("random"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── getType ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getType returns correct string values for all enum constants")
    void getType_allValues() {
        assertThat(SummaryType.CONCISE.getType()).isEqualTo("concise");
        assertThat(SummaryType.DETAILED.getType()).isEqualTo("detailed");
        assertThat(SummaryType.BULLET_POINTS.getType()).isEqualTo("bullet_points");
        assertThat(SummaryType.EXECUTIVE.getType()).isEqualTo("executive");
    }

    // ─── getPrompt ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPrompt returns non-empty prompt for all enum constants")
    void getPrompt_notEmpty() {
        for (SummaryType type : SummaryType.values()) {
            assertThat(type.getPrompt())
                    .as("Prompt for %s should not be blank", type)
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("CONCISE prompt mentions 2-3 sentences")
    void concise_prompt_content() {
        assertThat(SummaryType.CONCISE.getPrompt()).contains("2-3 sentences");
    }

    @Test
    @DisplayName("EXECUTIVE prompt mentions executive summary structure")
    void executive_prompt_content() {
        assertThat(SummaryType.EXECUTIVE.getPrompt()).contains("executive");
    }

    // ─── enum values count ────────────────────────────────────────────────────

    @Test
    @DisplayName("SummaryType has exactly 4 values")
    void values_count() {
        assertThat(SummaryType.values()).hasSize(4);
    }
}
