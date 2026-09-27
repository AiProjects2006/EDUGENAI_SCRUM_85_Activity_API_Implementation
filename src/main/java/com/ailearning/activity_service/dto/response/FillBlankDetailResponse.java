package com.ailearning.activity_service.dto.response;

public record FillBlankDetailResponse(
        Long blankId,
        String correctAnswer,
        Integer blankIndex
) {}