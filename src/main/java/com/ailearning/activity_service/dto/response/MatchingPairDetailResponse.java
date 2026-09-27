package com.ailearning.activity_service.dto.response;

public record MatchingPairDetailResponse(
        Long pairId,
        String leftItem,
        String rightItem,
        Integer displayOrder
) {}