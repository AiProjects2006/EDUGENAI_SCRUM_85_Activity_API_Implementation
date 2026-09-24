package com.ailearning.activity_service.dto.response;

import com.ailearning.activity_service.enums.ActivityStatus;
import com.ailearning.activity_service.enums.ActivityType;
import com.ailearning.activity_service.enums.DifficultyLevel;
import com.ailearning.activity_service.enums.GradeLevel;

import java.util.List;

public record ActivityDetailResponse(
        Long activityId,
        Long moduleId,
        GradeLevel gradeLevel,
        Integer displayOrder,
        String title,
        String description,
        ActivityType activityType,
        DifficultyLevel difficulty,
        Integer timeLimitMinutes,
        Integer totalMarks,
        ActivityStatus status,
        List<QuestionDetailResponse> questions
) {}