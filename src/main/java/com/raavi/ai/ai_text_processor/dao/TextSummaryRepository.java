package com.raavi.ai.ai_text_processor.dao;

import com.raavi.ai.ai_text_processor.entity.TextSummary;
import com.raavi.ai.ai_text_processor.enums.SummaryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TextSummaryRepository extends JpaRepository<TextSummary, Long> {
    
    /**
     * Find all summaries of a specific type with pagination
     * @param summaryType The type of summary to filter by
     * @param pageable Pagination parameters
     * @return Page of summaries matching the type
     */
    Page<TextSummary> findBySummaryType(SummaryType summaryType, Pageable pageable);
}
