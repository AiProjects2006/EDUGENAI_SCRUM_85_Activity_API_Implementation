package com.ailearning.activity_service.dto.response;

import com.ailearning.activity_service.enums.ActivityType;
import com.ailearning.activity_service.enums.DifficultyLevel;
import com.ailearning.activity_service.enums.GradeLevel;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityGenerationResponse {

    private String title;

    private String description;

    private ActivityType activityType;

    private DifficultyLevel difficulty;

    private GradeLevel gradeLevel;

    private Integer timeLimitMinutes;

    private Integer totalMarks;

    private List<QuestionResponse> questions;
}
