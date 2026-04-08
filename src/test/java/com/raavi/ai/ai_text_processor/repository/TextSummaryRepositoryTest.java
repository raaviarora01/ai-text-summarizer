package com.raavi.ai.ai_text_processor.repository;

import com.raavi.ai.ai_text_processor.dao.TextSummaryRepository;
import com.raavi.ai.ai_text_processor.entity.TextSummary;
import com.raavi.ai.ai_text_processor.enums.SummaryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository tests using H2 in-memory database.
 * Tests CRUD operations, findBySummaryType, and pagination.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:repotest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@DisplayName("TextSummaryRepository Tests")
class TextSummaryRepositoryTest {

    @Autowired
    private TextSummaryRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // ─── save and findById ────────────────────────────────────────────────────

    @Test
    @DisplayName("save persists entity and assigns ID")
    void save_persistsAndAssignsId() {
        TextSummary entity = buildEntity("Text for saving test.", SummaryType.CONCISE);
        TextSummary saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull().isPositive();
    }

    @Test
    @DisplayName("findById returns saved entity")
    void findById_returnsSavedEntity() {
        TextSummary entity = buildEntity("Text for findById test.", SummaryType.DETAILED);
        TextSummary saved = repository.save(entity);

        Optional<TextSummary> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getOriginalText()).isEqualTo("Text for findById test.");
        assertThat(found.get().getSummaryType()).isEqualTo(SummaryType.DETAILED);
    }

    @Test
    @DisplayName("findById returns empty for non-existent ID")
    void findById_nonExistent_returnsEmpty() {
        Optional<TextSummary> found = repository.findById(99999L);
        assertThat(found).isEmpty();
    }

    // ─── @PrePersist createdAt ────────────────────────────────────────────────

    @Test
    @DisplayName("save auto-sets createdAt via @PrePersist")
    void save_setsCreatedAt() {
        TextSummary entity = buildEntity("Text to check timestamp.", SummaryType.CONCISE);
        TextSummary saved = repository.save(entity);

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    // ─── findBySummaryType ────────────────────────────────────────────────────

    @Test
    @DisplayName("findBySummaryType returns only matching type")
    void findBySummaryType_returnsMatchingType() {
        repository.save(buildEntity("Concise text one.", SummaryType.CONCISE));
        repository.save(buildEntity("Concise text two.", SummaryType.CONCISE));
        repository.save(buildEntity("Detailed text.", SummaryType.DETAILED));

        Page<TextSummary> result = repository.findBySummaryType(
                SummaryType.CONCISE, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allSatisfy(e -> assertThat(e.getSummaryType()).isEqualTo(SummaryType.CONCISE));
    }

    @Test
    @DisplayName("findBySummaryType returns empty page when no match")
    void findBySummaryType_noMatch_returnsEmpty() {
        repository.save(buildEntity("Executive summary text.", SummaryType.EXECUTIVE));

        Page<TextSummary> result = repository.findBySummaryType(
                SummaryType.BULLET_POINTS, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("findBySummaryType works for all four summary types")
    void findBySummaryType_allTypes() {
        for (SummaryType type : SummaryType.values()) {
            repository.save(buildEntity("Text for " + type.getType(), type));
        }

        for (SummaryType type : SummaryType.values()) {
            Page<TextSummary> result = repository.findBySummaryType(
                    type, PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getSummaryType()).isEqualTo(type);
        }
    }

    // ─── pagination ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll returns correct page size")
    void findAll_pagination_correctPageSize() {
        for (int i = 0; i < 15; i++) {
            repository.save(buildEntity("Text number " + i + " for pagination test.", SummaryType.CONCISE));
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

        Page<TextSummary> page = repository.findAll(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("findBySummaryType pagination works correctly")
    void findBySummaryType_pagination() {
        for (int i = 0; i < 12; i++) {
            repository.save(buildEntity("Concise text " + i + " for pagination testing.", SummaryType.CONCISE));
        }

        Page<TextSummary> page0 = repository.findBySummaryType(SummaryType.CONCISE, PageRequest.of(0, 5));
        Page<TextSummary> page1 = repository.findBySummaryType(SummaryType.CONCISE, PageRequest.of(1, 5));

        assertThat(page0.getContent()).hasSize(5);
        assertThat(page0.getTotalElements()).isEqualTo(12);
        assertThat(page1.getContent()).hasSize(5);
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete removes entity from repository")
    void delete_removesEntity() {
        TextSummary entity = buildEntity("Entity to delete.", SummaryType.CONCISE);
        TextSummary saved = repository.save(entity);
        Long id = saved.getId();

        repository.deleteById(id);

        assertThat(repository.findById(id)).isEmpty();
    }

    // ─── count ────────────────────────────────────────────────────────────────

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
