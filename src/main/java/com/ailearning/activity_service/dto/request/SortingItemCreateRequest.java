package com.ailearning.activity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SortingItemCreateRequest(

        @NotBlank(message = "Item text is required")
        String itemText,

        @NotNull(message = "Correct order is required")
        @Positive(message = "Correct order must be positive")
        Integer correctOrder
) {
}