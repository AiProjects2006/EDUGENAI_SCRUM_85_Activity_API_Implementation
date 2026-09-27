package com.ailearning.activity_service.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EssayCreateRequest(

        @Positive(message = "Maximum word count must be positive")
        Integer maxWordCount,

        @Positive(message = "Minimum word count must be positive")
        Integer minWordCount,

        @Size(max = 1000, message = "Grading rubric cannot exceed 1000 characters")
        String gradingRubric
) {
}