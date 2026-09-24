package com.ailearning.activity_service.service.impl;

import com.ailearning.activity_service.dto.request.*;
import com.ailearning.activity_service.dto.response.*;
import com.ailearning.activity_service.dto.request.ActivityCreateRequest;
import com.ailearning.activity_service.dto.request.OptionCreateRequest;
import com.ailearning.activity_service.dto.request.QuestionCreateRequest;
import com.ailearning.activity_service.dto.response.ActivityCreateResponse;
import com.ailearning.activity_service.entity.*;
import com.ailearning.activity_service.entity.Activity;
import com.ailearning.activity_service.entity.Option;
import com.ailearning.activity_service.entity.Question;
import com.ailearning.activity_service.enums.ActivitySource;
import com.ailearning.activity_service.enums.ActivityStatus;
import com.ailearning.activity_service.enums.AttemptStatus;
import com.ailearning.activity_service.enums.GradeLevel;
import com.ailearning.activity_service.exception.ResourceNotFoundException;
import com.ailearning.activity_service.repository.ActivityRepository;
import com.ailearning.activity_service.repository.StudentActivityAttemptRepository;
import com.ailearning.activity_service.service.ActivityService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final StudentActivityAttemptRepository
            studentActivityAttemptRepository;

    private final ActivityRepository activityRepository;

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

    @Override
    public ActivitySubmissionResponse submitActivity(
            ActivitySubmissionRequest request) {

        StudentActivityAttempt attempt = StudentActivityAttempt.builder()
                .studentId(1L)    // TODO: Replace with authenticated student ID
                .source(ActivitySource.AI)
                .activityId(request.getActivityId())
                .startedAt(LocalDateTime.now())
                .submittedAt(LocalDateTime.now())
                .status(AttemptStatus.SUBMITTED)
                .build();

        List<AttemptQuestion> attemptQuestions = new ArrayList<>();

        for (AnswerRequest answer : request.getAnswers()) {

            QuestionResponse question = request.getGeneratedActivity()
                    .getQuestions()
                    .stream()
                    .filter(q -> q.getDisplayOrder()
                            .equals(answer.getQuestionNumber()))
                    .findFirst()
                    .orElse(null);

            if (question == null) {
                continue;
            }

            AttemptQuestion attemptQuestion = AttemptQuestion.builder()
                    .attempt(attempt)
                    .questionType(
                            request.getGeneratedActivity()
                                    .getActivityType()
                                    .name()
                    )
                    .questionText(question.getQuestionText())
                    .instructions(question.getInstruction())
                    .studentAnswer(answer.getStudentAnswer())
                    .correctAnswer(null)
                    .isCorrect(null)
                    .feedback(null)
                    .build();

            attemptQuestions.add(attemptQuestion);
        }

        attempt.setQuestions(attemptQuestions);

        studentActivityAttemptRepository.save(attempt);

        return ActivitySubmissionResponse.builder()
                .message("Activity submitted successfully.")
                .analysisStatus("PENDING")
                .activitySaved(true)
                .build();
    }

    @Override
    public List<ActivityHistoryResponse> getActivityHistory(Long studentId) {

        List<StudentActivityAttempt> attempts =
                studentActivityAttemptRepository
                        .findByStudentIdOrderBySubmittedAtDesc(studentId);

        return attempts.stream()
                .map(attempt -> ActivityHistoryResponse.builder()
                        .attemptId(attempt.getAttemptId())
                        .source(attempt.getSource())
                        .activityId(attempt.getActivityId())
                        .startedAt(attempt.getStartedAt())
                        .submittedAt(attempt.getSubmittedAt())
                        .status(attempt.getStatus())
                        .build())
                .toList();
    }

    @Override
    public ActivityHistoryDetailResponse getActivityHistoryById(
            Long studentId,
            Long attemptId) {

        StudentActivityAttempt attempt =
                studentActivityAttemptRepository
                        .findByAttemptIdAndStudentId(
                                attemptId,
                                studentId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Activity attempt not found"
                                ));

        List<AttemptQuestionResponse> questions =
                attempt.getQuestions()
                        .stream()
                        .map(question -> AttemptQuestionResponse.builder()
                                .attemptQuestionId(
                                        question.getAttemptQuestionId())
                                .questionType(
                                        question.getQuestionType())
                                .questionText(
                                        question.getQuestionText())
                                .instructions(
                                        question.getInstructions())
                                .studentAnswer(
                                        question.getStudentAnswer())
                                .correctAnswer(
                                        question.getCorrectAnswer())
                                .isCorrect(
                                        question.getIsCorrect())
                                .feedback(
                                        question.getFeedback())
                                .build())
                        .toList();

        return ActivityHistoryDetailResponse.builder()
                .attemptId(attempt.getAttemptId())
                .source(attempt.getSource())
                .activityId(attempt.getActivityId())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .status(attempt.getStatus())
                .questions(questions)
                .build();
    }

    @Override
    @Transactional
    public ActivityCreateResponse createActivity(
            ActivityCreateRequest request) {

        Activity activity = Activity.builder()
                .moduleId(request.moduleId())
                .gradeLevel(request.gradeLevel())
                .displayOrder(request.displayOrder())
                .title(request.title())
                .description(request.description())
                .activityType(request.activityType())
                .difficulty(request.difficulty())
                .timeLimitMinutes(request.timeLimitMinutes())
                .totalMarks(0)
                .status(ActivityStatus.DRAFT)
                .build();

        int totalMarks = 0;

        for (QuestionCreateRequest questionRequest : request.questions()) {

            Question question = Question.builder()
                    .activity(activity)
                    .questionText(questionRequest.questionText())
                    .instructions(questionRequest.instructions())
                    .imageUrl(questionRequest.imageUrl())
                    .points(questionRequest.points())
                    .displayOrder(questionRequest.displayOrder())
                    .build();

            for (OptionCreateRequest optionRequest :
                    questionRequest.options()) {

                Option option = Option.builder()
                        .question(question)
                        .optionText(optionRequest.optionText())
                        .correct(optionRequest.correct())
                        .displayOrder(optionRequest.displayOrder())
                        .build();

                question.getOptions().add(option);
            }

            activity.getQuestions().add(question);

            totalMarks += questionRequest.points();
        }

        activity.setTotalMarks(totalMarks);

        Activity savedActivity =
                activityRepository.save(activity);

        return new ActivityCreateResponse(
                savedActivity.getActivityId(),
                "Activity created successfully"
        );
    }

    @Override
    public List<ActivityListResponse> getAllActivities() {

        List<Activity> activities =
                activityRepository.findAll();

        return activities.stream()
                .map(activity -> new ActivityListResponse(
                        activity.getActivityId(),
                        activity.getModuleId(),
                        activity.getGradeLevel(),
                        activity.getDisplayOrder(),
                        activity.getTitle(),
                        activity.getDescription(),
                        activity.getActivityType(),
                        activity.getDifficulty(),
                        activity.getTimeLimitMinutes(),
                        activity.getTotalMarks(),
                        activity.getStatus()
                ))
                .toList();
    }

    @Override
    public ActivityDetailResponse getActivityById(Long activityId) {

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Activity not found with id: " + activityId
                        ));

        List<QuestionDetailResponse> questions =
                activity.getQuestions()
                        .stream()
                        .map(question -> {

                            List<OptionDetailResponse> options =
                                    question.getOptions()
                                            .stream()
                                            .map(option -> new OptionDetailResponse(
                                                    option.getOptionId(),
                                                    option.getOptionText(),
                                                    option.getCorrect(),
                                                    option.getDisplayOrder()
                                            ))
                                            .toList();

                            return new QuestionDetailResponse(
                                    question.getQuestionId(),
                                    question.getQuestionText(),
                                    question.getInstructions(),
                                    question.getImageUrl(),
                                    question.getPoints(),
                                    question.getDisplayOrder(),
                                    options
                            );
                        })
                        .toList();

        return new ActivityDetailResponse(
                activity.getActivityId(),
                activity.getModuleId(),
                activity.getGradeLevel(),
                activity.getDisplayOrder(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getActivityType(),
                activity.getDifficulty(),
                activity.getTimeLimitMinutes(),
                activity.getTotalMarks(),
                activity.getStatus(),
                questions
        );
    }

}