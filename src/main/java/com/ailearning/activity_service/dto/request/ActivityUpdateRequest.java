package com.ailearning.activity_service.dto.request;

import com.ailearning.activity_service.enums.ActivityStatus;
import com.ailearning.activity_service.enums.ActivityType;
import com.ailearning.activity_service.enums.DifficultyLevel;
import com.ailearning.activity_service.enums.GradeLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ActivityUpdateRequest(

        @NotNull(message = "Module ID is required")
        Long moduleId,

        @NotNull(message = "Grade level is required")
        GradeLevel gradeLevel,

        @NotNull(message = "Display order is required")
        @Positive(message = "Display order must be positive")
        Integer displayOrder,

        @NotBlank(message = "Title is required")
        @Size(max = 150)
        String title,

        @Size(max = 1000)
        String description,

        @NotNull(message = "Activity type is required")
        ActivityType activityType,

        @NotNull(message = "Difficulty is required")
        DifficultyLevel difficulty,

        @Positive(message = "Time limit must be positive")
        Integer timeLimitMinutes,

        @NotNull(message = "Status is required")
        ActivityStatus status,

        @NotEmpty(message = "At least one question is required")
        @Valid
        List<QuestionCreateRequest> questions
) {}