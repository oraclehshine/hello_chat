package com.hellochat.backend.repository;

import com.hellochat.backend.entity.SearchHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
