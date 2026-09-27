package com.ailearning.activity_service.dto.response;

public record SortingItemDetailResponse(
        Long itemId,
        String itemText,
        Integer correctOrder
) {}