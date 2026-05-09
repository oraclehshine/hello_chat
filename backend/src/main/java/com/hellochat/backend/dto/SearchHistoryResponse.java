package com.hellochat.backend.dto;

import com.hellochat.backend.entity.SearchHistory;
import java.time.LocalDateTime;

public class SearchHistoryResponse {

    private final Long historyId;
    private final String keyword;
    private final String searchType;
    private final LocalDateTime createdAt;

    public SearchHistoryResponse(SearchHistory history) {
        this.historyId = history.getId();
        this.keyword = history.getKeyword();
        this.searchType = history.getSearchType();
        this.createdAt = history.getCreatedAt();
    }

    public Long getHistoryId() {
        return historyId;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getSearchType() {
        return searchType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
