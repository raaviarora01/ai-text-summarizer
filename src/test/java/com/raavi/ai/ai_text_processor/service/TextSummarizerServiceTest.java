package com.raavi.ai.ai_text_processor.service;

import com.raavi.ai.ai_text_processor.dao.TextSummaryRepository;
import com.raavi.ai.ai_text_processor.dto.SummarizeRequest;
import com.raavi.ai.ai_text_processor.dto.SummarizeResponse;
import com.raavi.ai.ai_text_processor.entity.TextSummary;
import com.raavi.ai.ai_text_processor.enums.SummaryType;
import com.raavi.ai.ai_text_processor.exception.GeminiApiException;
import com.raavi.ai.ai_text_processor.serviceImpl.TextSummarizerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TextSummarizerServiceImpl.
 * GeminiService and repository are mocked — no DB or API calls.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TextSummarizerService Unit Tests")
class TextSummarizerServiceTest {

    @Mock
    private GeminiService geminiService;

    @Mock
    private TextSummaryRepository textSummaryRepository;

    @InjectMocks
    private TextSummarizerServiceImpl service;

    private TextSummary savedEntity;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "geminiModel", "gemini-2.5-flash");

        savedEntity = new TextSummary();
        savedEntity.setId(1L);
        savedEntity.setOriginalText("This is a test text that needs to be summarized.");
        savedEntity.setSummarizedText("Test summary.");
        savedEntity.setSummaryType(SummaryType.CONCISE);
        savedEntity.setTokensUsed(100);
        savedEntity.setModelUsed("gemini-2.5-flash");
        savedEntity.setCreatedAt(LocalDateTime.now());
    }

    // ─── summarizeText: happy path ────────────────────────────────────────────

    @Test
    @DisplayName("summarizeText returns response with correct fields")
    void summarizeText_success() {
        SummarizeRequest request = new SummarizeRequest(
                "This is a test text that needs to be summarized.", "concise");

        when(geminiService.summarizeText(anyString(), anyString()))
                .thenReturn(new GeminiService.GeminiResponse("Test summary.", 100));
        when(textSummaryRepository.save(any(TextSummary.class))).thenReturn(savedEntity);

        SummarizeResponse response = service.summarizeText(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSummarizedText()).isEqualTo("Test summary.");
        assertThat(response.getSummaryType()).isEqualTo("concise");
        assertThat(response.getModelUsed()).isEqualTo("gemini-2.5-flash");
    }

    @Test
    @DisplayName("summarizeText defaults to CONCISE when summaryType is null")
    void summarizeText_nullSummaryType_defaultsConcise() {
        SummarizeRequest request = new SummarizeRequest(
                "This is a test text that needs to be summarized.", null);

        when(geminiService.summarizeText(anyString(), anyString()))
                .thenReturn(new GeminiService.GeminiResponse("Concise summary.", 80));
        when(textSummaryRepository.save(any(TextSummary.class))).thenReturn(savedEntity);

        SummarizeResponse response = service.summarizeText(request);

        assertThat(response).isNotNull();
        verify(geminiService).summarizeText(anyString(), contains("2-3 sentences"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"concise", "detailed", "bullet_points", "executive"})
    @DisplayName("summarizeText works with all valid summary types")
    void summarizeText_allValidTypes(String summaryType) {
        SummarizeRequest request = new SummarizeRequest(
                "Text that is long enough for summarization testing.", summaryType);

        when(geminiService.summarizeText(anyString(), anyString()))
                .thenReturn(new GeminiService.GeminiResponse("Summary for " + summaryType, 50));

        TextSummary entity = new TextSummary();
        entity.setId(1L);
        entity.setOriginalText(request.getText());
        entity.setSummarizedText("Summary for " + summaryType);
        entity.setSummaryType(SummaryType.fromString(summaryType));
        entity.setTokensUsed(50);
        entity.setModelUsed("gemini-2.5-flash");
        entity.setCreatedAt(LocalDateTime.now());

        when(textSummaryRepository.save(any())).thenReturn(entity);

        assertThatCode(() -> service.summarizeText(request)).doesNotThrowAnyException();
    }

    // ─── summarizeText: validation failures ───────────────────────────────────

    @Test
    @DisplayName("summarizeText throws IllegalArgumentException for null text")
    void summarizeText_nullText_throws() {
        SummarizeRequest request = new SummarizeRequest(null, "concise");
        assertThatThrownBy(() -> service.summarizeText(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Text cannot be null or empty");
    }

    @Test
    @DisplayName("summarizeText throws IllegalArgumentException for empty text")
    void summarizeText_emptyText_throws() {
        SummarizeRequest request = new SummarizeRequest("", "concise");
        assertThatThrownBy(() -> service.summarizeText(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("summarizeText throws IllegalArgumentException for text shorter than 10 chars")
    void summarizeText_tooShortText_throws() {
        SummarizeRequest request = new SummarizeRequest("short", "concise");
        assertThatThrownBy(() -> service.summarizeText(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 characters");
    }

    @Test
    @DisplayName("summarizeText throws for text exceeding 50000 characters")
    void summarizeText_tooLongText_throws() {
        String tooLong = "a".repeat(50001);
        SummarizeRequest request = new SummarizeRequest(tooLong, "concise");
        assertThatThrownBy(() -> service.summarizeText(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50000");
    }

    @Test
    @DisplayName("summarizeText throws IllegalArgumentException for invalid summaryType")
    void summarizeText_invalidSummaryType_throws() {
        SummarizeRequest request = new SummarizeRequest(
                "This is a test text that needs to be summarized.", "invalid_type");
        assertThatThrownBy(() -> service.summarizeText(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid summary type");
    }

    // ─── summarizeText: Gemini API failure ────────────────────────────────────

    @Test
    @DisplayName("summarizeText propagates GeminiApiException when API fails")
    void summarizeText_geminiFailure_propagatesException() {
        SummarizeRequest request = new SummarizeRequest(
                "This is a test text that needs to be summarized.", "concise");

        when(geminiService.summarizeText(anyString(), anyString()))
                .thenThrow(new GeminiApiException("Gemini unavailable", 503));

        assertThatThrownBy(() -> service.summarizeText(request))
                .isInstanceOf(GeminiApiException.class)
                .hasMessageContaining("Gemini unavailable");
    }

    @Test
    @DisplayName("summarizeText wraps unexpected exception in RuntimeException")
    void summarizeText_unexpectedException_wraps() {
        SummarizeRequest request = new SummarizeRequest(
                "This is a test text that needs to be summarized.", "concise");

        when(geminiService.summarizeText(anyString(), anyString()))
                .thenThrow(new RuntimeException("Unexpected DB failure"));

        assertThatThrownBy(() -> service.summarizeText(request))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── getHistory ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getHistory returns mapped page from repository")
    void getHistory_returnsMappedPage() {
        Page<TextSummary> repoPage = new PageImpl<>(List.of(savedEntity));
        when(textSummaryRepository.findAll(any(PageRequest.class))).thenReturn(repoPage);

        Page<SummarizeResponse> result = service.getHistory(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getHistory returns empty page when no records exist")
    void getHistory_emptyResult() {
        when(textSummaryRepository.findAll(any(PageRequest.class)))
                .thenReturn(Page.empty());

        Page<SummarizeResponse> result = service.getHistory(PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ─── getHistoryByType ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getHistoryByType returns filtered results")
    void getHistoryByType_returnsFiltered() {
        Page<TextSummary> repoPage = new PageImpl<>(List.of(savedEntity));
        when(textSummaryRepository.findBySummaryType(eq(SummaryType.CONCISE), any()))
                .thenReturn(repoPage);

        Page<SummarizeResponse> result = service.getHistoryByType("concise", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(textSummaryRepository).findBySummaryType(SummaryType.CONCISE, PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("getHistoryByType throws IllegalArgumentException for invalid type")
    void getHistoryByType_invalidType_throws() {
        assertThatThrownBy(() -> service.getHistoryByType("not_a_type", PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid summary type");
    }

    // ─── repository interaction ───────────────────────────────────────────────

    @Test
    @DisplayName("summarizeText saves entity to repository exactly once")
    void summarizeText_savesExactlyOnce() {
        SummarizeRequest request = new SummarizeRequest(
                "This is a test text that needs to be summarized.", "concise");

        when(geminiService.summarizeText(anyString(), anyString()))
                .thenReturn(new GeminiService.GeminiResponse("Summary", 50));
        when(textSummaryRepository.save(any())).thenReturn(savedEntity);

        service.summarizeText(request);

        verify(textSummaryRepository, times(1)).save(any(TextSummary.class));
    }

    @Test
    @DisplayName("summarizeText calls GeminiService exactly once")
    void summarizeText_callsGeminiOnce() {
        SummarizeRequest request = new SummarizeRequest(
                "This is a test text that needs to be summarized.", "concise");

        when(geminiService.summarizeText(anyString(), anyString()))
                .thenReturn(new GeminiService.GeminiResponse("Summary", 50));
        when(textSummaryRepository.save(any())).thenReturn(savedEntity);

        service.summarizeText(request);

        verify(geminiService, times(1)).summarizeText(anyString(), anyString());
    }
}
