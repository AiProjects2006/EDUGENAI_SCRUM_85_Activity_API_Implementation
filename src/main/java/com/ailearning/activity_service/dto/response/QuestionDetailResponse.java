package com.ailearning.activity_service.dto.response;

import java.util.List;

public record QuestionDetailResponse(
        Long questionId,
        String questionText,
        String instructions,
        String imageUrl,
        Integer points,
        Integer displayOrder,
        List<OptionDetailResponse> options
) {}