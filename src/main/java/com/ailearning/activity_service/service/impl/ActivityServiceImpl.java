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
import com.ailearning.activity_service.enums.*;
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

    private void createDragMappings(
            Question question,
            QuestionCreateRequest request) {

        if (request.dragMappings() == null) {
            return;
        }

        for (DragMappingCreateRequest mappingRequest : request.dragMappings()) {

            int dragItemIndex = mappingRequest.dragItemIndex() - 1;
            int dropZoneIndex = mappingRequest.dropZoneIndex() - 1;

            if (dragItemIndex < 0 ||
                    dragItemIndex >= question.getDragItems().size()) {

                throw new IllegalArgumentException(
                        "Invalid drag item index: "
                                + mappingRequest.dragItemIndex());
            }

            if (dropZoneIndex < 0 ||
                    dropZoneIndex >= question.getDropZones().size()) {

                throw new IllegalArgumentException(
                        "Invalid drop zone index: "
                                + mappingRequest.dropZoneIndex());
            }

            DragItem dragItem =
                    question.getDragItems().get(dragItemIndex);

            DropZone dropZone =
                    question.getDropZones().get(dropZoneIndex);

            DragMapping mapping = DragMapping.builder()
                    .dragItem(dragItem)
                    .dropZone(dropZone)
                    .build();

            dragItem.getDragMappings().add(mapping);
            dropZone.getDragMappings().add(mapping);
        }
    }

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
    public ActivityCreateResponse createActivity(ActivityCreateRequest request) {

        if (request.activityType() == ActivityType.TIMED_QUIZ
                && request.timeLimitMinutes() == null) {

            throw new IllegalArgumentException(
                    "TIMED_QUIZ requires timeLimitMinutes"
            );
        }

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

            validateQuestionData(questionRequest, request.activityType());

            Question question = Question.builder()
                    .activity(activity)
                    .questionText(questionRequest.questionText())
                    .instructions(questionRequest.instructions())
                    .imageUrl(questionRequest.imageUrl())
                    .points(questionRequest.points())
                    .displayOrder(questionRequest.displayOrder())
                    .build();

            // 1. Options
            if (questionRequest.options() != null) {

                for (OptionCreateRequest optionRequest : questionRequest.options()) {

                    Option option = Option.builder()
                            .question(question)
                            .optionText(optionRequest.optionText())
                            .correct(optionRequest.correct())
                            .displayOrder(optionRequest.displayOrder())
                            .build();

                    question.getOptions().add(option);
                }
            }

            // 2. Fill blanks
            if (questionRequest.fillBlanks() != null) {

                for (FillBlankCreateRequest blankRequest : questionRequest.fillBlanks()) {

                    FillBlank fillBlank = FillBlank.builder()
                            .question(question)
                            .correctAnswer(blankRequest.correctAnswer())
                            .blankIndex(blankRequest.blankIndex())
                            .build();

                    question.getFillBlanks().add(fillBlank);
                }
            }

            // 3. Matching pairs
            if (questionRequest.matchingPairs() != null) {

                for (MatchingPairCreateRequest pairRequest : questionRequest.matchingPairs()) {

                    MatchingPair matchingPair = MatchingPair.builder()
                            .question(question)
                            .leftItem(pairRequest.leftItem())
                            .rightItem(pairRequest.rightItem())
                            .displayOrder(pairRequest.displayOrder())
                            .build();

                    question.getMatchingPairs().add(matchingPair);
                }
            }

            // 4. Sorting items
            if (questionRequest.sortingItems() != null) {

                for (SortingItemCreateRequest itemRequest : questionRequest.sortingItems()) {

                    SortingItem sortingItem = SortingItem.builder()
                            .question(question)
                            .itemText(itemRequest.itemText())
                            .correctOrder(itemRequest.correctOrder())
                            .build();

                    question.getSortingItems().add(sortingItem);
                }
            }

            // 5. Drag items
            if (questionRequest.dragItems() != null) {

                for (DragItemCreateRequest itemRequest : questionRequest.dragItems()) {

                    DragItem dragItem = DragItem.builder()
                            .question(question)
                            .itemText(itemRequest.itemText())
                            .imageUrl(itemRequest.imageUrl())
                            .build();

                    question.getDragItems().add(dragItem);
                }
            }

            // 6. Drop zones
            if (questionRequest.dropZones() != null) {

                for (DropZoneCreateRequest zoneRequest : questionRequest.dropZones()) {

                    DropZone dropZone = DropZone.builder()
                            .question(question)
                            .zoneLabel(zoneRequest.zoneLabel())
                            .build();

                    question.getDropZones().add(dropZone);
                }
            }

            // 7. Hotspot regions
            if (questionRequest.hotspotRegions() != null) {

                for (HotspotRegionCreateRequest regionRequest : questionRequest.hotspotRegions()) {

                    HotspotRegion region = HotspotRegion.builder()
                            .question(question)
                            .xCoordinate(regionRequest.xCoordinate())
                            .yCoordinate(regionRequest.yCoordinate())
                            .width(regionRequest.width())
                            .height(regionRequest.height())
                            .build();

                    question.getHotspotRegions().add(region);
                }
            }

            // 8. Essay
            if (questionRequest.essay() != null) {

                EssayCreateRequest essayRequest = questionRequest.essay();

                Essay essay = Essay.builder()
                        .question(question)
                        .maxWordCount(essayRequest.maxWordCount())
                        .minWordCount(essayRequest.minWordCount())
                        .gradingRubric(essayRequest.gradingRubric())
                        .build();

                question.setEssay(essay);
            }

            // 9. Drag mappings
            createDragMappings(question, questionRequest);

            activity.getQuestions().add(question);

            totalMarks += questionRequest.points();
        }

        activity.setTotalMarks(totalMarks);

        Activity savedActivity = activityRepository.save(activity);

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

    private void validateQuestionData(
            QuestionCreateRequest request,
            ActivityType activityType) {

        boolean hasOptions =
                request.options() != null &&
                        !request.options().isEmpty();

        boolean hasFillBlanks =
                request.fillBlanks() != null &&
                        !request.fillBlanks().isEmpty();

        boolean hasMatchingPairs =
                request.matchingPairs() != null &&
                        !request.matchingPairs().isEmpty();

        boolean hasSortingItems =
                request.sortingItems() != null &&
                        !request.sortingItems().isEmpty();

        boolean hasDragItems =
                request.dragItems() != null &&
                        !request.dragItems().isEmpty();

        boolean hasDropZones =
                request.dropZones() != null &&
                        !request.dropZones().isEmpty();

        boolean hasDragMappings =
                request.dragMappings() != null &&
                        !request.dragMappings().isEmpty();

        boolean hasHotspotRegions =
                request.hotspotRegions() != null &&
                        !request.hotspotRegions().isEmpty();

        boolean hasEssay =
                request.essay() != null;

        // -----------------------------------------
        // DRAG & DROP STRUCTURE VALIDATION
        // -----------------------------------------

        if (hasDragItems || hasDropZones || hasDragMappings) {

            if (!hasDragItems || !hasDropZones || !hasDragMappings) {
                throw new IllegalArgumentException(
                        "DRAG_DROP requires dragItems, dropZones and dragMappings"
                );
            }

            if (request.dragMappings().size() != request.dragItems().size()) {
                throw new IllegalArgumentException(
                        "Each drag item must have exactly one mapping"
                );
            }
        }

        // -----------------------------------------
        // ONLY ONE ANSWER STRUCTURE
        // -----------------------------------------

        int structureCount = 0;

        if (hasOptions) structureCount++;
        if (hasFillBlanks) structureCount++;
        if (hasMatchingPairs) structureCount++;
        if (hasSortingItems) structureCount++;
        if (hasDragItems) structureCount++;
        if (hasHotspotRegions) structureCount++;
        if (hasEssay) structureCount++;

        if (structureCount == 0) {
            throw new IllegalArgumentException(
                    "Question must contain at least one answer structure"
            );
        }

        if (structureCount > 1) {
            throw new IllegalArgumentException(
                    "Question cannot contain multiple answer structures"
            );
        }

        // -----------------------------------------
        // ACTIVITY TYPE VALIDATION
        // -----------------------------------------

        switch (activityType) {

            // -------------------------------------
            // FILL BLANK
            // -------------------------------------

            case FILL_BLANK -> {

                if (!hasFillBlanks) {
                    throw new IllegalArgumentException(
                            "FILL_BLANK activity requires fillBlanks"
                    );
                }
            }

            // -------------------------------------
            // MATCHING
            // -------------------------------------

            case MATCHING -> {

                if (!hasMatchingPairs) {
                    throw new IllegalArgumentException(
                            "MATCHING activity requires matchingPairs"
                    );
                }

                if (request.matchingPairs().size() < 2) {
                    throw new IllegalArgumentException(
                            "MATCHING requires at least two pairs"
                    );
                }
            }

            // -------------------------------------
            // SORTING
            // -------------------------------------

            case SORTING -> {

                if (!hasSortingItems) {
                    throw new IllegalArgumentException(
                            "SORTING activity requires sortingItems"
                    );
                }

                if (request.sortingItems().size() < 2) {
                    throw new IllegalArgumentException(
                            "SORTING requires at least two items"
                    );
                }

                validateSortingItems(request);
            }

            // -------------------------------------
            // DRAG & DROP
            // -------------------------------------

            case DRAG_DROP -> {

                if (!hasDragItems ||
                        !hasDropZones ||
                        !hasDragMappings) {

                    throw new IllegalArgumentException(
                            "DRAG_DROP activity requires dragItems, dropZones and dragMappings"
                    );
                }

                validateDragMappings(request);
            }

            // -------------------------------------
            // HOTSPOT
            // -------------------------------------

            case HOTSPOT -> {

                if (!hasHotspotRegions) {
                    throw new IllegalArgumentException(
                            "HOTSPOT activity requires hotspotRegions"
                    );
                }
            }

            // -------------------------------------
            // ESSAY / SHORT ANSWER
            // -------------------------------------

            case ESSAY, SHORT_ANSWER -> {

                if (!hasEssay) {
                    throw new IllegalArgumentException(
                            activityType + " activity requires essay data"
                    );
                }

                validateEssay(request.essay());
            }

            // -------------------------------------
            // TRUE / FALSE
            // -------------------------------------

            case TRUE_FALSE -> {

                if (!hasOptions) {
                    throw new IllegalArgumentException(
                            "TRUE_FALSE activity requires options"
                    );
                }

                if (request.options().size() != 2) {
                    throw new IllegalArgumentException(
                            "TRUE_FALSE requires exactly two options"
                    );
                }
            }

            // -------------------------------------
            // MCQ / QUIZ
            // -------------------------------------

            case MCQ, QUIZ, TIMED_QUIZ, CHALLENGE_QUIZ -> {

                if (!hasOptions) {
                    throw new IllegalArgumentException(
                            activityType + " activity requires options"
                    );
                }

                if (request.options().size() < 2) {
                    throw new IllegalArgumentException(
                            activityType + " requires at least two options"
                    );
                }

                long correctCount = request.options()
                        .stream()
                        .filter(OptionCreateRequest::correct)
                        .count();

                if (correctCount == 0) {
                    throw new IllegalArgumentException(
                            activityType + " requires at least one correct option"
                    );
                }
            }

            // -------------------------------------
            // POLL
            // -------------------------------------

            case POLL -> {

                if (!hasOptions) {
                    throw new IllegalArgumentException(
                            "POLL activity requires options"
                    );
                }

                if (request.options().size() < 2) {
                    throw new IllegalArgumentException(
                            "POLL requires at least two options"
                    );
                }
            }

            // -------------------------------------
            // PROBLEM SOLVING / APPLICATION
            // -------------------------------------

            case PROBLEM_SOLVING, APPLICATION_BASED -> {

                if (!hasOptions && !hasEssay) {
                    throw new IllegalArgumentException(
                            activityType + " requires options or essay data"
                    );
                }

                if (hasOptions && request.options().size() < 2) {
                    throw new IllegalArgumentException(
                            activityType + " requires at least two options"
                    );
                }

                if (hasEssay) {
                    validateEssay(request.essay());
                }
            }
        }
    }

    private void validateSortingItems(QuestionCreateRequest request) {

        List<Integer> orders = request.sortingItems()
                .stream()
                .map(SortingItemCreateRequest::correctOrder)
                .toList();

        int expectedSize = orders.size();

        for (int i = 1; i <= expectedSize; i++) {

            if (!orders.contains(i)) {
                throw new IllegalArgumentException(
                        "SORTING correctOrder must contain every value from 1 to "
                                + expectedSize
                );
            }
        }
    }

    private void validateDragMappings(QuestionCreateRequest request) {

        int dragItemCount = request.dragItems().size();
        int dropZoneCount = request.dropZones().size();

        java.util.Set<Integer> dragIndexes = new java.util.HashSet<>();
        java.util.Set<Integer> dropZoneIndexes = new java.util.HashSet<>();

        for (DragMappingCreateRequest mapping :
                request.dragMappings()) {

            int dragIndex = mapping.dragItemIndex();
            int dropZoneIndex = mapping.dropZoneIndex();

            if (dragIndex < 1 || dragIndex > dragItemCount) {
                throw new IllegalArgumentException(
                        "Invalid drag item index: " + dragIndex
                );
            }

            if (dropZoneIndex < 1 || dropZoneIndex > dropZoneCount) {
                throw new IllegalArgumentException(
                        "Invalid drop zone index: " + dropZoneIndex
                );
            }

            if (!dragIndexes.add(dragIndex)) {
                throw new IllegalArgumentException(
                        "A drag item cannot have multiple mappings"
                );
            }

            dropZoneIndexes.add(dropZoneIndex);
        }
    }

    private void validateEssay(EssayCreateRequest essay) {

        if (essay.minWordCount() != null &&
                essay.maxWordCount() != null &&
                essay.minWordCount() > essay.maxWordCount()) {

            throw new IllegalArgumentException(
                    "Minimum word count cannot be greater than maximum word count"
            );
        }
    }

}