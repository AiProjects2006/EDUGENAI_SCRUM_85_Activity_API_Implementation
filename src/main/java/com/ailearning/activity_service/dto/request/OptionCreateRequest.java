package com.ailearning.activity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OptionCreateRequest(

        @NotBlank(message = "Option text is required")
        String optionText,

        @NotNull(message = "Correct flag is required")
        Boolean correct,

        @NotNull(message = "Display order is required")
        @Positive(message = "Display order must be positive")
        Integer displayOrder
) {
}