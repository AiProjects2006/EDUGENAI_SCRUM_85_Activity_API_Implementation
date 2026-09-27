package com.ailearning.activity_service.dto.response;

public record DragItemDetailResponse(
        Long itemId,
        String itemText,
        String imageUrl
) {}