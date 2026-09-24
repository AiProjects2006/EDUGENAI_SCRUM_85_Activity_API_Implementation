package com.ailearning.activity_service.dto.response;

public record OptionDetailResponse(
        Long optionId,
        String optionText,
        Boolean correct,
        Integer displayOrder
) {}