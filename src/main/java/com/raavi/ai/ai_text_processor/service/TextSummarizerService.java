package com.raavi.ai.ai_text_processor.service;

import com.raavi.ai.ai_text_processor.dto.SummarizeRequest;
import com.raavi.ai.ai_text_processor.dto.SummarizeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TextSummarizerService {
    
    /**
     * Summarizes the provided text using the specified summary type
     * @param request The summarization request containing text and summary type
     * @return The generated summary with metadata
     */
    SummarizeResponse summarizeText(SummarizeRequest request);

    /**
     * Retrieves history of all summaries with pagination
     * @param pageable Pagination parameters
     * @return Page of summaries
     */
    Page<SummarizeResponse> getHistory(Pageable pageable);

    /**
     * Retrieves history of summaries filtered by summary type
     * @param summaryType The type of summary to filter by
     * @param pageable Pagination parameters
     * @return Page of summaries matching the filter
     */
    Page<SummarizeResponse> getHistoryByType(String summaryType, Pageable pageable);
}
