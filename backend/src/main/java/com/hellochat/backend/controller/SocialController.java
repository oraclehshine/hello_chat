package com.hellochat.backend.controller;

import com.hellochat.backend.common.ApiResponse;
import com.hellochat.backend.common.CurrentUser;
import com.hellochat.backend.common.TokenProvider;
import com.hellochat.backend.dto.PresenceRequest;
import com.hellochat.backend.dto.PresenceResponse;
import com.hellochat.backend.dto.RecommendationResponse;
import com.hellochat.backend.dto.SearchHistoryResponse;
import com.hellochat.backend.dto.SocialSearchResponse;
import com.hellochat.backend.service.SocialService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social")
public class SocialController {

    private final SocialService socialService;
    private final TokenProvider tokenProvider;

    public SocialController(SocialService socialService, TokenProvider tokenProvider) {
        this.socialService = socialService;
        this.tokenProvider = tokenProvider;
    }

    @PutMapping("/presence")
    public ApiResponse<PresenceResponse> updatePresence(
        HttpServletRequest request,
        @RequestBody PresenceRequest presenceRequest
    ) {
        return ApiResponse.success(socialService.updatePresence(currentUserId(request), presenceRequest.getStatus()));
    }

    @GetMapping("/presence/{userId}")
    public ApiResponse<PresenceResponse> getPresence(@PathVariable Long userId) {
        return ApiResponse.success(socialService.getPresence(userId));
    }

    @GetMapping("/search")
    public ApiResponse<SocialSearchResponse> search(
        HttpServletRequest request,
        @RequestParam(defaultValue = "") String keyword
    ) {
        return ApiResponse.success(socialService.search(currentUserId(request), keyword));
    }

    @GetMapping("/search/history")
    public ApiResponse<List<SearchHistoryResponse>> listSearchHistory(HttpServletRequest request) {
        return ApiResponse.success(socialService.listSearchHistory(currentUserId(request)));
    }

    @GetMapping("/recommendations")
    public ApiResponse<RecommendationResponse> recommend(HttpServletRequest request) {
        return ApiResponse.success(socialService.recommend(currentUserId(request)));
    }

    private Long currentUserId(HttpServletRequest request) {
        return CurrentUser.requireUserId(request, tokenProvider);
    }
}
