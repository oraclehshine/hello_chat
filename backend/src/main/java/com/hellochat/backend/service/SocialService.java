package com.hellochat.backend.service;

import com.hellochat.backend.dto.PresenceResponse;
import com.hellochat.backend.dto.RecommendationResponse;
import com.hellochat.backend.dto.SearchHistoryResponse;
import com.hellochat.backend.dto.SocialSearchResponse;
import java.util.List;

public interface SocialService {

    PresenceResponse updatePresence(Long userId, String status);

    PresenceResponse getPresence(Long userId);

    SocialSearchResponse search(Long userId, String keyword);

    List<SearchHistoryResponse> listSearchHistory(Long userId);

    RecommendationResponse recommend(Long userId);
}
