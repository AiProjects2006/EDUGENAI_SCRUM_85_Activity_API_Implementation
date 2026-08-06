package com.ailearning.activity_service.service.impl;

import com.ailearning.activity_service.dto.request.ActivityGenerationRequest;
import com.ailearning.activity_service.dto.response.ActivityGenerationResponse;
import com.ailearning.activity_service.dto.response.OptionResponse;
import com.ailearning.activity_service.dto.response.QuestionResponse;
import com.ailearning.activity_service.enums.GradeLevel;
import com.ailearning.activity_service.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    @Override
    public ActivityGenerationResponse generateActivity(ActivityGenerationRequest request) {

        // TODO: Replace with AI service call

        OptionResponse option1 = OptionResponse.builder()
                .optionText("Dog")
                .displayOrder(1)
                .build();

        OptionResponse option2 = OptionResponse.builder()
                .optionText("Cat")
                .displayOrder(2)
                .build();

        OptionResponse option3 = OptionResponse.builder()
                .optionText("Cow")
                .displayOrder(3)
                .build();

        List<OptionResponse> options = List.of(option1, option2, option3);

        QuestionResponse question = QuestionResponse.builder()
                .questionText("Which animal barks?")
                .instruction("Choose one answer.")
                .points(2)
                .displayOrder(1)
                .options(options)
                .build();

        List<QuestionResponse> questions = List.of(question);

        ActivityGenerationResponse response = ActivityGenerationResponse.builder()
                .title("Animals Quiz")
                /*Module module = moduleService.getModule(request.getModuleId());
                .title(module.getTitle() + " Quiz");*/
                .description("Choose the correct answer.")
                .activityType(request.getActivityType())
                .difficulty(request.getDifficultyLevel())
                .gradeLevel(GradeLevel.GRADE_3)
                /*ModuleDTO module = moduleClient.getModule(request.getModuleId());
                .gradeLevel(module.getGradeLevel())*/
                .timeLimitMinutes(10)
                .totalMarks(10)
                .questions(questions)
                .build();

        // TODO:
        // 1. Retrieve module details using moduleId
        // 2. Build AI request
        // 3. Call AI service
        // 4. Return AI response
        return response;
    }
}