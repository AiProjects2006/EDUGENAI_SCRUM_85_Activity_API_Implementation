package com.ailearning.activity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record FillBlankCreateRequest(

        @NotBlank(message = "Correct answer is required")
        String correctAnswer,

        @NotNull(message = "Blank index is required")
        @PositiveOrZero(message = "Blank index cannot be negative")
        Integer blankIndex
) {
}