package com.ailearning.activity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MatchingPairCreateRequest(

        @NotBlank(message = "Left item is required")
        String leftItem,

        @NotBlank(message = "Right item is required")
        String rightItem,

        @NotNull(message = "Display order is required")
        @Positive(message = "Display order must be positive")
        Integer displayOrder
) {
}