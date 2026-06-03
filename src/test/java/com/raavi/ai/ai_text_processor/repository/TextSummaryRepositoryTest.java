package com.raavi.ai.ai_text_processor.repository;

import com.raavi.ai.ai_text_processor.dao.TextSummaryRepository;
import com.raavi.ai.ai_text_processor.entity.TextSummary;
import com.raavi.ai.ai_text_processor.enums.SummaryType;
import com.raavi.ai.ai_text_processor.service.GeminiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository tests using H2 in-memory database.
 *
 * @AutoConfigureTestDatabase(replace = ALWAYS) forces Spring to replace
 * any configured datasource (MySQL) with H2 for these tests.
 * This is the correct way in Spring Boot 4 — @DataJpaTest alone is not
 * sufficient when the entity has MySQL-specific column definitions.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@DisplayName("TextSummaryRepository Tests")
public class TextSummaryRepositoryTest {

    @Autowired
    private TextSummaryRepository repository;

    // GeminiService throws IllegalStateException at startup if GEMINI_API_KEY is missing.
    // Even in @DataJpaTest, Spring Boot 4 loads it via the main application class scan.
    // Mock it to prevent context failure.
    @MockitoBean
    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // ─── save and findById ────────────────────────────────────────────────────

    @Test
    @DisplayName("save persists entity and assigns ID")
    void save_persistsAndAssignsId() {
        TextSummary saved = repository.save(buildEntity("Text for saving test.", SummaryType.CONCISE));
        assertThat(saved.getId()).isNotNull().isPositive();
    }

    @Test
    @DisplayName("findById returns saved entity")
    void findById_returnsSavedEntity() {
        TextSummary saved = repository.save(buildEntity("Text for findById test.", SummaryType.DETAILED));

        Optional<TextSummary> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getOriginalText()).isEqualTo("Text for findById test.");
        assertThat(found.get().getSummaryType()).isEqualTo(SummaryType.DETAILED);
    }

    @Test
    @DisplayName("findById returns empty for non-existent ID")
    void findById_nonExistent_returnsEmpty() {
        assertThat(repository.findById(99999L)).isEmpty();
    }

    @Test
    @DisplayName("save auto-sets createdAt via @PrePersist")
    void save_setsCreatedAt() {
        TextSummary saved = repository.save(buildEntity("Text to check timestamp.", SummaryType.CONCISE));
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    // ─── findBySummaryType ────────────────────────────────────────────────────

    @Test
    @DisplayName("findBySummaryType returns only matching type")
    void findBySummaryType_returnsMatchingType() {
        repository.save(buildEntity("Concise text one.", SummaryType.CONCISE));
        repository.save(buildEntity("Concise text two.", SummaryType.CONCISE));
        repository.save(buildEntity("Detailed text.", SummaryType.DETAILED));

        Page<TextSummary> result = repository.findBySummaryType(SummaryType.CONCISE, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allSatisfy(e -> assertThat(e.getSummaryType()).isEqualTo(SummaryType.CONCISE));
    }

    @Test
    @DisplayName("findBySummaryType returns empty page when no match")
    void findBySummaryType_noMatch_returnsEmpty() {
        repository.save(buildEntity("Executive summary text.", SummaryType.EXECUTIVE));

        Page<TextSummary> result = repository.findBySummaryType(SummaryType.BULLET_POINTS, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("findBySummaryType works for all four types")
    void findBySummaryType_allTypes() {
        for (SummaryType type : SummaryType.values()) {
            repository.save(buildEntity("Text for " + type.getType() + " type test.", type));
        }
        for (SummaryType type : SummaryType.values()) {
            Page<TextSummary> result = repository.findBySummaryType(type, PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
        }
    }

    // ─── pagination ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll returns correct page size")
    void findAll_pagination_correctPageSize() {
        for (int i = 0; i < 15; i++) {
            repository.save(buildEntity("Pagination test text number " + i + ".", SummaryType.CONCISE));
        }
        Page<TextSummary> page0 = repository.findAll(PageRequest.of(0, 10));
        Page<TextSummary> page1 = repository.findAll(PageRequest.of(1, 10));

        assertThat(page0.getContent()).hasSize(10);
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page0.getTotalElements()).isEqualTo(15);
    }

    @Test
    @DisplayName("findAll returns total element count correctly")
    void findAll_totalElements() {
        repository.save(buildEntity("First entity.", SummaryType.CONCISE));
        repository.save(buildEntity("Second entity.", SummaryType.DETAILED));
        repository.save(buildEntity("Third entity.", SummaryType.EXECUTIVE));

        assertThat(repository.findAll(PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
    }

    // ─── delete and count ─────────────────────────────────────────────────────

    @Test
    @DisplayName("delete removes entity from repository")
    void delete_removesEntity() {
        TextSummary saved = repository.save(buildEntity("Entity to delete.", SummaryType.CONCISE));
        repository.deleteById(saved.getId());
        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("count returns correct number of entities")
    void count_returnsCorrectNumber() {
        assertThat(repository.count()).isZero();
        repository.save(buildEntity("First.", SummaryType.CONCISE));
        repository.save(buildEntity("Second.", SummaryType.DETAILED));
        assertThat(repository.count()).isEqualTo(2);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private TextSummary buildEntity(String text, SummaryType type) {
        TextSummary entity = new TextSummary();
        entity.setOriginalText(text);
        entity.setSummarizedText("Summary of: " + text);
        entity.setSummaryType(type);
        entity.setTokensUsed(100);
        entity.setModelUsed("gemini-2.5-flash");
        return entity;
    }
}
