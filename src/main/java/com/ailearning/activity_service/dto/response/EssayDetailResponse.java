package com.ailearning.activity_service.dto.response;

public record EssayDetailResponse(
        Long essayId,
        Integer maxWordCount,
        Integer minWordCount,
        String gradingRubric
) {}