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
import com.ailearning.activity_service.service.RedisCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

        private final StudentActivityAttemptRepository studentActivityAttemptRepository;

        private final ActivityRepository activityRepository;

        private final RestTemplate restTemplate;

        private final RedisCacheService redisCacheService;

        private final ObjectMapper objectMapper;

        @Value("${ai.service.base-url:http://localhost:8000}")
        private String aiServiceBaseUrl;

        @Value("${course.service.base-url:http://localhost:8082}")
        private String courseServiceBaseUrl;

        @Value("${learning-resource.service.base-url:http://localhost:8085}")
        private String learningResourceServiceBaseUrl;

        @Value("${app.cache.activity.ttl-seconds:86400}")
        private long cacheTtlSeconds;


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

                        DragItem dragItem = question.getDragItems().get(dragItemIndex);

                        DropZone dropZone = question.getDropZones().get(dropZoneIndex);

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
                // 1. Resolve subject, course and topic from Course Service
                String subject = request.getSubject();
                String topic = request.getTopic();
                String moduleDescription = null;

                if (request.getModuleId() != null) {
                        try {
                                String moduleUrl = courseServiceBaseUrl + "/api/v1/modules/" + request.getModuleId();
                                ResponseEntity<Map> moduleResp = restTemplate.getForEntity(moduleUrl, Map.class);
                                if (moduleResp.getStatusCode().is2xxSuccessful() && moduleResp.getBody() != null) {
                                        Map body = moduleResp.getBody();
                                        Map data = body.containsKey("data") && body.get("data") instanceof Map
                                                        ? (Map) body.get("data")
                                                        : body;
                                        if (topic == null || topic.isBlank()) {
                                                topic = (String) data.get("title");
                                        }
                                        moduleDescription = (String) data.get("description");

                                        Object courseIdObj = data.get("courseId");
                                        if (courseIdObj != null && (subject == null || subject.isBlank())) {
                                                Long courseId = Long.valueOf(courseIdObj.toString());
                                                String courseUrl = courseServiceBaseUrl + "/api/v1/courses/" + courseId;
                                                ResponseEntity<Map> courseResp = restTemplate.getForEntity(courseUrl, Map.class);
                                                if (courseResp.getStatusCode().is2xxSuccessful() && courseResp.getBody() != null) {
                                                        Map cData = courseResp.getBody();
                                                        Map cBody = cData.containsKey("data") && cData.get("data") instanceof Map
                                                                        ? (Map) cData.get("data")
                                                                        : cData;
                                                        Object subjectIdObj = cBody.get("subjectId");
                                                        if (subjectIdObj != null) {
                                                                Long subjectId = Long.valueOf(subjectIdObj.toString());
                                                                String subjectUrl = courseServiceBaseUrl + "/api/v1/subjects/" + subjectId;
                                                                ResponseEntity<Map> subjectResp = restTemplate.getForEntity(subjectUrl, Map.class);
                                                                if (subjectResp.getStatusCode().is2xxSuccessful() && subjectResp.getBody() != null) {
                                                                        Map sData = subjectResp.getBody();
                                                                        Map sBody = sData.containsKey("data") && sData.get("data") instanceof Map
                                                                                        ? (Map) sData.get("data")
                                                                                        : sData;
                                                                        subject = (String) sBody.get("name");
                                                                }
                                                        }
                                                }
                                        }
                                }
                        } catch (Exception e) {
                                log.warn("Could not query Course Service for module {}: {}", request.getModuleId(), e.getMessage());
                        }
                }

                if (subject == null || subject.isBlank()) {
                        subject = "Science";
                }
                if (topic == null || topic.isBlank()) {
                        topic = "General Concepts";
                }

                // 2. Retrieve module content from Learning Resources Service
                StringBuilder lessonContentBuilder = new StringBuilder();
                if (moduleDescription != null && !moduleDescription.isBlank()) {
                        lessonContentBuilder.append("Module Overview: ").append(moduleDescription).append("\n\n");
                }

                if (request.getModuleId() != null) {
                        try {
                                String resUrl = learningResourceServiceBaseUrl + "/api/resources/module/" + request.getModuleId();
                                ResponseEntity<List> resResp = restTemplate.getForEntity(resUrl, List.class);
                                if (resResp.getStatusCode().is2xxSuccessful() && resResp.getBody() != null) {
                                        List<?> list = resResp.getBody();
                                        for (Object item : list) {
                                                if (item instanceof Map<?, ?> map) {
                                                        String rTitle = (String) map.get("title");
                                                        String rDesc = (String) map.get("description");
                                                        String rContent = (String) map.get("content");
                                                        if (rTitle != null && !rTitle.isBlank()) {
                                                                lessonContentBuilder.append("Slide: ").append(rTitle).append("\n");
                                                        }
                                                        if (rDesc != null && !rDesc.isBlank()) {
                                                                lessonContentBuilder.append("Description: ").append(rDesc).append("\n");
                                                        }
                                                        if (rContent != null && !rContent.isBlank()) {
                                                                lessonContentBuilder.append("Content: ").append(rContent).append("\n\n");
                                                        }
                                                }
                                        }
                                }
                        } catch (Exception e) {
                                log.warn("Could not query Learning Resource Service for module {}: {}", request.getModuleId(), e.getMessage());
                        }

                        // Also check AI Content chunks if available
                        try {
                                String aiContentUrl = learningResourceServiceBaseUrl + "/api/resources/ai/module/" + request.getModuleId();
                                ResponseEntity<Map> aiResResp = restTemplate.getForEntity(aiContentUrl, Map.class);
                                if (aiResResp.getStatusCode().is2xxSuccessful() && aiResResp.getBody() != null) {
                                        Map body = aiResResp.getBody();
                                        Object resources = body.get("resources");
                                        if (resources instanceof List<?> rList) {
                                                for (Object rObj : rList) {
                                                        if (rObj instanceof Map<?, ?> rMap) {
                                                                Object chunks = rMap.get("chunks");
                                                                if (chunks instanceof List<?> cList) {
                                                                        for (Object cObj : cList) {
                                                                                if (cObj instanceof Map<?, ?> cMap) {
                                                                                        String cText = (String) cMap.get("content");
                                                                                        if (cText != null && !cText.isBlank()) {
                                                                                                lessonContentBuilder.append(cText).append("\n");
                                                                                        }
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        } catch (Exception e) {
                                log.debug("AI chunks query note: {}", e.getMessage());
                        }
                }

                String lessonContent = lessonContentBuilder.toString().trim();

                ActivityType actType = request.getActivityType() != null ? request.getActivityType() : ActivityType.MCQ;
                int count = request.getQuestionCount() != null && request.getQuestionCount() > 0
                                ? request.getQuestionCount()
                                : 5;
                DifficultyLevel diffLevel = request.getDifficultyLevel() != null ? request.getDifficultyLevel()
                                : DifficultyLevel.MEDIUM;

                String aiActivityType = switch (actType) {
                        case MCQ -> "MCQ";
                        case QUIZ -> "Quiz";
                        case TIMED_QUIZ -> "TimedQuiz";
                        case TRUE_FALSE -> "TRUE_FALSE";
                        case FILL_BLANK -> "FillInTheBlanks";
                        case MATCHING -> "Matching";
                        case SORTING -> "Sorting";
                        case DRAG_DROP -> "DragDrop";
                        case POLL -> "Poll";
                        case SHORT_ANSWER -> "ShortAnswer";
                        case ESSAY -> "StructuredEssay";
                        case CHALLENGE_QUIZ -> "ChallengeQuiz";
                        case HOTSPOT -> "Hotspot";
                        case PROBLEM_SOLVING -> "ProblemSolving";
                        case APPLICATION_BASED -> "ApplicationBased";
                };

                // 3. Check Redis Cache
                String cacheKey = String.format("activity:gen:%s:%s:%s:%d:%s",
                                subject.trim().toLowerCase(),
                                topic.trim().toLowerCase(),
                                aiActivityType.toLowerCase(),
                                count,
                                diffLevel.name());

                String cachedJson = redisCacheService.get(cacheKey);
                if (cachedJson != null && !cachedJson.isBlank()) {
                        try {
                                log.info("Redis cache HIT for key: {}", cacheKey);
                                return objectMapper.readValue(cachedJson, ActivityGenerationResponse.class);
                        } catch (Exception e) {
                                log.warn("Failed to deserialize cached activity: {}", e.getMessage());
                        }
                }

                // 4. Call Multi-Agent Setup
                List<QuestionResponse> questions = new ArrayList<>();
                int totalMarks = 0;

                try {
                        Map<String, Object> aiPayload = new HashMap<>();
                        aiPayload.put("subject", subject);
                        aiPayload.put("topic", topic);
                        aiPayload.put("activityType", aiActivityType);
                        aiPayload.put("numberOfQuestions", count);
                        aiPayload.put("difficultyLevel", diffLevel.name());
                        if (!lessonContent.isBlank()) {
                                aiPayload.put("lessonContent", lessonContent);
                        }

                        String aiUrl = aiServiceBaseUrl + "/generate-activity";
                        log.info("Calling Multi-Agent Setup at {} for subject='{}', topic='{}', type='{}', hasLessonContent={}",
                                        aiUrl, subject, topic, aiActivityType, !lessonContent.isBlank());

                        ResponseEntity<Map> aiResp = restTemplate.postForEntity(aiUrl, aiPayload, Map.class);
                        if (aiResp.getStatusCode().is2xxSuccessful() && aiResp.getBody() != null) {
                                Map respBody = aiResp.getBody();
                                Object rawQuestions = respBody.get("questions");
                                if (rawQuestions instanceof List<?> qList) {
                                        int order = 1;
                                        for (Object qObj : qList) {
                                                if (qObj instanceof Map<?, ?> qMap) {
                                                        String qText = qMap.get("question") != null
                                                                        ? qMap.get("question").toString()
                                                                        : "Question " + order;

                                                        if (qMap.get("scenario") != null) {
                                                                qText = qMap.get("scenario").toString() + "\n\n" + qText;
                                                        }

                                                        String answer = null;
                                                        if (qMap.get("answer") != null) {
                                                                answer = qMap.get("answer").toString();
                                                        } else if (qMap.get("expectedAnswer") != null) {
                                                                answer = qMap.get("expectedAnswer").toString();
                                                        } else if (qMap.get("correctAnswer") != null) {
                                                                answer = qMap.get("correctAnswer").toString();
                                                        } else if (qMap.get("correct_answer") != null) {
                                                                answer = qMap.get("correct_answer").toString();
                                                        }

                                                        // Ensure FILL_BLANK has proper blank formatting
                                                        if (actType == ActivityType.FILL_BLANK) {
                                                                if (!qText.contains("______")) {
                                                                        if (qText.contains("___")) {
                                                                                qText = qText.replaceAll("_{3,}", "______");
                                                                        } else if (qText.contains("[blank]")) {
                                                                                qText = qText.replace("[blank]", "______");
                                                                        } else {
                                                                                qText = qText + " ______";
                                                                        }
                                                                }
                                                        }

                                                        List<OptionResponse> optionResponses = new ArrayList<>();
                                                        Object rawOptions = qMap.get("options");
                                                        if (rawOptions instanceof List<?> optList) {
                                                                int optOrder = 1;
                                                                for (Object opt : optList) {
                                                                        if (opt != null) {
                                                                                String optText = opt.toString();
                                                                                optionResponses.add(OptionResponse.builder()
                                                                                                .optionText(optText)
                                                                                                .displayOrder(optOrder++)
                                                                                                .correct(answer != null && answer.equalsIgnoreCase(optText))
                                                                                                .build());
                                                                        }
                                                                }
                                                        }

                                                        // Ensure True/False has True/False options
                                                        if (actType == ActivityType.TRUE_FALSE && optionResponses.isEmpty()) {
                                                                if (answer == null) answer = "True";
                                                                optionResponses.add(OptionResponse.builder().optionText("True").displayOrder(1).correct("True".equalsIgnoreCase(answer)).build());
                                                                optionResponses.add(OptionResponse.builder().optionText("False").displayOrder(2).correct("False".equalsIgnoreCase(answer)).build());
                                                        }

                                                        // Set default instruction based on activity type
                                                        String instruction = switch (actType) {
                                                                case TRUE_FALSE -> "Select True or False.";
                                                                case FILL_BLANK -> "Type the missing word in the blank.";
                                                                case SHORT_ANSWER -> "Type your concise answer below.";
                                                                case PROBLEM_SOLVING -> "Solve the exercise and provide your answer.";
                                                                case ESSAY -> "Write your comprehensive answer below.";
                                                                case CHALLENGE_QUIZ -> "Type your answer for this challenge.";
                                                                case APPLICATION_BASED -> "Apply your knowledge to choose the best response.";
                                                                case POLL -> "Select your opinion or choice.";
                                                                case TIMED_QUIZ -> "Select the correct answer before the time expires.";
                                                                default -> "Choose the correct answer.";
                                                        };

                                                        // Extract rich fields for interactive activity types
                                                        List<String> categories = null;
                                                        if (qMap.get("categories") instanceof List<?> catList) {
                                                                categories = new ArrayList<>();
                                                                for (Object c : catList) if (c != null) categories.add(c.toString());
                                                        }

                                                        List<Object> items = null;
                                                        if (qMap.get("items") instanceof List<?> itmList) {
                                                                items = new ArrayList<>(itmList);
                                                        }

                                                        // If Python returned correctGroups for category sorting, transform into items array
                                                        if (qMap.get("correctGroups") instanceof Map<?, ?> groups) {
                                                                if (categories == null) categories = new ArrayList<>();
                                                                if (items == null) items = new ArrayList<>();
                                                                for (Map.Entry<?, ?> entry : groups.entrySet()) {
                                                                        String catName = entry.getKey().toString();
                                                                        if (!categories.contains(catName)) categories.add(catName);
                                                                        if (entry.getValue() instanceof List<?> groupItems) {
                                                                                for (Object gItem : groupItems) {
                                                                                        if (gItem != null) {
                                                                                                Map<String, Object> itm = new HashMap<>();
                                                                                                itm.put("name", gItem.toString());
                                                                                                itm.put("category", catName);
                                                                                                itm.put("icon", "twemoji:sparkles");
                                                                                                items.add(itm);
                                                                                        }
                                                                                }
                                                                        }
                                                                }
                                                        }

                                                        List<String> itemsToOrder = null;
                                                        if (qMap.get("itemsToOrder") instanceof List<?> ordList) {
                                                                itemsToOrder = new ArrayList<>();
                                                                for (Object o : ordList) if (o != null) itemsToOrder.add(o.toString());
                                                        } else if (qMap.get("correctOrder") instanceof List<?> ordList) {
                                                                itemsToOrder = new ArrayList<>();
                                                                for (Object o : ordList) if (o != null) itemsToOrder.add(o.toString());
                                                        }

                                                        String imageUrl = qMap.get("imageUrl") != null ? qMap.get("imageUrl").toString() : null;
                                                        if (imageUrl == null && actType == ActivityType.HOTSPOT) {
                                                                imageUrl = "https://images.unsplash.com/photo-1530595467537-0b5996c41f2d?auto=format&fit=crop&w=600&q=80";
                                                        }

                                                        String passage = qMap.get("passage") != null ? qMap.get("passage").toString() : null;
                                                        Integer timeLimitSeconds = qMap.get("timeLimitSeconds") != null
                                                                        ? Integer.valueOf(qMap.get("timeLimitSeconds").toString())
                                                                        : (actType == ActivityType.TIMED_QUIZ ? 20 : null);

                                                        String correctFeedback = qMap.get("correctFeedback") != null
                                                                        ? qMap.get("correctFeedback").toString()
                                                                        : "Great job! That is correct! 🌟";

                                                        String incorrectFeedback = qMap.get("incorrectFeedback") != null
                                                                        ? qMap.get("incorrectFeedback").toString()
                                                                        : "Oops! Try again next time.";

                                                        String icon = qMap.get("icon") != null
                                                                        ? qMap.get("icon").toString()
                                                                        : switch (actType) {
                                                                                case TRUE_FALSE -> "twemoji:check-mark-button";
                                                                                case FILL_BLANK -> "twemoji:pencil";
                                                                                case SORTING -> "twemoji:card-file-box";
                                                                                case DRAG_DROP -> "twemoji:puzzle-piece";
                                                                                case HOTSPOT -> "twemoji:magnifying-glass-tilted-right";
                                                                                case ESSAY -> "twemoji:open-book";
                                                                                case SHORT_ANSWER -> "twemoji:thinking-face";
                                                                                case PROBLEM_SOLVING -> "twemoji:abacus";
                                                                                case CHALLENGE_QUIZ -> "twemoji:trophy";
                                                                                default -> "twemoji:sparkles";
                                                                        };

                                                        int points = 2;
                                                        totalMarks += points;

                                                        questions.add(QuestionResponse.builder()
                                                                        .questionText(qText)
                                                                        .instruction(instruction)
                                                                        .points(points)
                                                                        .displayOrder(order++)
                                                                        .correctAnswer(answer)
                                                                        .options(optionResponses)
                                                                        .activityType(actType.name())
                                                                        .categories(categories)
                                                                        .items(items)
                                                                        .itemsToOrder(itemsToOrder)
                                                                        .imageUrl(imageUrl)
                                                                        .passage(passage)
                                                                        .timeLimitSeconds(timeLimitSeconds)
                                                                        .icon(icon)
                                                                        .correctFeedback(correctFeedback)
                                                                        .incorrectFeedback(incorrectFeedback)
                                                                        .build());
                                                }
                                        }
                                }
                        }
                } catch (Exception e) {
                        log.warn("Multi-Agent Setup call failed ({}); using intelligent fallback generator.", e.getMessage());
                }

                // 5. Fallback if AI service returned no questions
                if (questions.isEmpty()) {
                        questions = createFallbackQuestions(topic, subject, actType, count);
                        totalMarks = questions.size() * 2;
                }

                ActivityGenerationResponse response = ActivityGenerationResponse.builder()
                                .title(topic + " " + (actType == ActivityType.TIMED_QUIZ ? "Timed Quiz" : "Activity"))
                                .description("Interactive learning activity for " + topic + " (" + subject + ")")
                                .activityType(actType)
                                .difficulty(diffLevel)
                                .gradeLevel(GradeLevel.GRADE_3)
                                .timeLimitMinutes(actType == ActivityType.TIMED_QUIZ ? 10 : null)
                                .totalMarks(totalMarks)
                                .questions(questions)
                                .build();

                // 6. Store in Redis Cache
                try {
                        redisCacheService.set(cacheKey, objectMapper.writeValueAsString(response), cacheTtlSeconds);
                } catch (Exception e) {
                        log.warn("Failed to write activity to Redis cache: {}", e.getMessage());
                }

                return response;
        }

        private List<QuestionResponse> createFallbackQuestions(String topic, String subject, ActivityType actType, int count) {
                List<QuestionResponse> list = new ArrayList<>();
                for (int i = 1; i <= count; i++) {
                        switch (actType) {
                                case TRUE_FALSE -> {
                                        boolean isTrue = (i % 2 != 0);
                                        list.add(QuestionResponse.builder()
                                                        .questionText(isTrue
                                                                        ? String.format("%s is a fundamental principle studied in %s.", topic, subject)
                                                                        : String.format("%s is entirely unrelated to the principles of %s.", topic, subject))
                                                        .instruction("Select True or False.")
                                                        .points(2)
                                                        .displayOrder(i)
                                                        .activityType("TRUE_FALSE")
                                                        .icon("twemoji:check-mark-button")
                                                        .correctAnswer(isTrue ? "True" : "False")
                                                        .options(List.of(
                                                                        OptionResponse.builder().optionText("True").displayOrder(1).correct(isTrue).build(),
                                                                        OptionResponse.builder().optionText("False").displayOrder(2).correct(!isTrue).build()))
                                                        .correctFeedback("Great job! That is correct! 🌟")
                                                        .incorrectFeedback("Oops! That statement is actually " + (isTrue ? "True" : "False") + ".")
                                                        .build());
                                }
                                case FILL_BLANK -> {
                                        String answer = "essential";
                                        list.add(QuestionResponse.builder()
                                                        .questionText(String.format("In %s, understanding the key mechanisms of %s is ______ for students.", subject, topic))
                                                        .instruction("Type the missing word in the blank.")
                                                        .points(2)
                                                        .displayOrder(i)
                                                        .activityType("FILL_BLANKS")
                                                        .icon("twemoji:pencil")
                                                        .correctAnswer(answer)
                                                        .options(List.of())
                                                        .correctFeedback("Well done! You filled the blank correctly! 🌟")
                                                        .incorrectFeedback("The correct word is \"" + answer + "\".")
                                                        .build());
                                }
                                case SHORT_ANSWER, CHALLENGE_QUIZ, PROBLEM_SOLVING -> {
                                        String answer = "Photosynthesis";
                                        list.add(QuestionResponse.builder()
                                                        .questionText(String.format("What is the primary process or concept associated with %s in %s (Concept %d)?", topic, subject, i))
                                                        .instruction("Type your concise answer below.")
                                                        .points(2)
                                                        .displayOrder(i)
                                                        .activityType(actType == ActivityType.CHALLENGE_QUIZ ? "CHALLENGE_QUIZ" : (actType == ActivityType.PROBLEM_SOLVING ? "PROBLEM_SOLVING" : "SHORT_ANSWER"))
                                                        .icon(actType == ActivityType.CHALLENGE_QUIZ ? "twemoji:trophy" : (actType == ActivityType.PROBLEM_SOLVING ? "twemoji:abacus" : "twemoji:thinking-face"))
                                                        .correctAnswer(answer)
                                                        .options(List.of())
                                                        .correctFeedback("Superb answer! 🌟")
                                                        .incorrectFeedback("Oops! The correct answer was \"" + answer + "\".")
                                                        .build());
                                }
                                case ESSAY -> {
                                        String passage = String.format("%s is a central topic in %s. It introduces key definitions, interrelated concepts, and real-world mechanisms that form the basis for scientific observation and problem solving.", topic, subject);
                                        list.add(QuestionResponse.builder()
                                                        .questionText(String.format("Why is %s significant in %s? Explain in 2-3 sentences based on the passage.", topic, subject))
                                                        .instruction("Write your comprehensive answer below.")
                                                        .points(2)
                                                        .displayOrder(i)
                                                        .activityType("STRUCTURED_ESSAY")
                                                        .icon("twemoji:open-book")
                                                        .passage(passage)
                                                        .correctAnswer("Any")
                                                        .options(List.of())
                                                        .correctFeedback("Wonderful essay response! 🌟")
                                                        .incorrectFeedback("Try adding more details from the passage.")
                                                        .build());
                                }
                                case APPLICATION_BASED -> {
                                        String coreAns = "Apply principle " + i + " to resolve the case";
                                        list.add(QuestionResponse.builder()
                                                        .questionText(String.format("Case Scenario %d: A researcher observes anomalies in %s. How should this scenario be approached?", i, topic))
                                                        .instruction("Apply your knowledge to choose the best response.")
                                                        .points(2)
                                                        .displayOrder(i)
                                                        .activityType("APPLICATION")
                                                        .icon("twemoji:light-bulb")
                                                        .correctAnswer(coreAns)
                                                        .options(List.of(
                                                                        OptionResponse.builder().optionText(coreAns).displayOrder(1).correct(true).build(),
                                                                        OptionResponse.builder().optionText("Disregard observations").displayOrder(2).correct(false).build(),
                                                                        OptionResponse.builder().optionText("Change the subject entirely").displayOrder(3).correct(false).build(),
                                                                        OptionResponse.builder().optionText("Invalidate all prior studies").displayOrder(4).correct(false).build()))
                                                        .correctFeedback("Spot on application! 🌟")
                                                        .incorrectFeedback("Oops! The best response was \"" + coreAns + "\".")
                                                        .build());
                                }
                                case POLL -> {
                                        list.add(QuestionResponse.builder()
                                                        .questionText(String.format("Which aspect of %s do you find most interesting to explore?", topic))
                                                        .instruction("Select your choice.")
                                                        .points(2)
                                                        .displayOrder(i)
                                                        .activityType("POLL")
                                                        .icon("twemoji:bar-chart")
                                                        .correctAnswer("Theoretical principles")
                                                        .options(List.of(
                                                                        OptionResponse.builder().optionText("Theoretical principles").displayOrder(1).correct(true).build(),
                                                                        OptionResponse.builder().optionText("Practical experiments").displayOrder(2).correct(false).build(),
                                                                        OptionResponse.builder().optionText("Historical discoveries").displayOrder(3).correct(false).build(),
                                                                        OptionResponse.builder().optionText("Future innovations").displayOrder(4).correct(false).build()))
                                                        .correctFeedback("Thanks for voting! 🌟")
                                                        .incorrectFeedback("Thanks for voting!")
                                                        .build());
                                }
                                case SORTING, MATCHING -> {
                                        String cat1 = "Primary " + topic;
                                        String cat2 = "Secondary Factors";
                                        List<String> categories = List.of(cat1, cat2);
                                        List<Object> items = List.of(
                                                        Map.of("name", "Key Concept " + i + "A", "category", cat1, "icon", "twemoji:star"),
                                                        Map.of("name", "Key Concept " + i + "B", "category", cat1, "icon", "twemoji:sparkles"),
                                                        Map.of("name", "External Factor " + i + "A", "category", cat2, "icon", "twemoji:deciduous-tree"),
                                                        Map.of("name", "External Factor " + i + "B", "category", cat2, "icon", "twemoji:cloud")
                                        );
                                        list.add(QuestionResponse.builder()
                                                        .questionText(String.format("Sort the following concepts into the correct categories for %s:", topic))
                                                        .instruction("Drag each item into its correct category box.")
                                                        .points(2)
                                                        .displayOrder(i)
                                                        .activityType(actType == ActivityType.MATCHING ? "MATCH_FOLLOWING" : "SORTING")
                                                        .icon("twemoji:card-file-box")
                                                        .categories(categories)
                                                        .items(items)
                                                        .correctFeedback("Incredible! All items sorted into the correct categories! 🌟")
                                                        .incorrectFeedback("Check your sorted items and try again.")
                                                        .build());
                                }
                                case DRAG_DROP -> {
                                        List<String> orderList = List.of(
                                                        "1. Introduction to " + topic,
                                                        "2. Identification of primary elements",
                                                        "3. Execution of the process",
                                                        "4. Evaluation of results"
                                        );
                                        list.add(QuestionResponse.builder()
                                                        .questionText(String.format("Arrange the stages of %s in the correct chronological order:", topic))
                                                        .instruction("Drag and arrange items from first to last.")
                                                        .points(2)
                                                        .displayOrder(i)
                                                        .activityType("DRAG_DROP")
                                                        .icon("twemoji:puzzle-piece")
                                                        .itemsToOrder(orderList)
                                                        .correctFeedback("Perfect order! You arranged all steps correctly! 🌟")
                                                        .incorrectFeedback("Try arranging the steps again.")
                                                        .build());
                                }
                                case HOTSPOT -> {
                                        list.add(QuestionResponse.builder()
                                                        .questionText(String.format("Click on the bottom zone of the diagram related to %s:", topic))
                                                        .instruction("Click directly on the target area of the diagram.")
                                                        .points(2)
                                                        .displayOrder(i)
                                                        .activityType("HOTSPOT")
                                                        .icon("twemoji:magnifying-glass-tilted-right")
                                                        .imageUrl("https://images.unsplash.com/photo-1530595467537-0b5996c41f2d?auto=format&fit=crop&w=600&q=80")
                                                        .correctFeedback("Spot on! Found the target zone! 🌟")
                                                        .incorrectFeedback("Missed the target area! Click closer to the base.")
                                                        .build());
                                }
                                case TIMED_QUIZ -> {
                                        String coreAnswer = "Core Principle of " + topic + " (#" + i + ")";
                                        list.add(QuestionResponse.builder()
                                                        .questionText(String.format("Fast Challenge: Which of the following is a primary characteristic of %s in %s?", topic, subject))
                                                        .instruction("Answer before the timer expires!")
                                                        .points(2)
                                                        .displayOrder(i)
                                                        .activityType("TIMED_QUIZ")
                                                        .timeLimitSeconds(20)
                                                        .icon("twemoji:stopwatch")
                                                        .correctAnswer(coreAnswer)
                                                        .options(List.of(
                                                                        OptionResponse.builder().optionText(coreAnswer).displayOrder(1).correct(true).build(),
                                                                        OptionResponse.builder().optionText("Secondary external factor").displayOrder(2).correct(false).build(),
                                                                        OptionResponse.builder().optionText("Opposing unrelated element").displayOrder(3).correct(false).build(),
                                                                        OptionResponse.builder().optionText("Irrelevant attribute").displayOrder(4).correct(false).build()))
                                                        .correctFeedback("Speedy and accurate! 🌟")
                                                        .incorrectFeedback("Time or answer missed! The correct answer was \"" + coreAnswer + "\".")
                                                        .build());
                                }
                                default -> {
                                        // MCQ, QUIZ
                                        String coreAnswer = "Core Principle of " + topic + " (#" + i + ")";
                                        list.add(QuestionResponse.builder()
                                                        .questionText(String.format("Which of the following is a primary characteristic of %s in %s?", topic, subject))
                                                        .instruction("Choose one answer.")
                                                        .points(2)
                                                        .displayOrder(i)
                                                        .activityType("MCQ")
                                                        .icon("twemoji:question-mark")
                                                        .correctAnswer(coreAnswer)
                                                        .options(List.of(
                                                                        OptionResponse.builder().optionText(coreAnswer).displayOrder(1).correct(true).build(),
                                                                        OptionResponse.builder().optionText("Secondary external factor").displayOrder(2).correct(false).build(),
                                                                        OptionResponse.builder().optionText("Opposing unrelated element").displayOrder(3).correct(false).build(),
                                                                        OptionResponse.builder().optionText("Irrelevant attribute").displayOrder(4).correct(false).build()))
                                                        .correctFeedback("Great job! That is correct! 🌟")
                                                        .incorrectFeedback("Oops! The correct answer was \"" + coreAnswer + "\".")
                                                        .build());
                                }
                        }
                }
                return list;
        }


        @Override
        public ActivitySubmissionResponse submitActivity(
                        ActivitySubmissionRequest request) {

                StudentActivityAttempt attempt = StudentActivityAttempt.builder()
                                .studentId(1L) // TODO: Replace with authenticated student ID
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
                                                                        .name())
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

                List<StudentActivityAttempt> attempts = studentActivityAttemptRepository
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

                StudentActivityAttempt attempt = studentActivityAttemptRepository
                                .findByAttemptIdAndStudentId(
                                                attemptId,
                                                studentId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Activity attempt not found"));

                List<AttemptQuestionResponse> questions = attempt.getQuestions()
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
                                        "TIMED_QUIZ requires timeLimitMinutes");
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

                evictActivityCaches(savedActivity.getModuleId(), savedActivity.getActivityId());

                return new ActivityCreateResponse(
                                savedActivity.getActivityId(),
                                "Activity created successfully");
        }

        @Override
        public List<ActivityListResponse> getAllActivities(Long moduleId) {
                String cacheKey = (moduleId != null) ? "activity:module:" + moduleId : "activity:all";
                String cached = redisCacheService.get(cacheKey);
                if (cached != null) {
                        try {
                                return objectMapper.readValue(cached, new com.fasterxml.jackson.core.type.TypeReference<List<ActivityListResponse>>() {});
                        } catch (Exception e) {
                                log.warn("Failed to deserialize cached activities for key {}: {}", cacheKey, e.getMessage());
                        }
                }

                List<Activity> activities = (moduleId != null)
                                ? activityRepository.findByModuleId(moduleId)
                                : activityRepository.findAll();

                List<ActivityListResponse> result = activities.stream()
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
                                                activity.getStatus()))
                                .toList();

                try {
                        redisCacheService.set(cacheKey, objectMapper.writeValueAsString(result), cacheTtlSeconds);
                } catch (Exception e) {
                        log.warn("Failed to serialize activities for cache key {}: {}", cacheKey, e.getMessage());
                }

                return result;
        }

        @Override
        public List<ActivityListResponse> getAllActivities() {
                return getAllActivities(null);
        }

        @Override
        @Transactional(readOnly = true)
        public ActivityDetailResponse getActivityById(Long activityId) {
                String cacheKey = "activity:id:" + activityId;
                String cached = redisCacheService.get(cacheKey);
                if (cached != null) {
                        try {
                                return objectMapper.readValue(cached, ActivityDetailResponse.class);
                        } catch (Exception e) {
                                log.warn("Failed to deserialize cached activity detail for key {}: {}", cacheKey, e.getMessage());
                        }
                }

                Activity activity = activityRepository.findById(activityId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Activity not found with id: " + activityId));

                List<QuestionDetailResponse> questions = activity.getQuestions().stream()
                                .map(question -> {

                                        // Options
                                        List<OptionDetailResponse> options = question.getOptions().stream()
                                                        .map(option -> new OptionDetailResponse(
                                                                        option.getOptionId(),
                                                                        option.getOptionText(),
                                                                        option.getCorrect(),
                                                                        option.getDisplayOrder()))
                                                        .toList();

                                        // Fill blanks
                                        List<FillBlankDetailResponse> fillBlanks = question.getFillBlanks().stream()
                                                        .map(blank -> new FillBlankDetailResponse(
                                                                        blank.getBlankId(),
                                                                        blank.getCorrectAnswer(),
                                                                        blank.getBlankIndex()))
                                                        .toList();

                                        // Matching pairs
                                        List<MatchingPairDetailResponse> matchingPairs = question.getMatchingPairs()
                                                        .stream()
                                                        .map(pair -> new MatchingPairDetailResponse(
                                                                        pair.getPairId(),
                                                                        pair.getLeftItem(),
                                                                        pair.getRightItem(),
                                                                        pair.getDisplayOrder()))
                                                        .toList();

                                        // Sorting items
                                        List<SortingItemDetailResponse> sortingItems = question.getSortingItems()
                                                        .stream()
                                                        .map(item -> new SortingItemDetailResponse(
                                                                        item.getItemId(),
                                                                        item.getItemText(),
                                                                        item.getCorrectOrder()))
                                                        .toList();

                                        // Drag items
                                        List<DragItemDetailResponse> dragItems = question.getDragItems().stream()
                                                        .map(item -> new DragItemDetailResponse(
                                                                        item.getItemId(),
                                                                        item.getItemText(),
                                                                        item.getImageUrl()))
                                                        .toList();

                                        // Drop zones
                                        List<DropZoneDetailResponse> dropZones = question.getDropZones().stream()
                                                        .map(zone -> new DropZoneDetailResponse(
                                                                        zone.getZoneId(),
                                                                        zone.getZoneLabel()))
                                                        .toList();

                                        // Drag mappings
                                        List<DragMappingDetailResponse> dragMappings = question.getDragItems().stream()
                                                        .flatMap(item -> item.getDragMappings().stream())
                                                        .map(mapping -> new DragMappingDetailResponse(
                                                                        mapping.getMappingId(),
                                                                        mapping.getDragItem().getItemId(),
                                                                        mapping.getDropZone().getZoneId()))
                                                        .toList();

                                        // Hotspot regions
                                        List<HotspotRegionDetailResponse> hotspotRegions = question.getHotspotRegions()
                                                        .stream()
                                                        .map(region -> new HotspotRegionDetailResponse(
                                                                        region.getRegionId(),
                                                                        region.getXCoordinate(),
                                                                        region.getYCoordinate(),
                                                                        region.getWidth(),
                                                                        region.getHeight()))
                                                        .toList();

                                        // Essay
                                        EssayDetailResponse essay = null;

                                        if (question.getEssay() != null) {
                                                essay = new EssayDetailResponse(
                                                                question.getEssay().getEssayId(),
                                                                question.getEssay().getMaxWordCount(),
                                                                question.getEssay().getMinWordCount(),
                                                                question.getEssay().getGradingRubric());
                                        }

                                        return new QuestionDetailResponse(
                                                        question.getQuestionId(),
                                                        question.getQuestionText(),
                                                        question.getInstructions(),
                                                        question.getImageUrl(),
                                                        question.getPoints(),
                                                        question.getDisplayOrder(),
                                                        options,
                                                        fillBlanks,
                                                        matchingPairs,
                                                        sortingItems,
                                                        dragItems,
                                                        dropZones,
                                                        dragMappings,
                                                        hotspotRegions,
                                                        essay);
                                })
                                .toList();

                ActivityDetailResponse response = new ActivityDetailResponse(
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
                                questions);

                try {
                        redisCacheService.set(cacheKey, objectMapper.writeValueAsString(response), cacheTtlSeconds);
                } catch (Exception e) {
                        log.warn("Failed to serialize activity detail for cache key {}: {}", cacheKey, e.getMessage());
                }

                return response;
        }

        @Override
        @Transactional
        public ActivityDetailResponse updateActivity(
                        Long activityId,
                        ActivityUpdateRequest request) {

                Activity activity = activityRepository.findById(activityId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Activity not found with id: " + activityId));

                // Update activity fields
                activity.setModuleId(request.moduleId());
                activity.setGradeLevel(request.gradeLevel());
                activity.setDisplayOrder(request.displayOrder());
                activity.setTitle(request.title());
                activity.setDescription(request.description());
                activity.setActivityType(request.activityType());
                activity.setDifficulty(request.difficulty());
                activity.setTimeLimitMinutes(request.timeLimitMinutes());
                activity.setStatus(request.status());

                // Validate time limit for timed quizzes
                if (request.activityType() == ActivityType.TIMED_QUIZ
                                && request.timeLimitMinutes() == null) {
                        throw new IllegalArgumentException(
                                        "Time limit is required for TIMED_QUIZ");
                }

                // Remove existing questions and their child data
                activity.getQuestions().clear();

                int totalMarks = 0;

                for (QuestionCreateRequest questionRequest : request.questions()) {

                        // Validate question structure
                        validateQuestionData(
                                        questionRequest,
                                        request.activityType());

                        Question question = Question.builder()
                                        .activity(activity)
                                        .questionText(questionRequest.questionText())
                                        .instructions(questionRequest.instructions())
                                        .imageUrl(questionRequest.imageUrl())
                                        .points(questionRequest.points())
                                        .displayOrder(questionRequest.displayOrder())
                                        .build();

                        // Options
                        for (OptionCreateRequest optionRequest : questionRequest.options()) {

                                Option option = Option.builder()
                                                .question(question)
                                                .optionText(optionRequest.optionText())
                                                .correct(optionRequest.correct())
                                                .displayOrder(optionRequest.displayOrder())
                                                .build();

                                question.getOptions().add(option);
                        }

                        // Fill blanks
                        for (FillBlankCreateRequest blankRequest : questionRequest.fillBlanks()) {

                                FillBlank fillBlank = FillBlank.builder()
                                                .question(question)
                                                .correctAnswer(blankRequest.correctAnswer())
                                                .blankIndex(blankRequest.blankIndex())
                                                .build();

                                question.getFillBlanks().add(fillBlank);
                        }

                        // Matching pairs
                        for (MatchingPairCreateRequest pairRequest : questionRequest.matchingPairs()) {

                                MatchingPair pair = MatchingPair.builder()
                                                .question(question)
                                                .leftItem(pairRequest.leftItem())
                                                .rightItem(pairRequest.rightItem())
                                                .displayOrder(pairRequest.displayOrder())
                                                .build();

                                question.getMatchingPairs().add(pair);
                        }

                        // Sorting items
                        for (SortingItemCreateRequest itemRequest : questionRequest.sortingItems()) {

                                SortingItem item = SortingItem.builder()
                                                .question(question)
                                                .itemText(itemRequest.itemText())
                                                .correctOrder(itemRequest.correctOrder())
                                                .build();

                                question.getSortingItems().add(item);
                        }

                        // Drag items
                        for (DragItemCreateRequest itemRequest : questionRequest.dragItems()) {

                                DragItem item = DragItem.builder()
                                                .question(question)
                                                .itemText(itemRequest.itemText())
                                                .imageUrl(itemRequest.imageUrl())
                                                .build();

                                question.getDragItems().add(item);
                        }

                        // Drop zones
                        for (DropZoneCreateRequest zoneRequest : questionRequest.dropZones()) {

                                DropZone zone = DropZone.builder()
                                                .question(question)
                                                .zoneLabel(zoneRequest.zoneLabel())
                                                .build();

                                question.getDropZones().add(zone);
                        }

                        // Drag mappings
                        for (DragMappingCreateRequest mappingRequest : questionRequest.dragMappings()) {

                                int dragItemIndex = mappingRequest.dragItemIndex();
                                int dropZoneIndex = mappingRequest.dropZoneIndex();

                                if (dragItemIndex < 1 ||
                                                dragItemIndex > question.getDragItems().size()) {
                                        throw new IllegalArgumentException(
                                                        "Invalid drag item index: " + dragItemIndex);
                                }

                                if (dropZoneIndex < 1 ||
                                                dropZoneIndex > question.getDropZones().size()) {
                                        throw new IllegalArgumentException(
                                                        "Invalid drop zone index: " + dropZoneIndex);
                                }

                                DragItem dragItem = question.getDragItems().get(dragItemIndex - 1);

                                DropZone dropZone = question.getDropZones().get(dropZoneIndex - 1);

                                DragMapping mapping = DragMapping.builder()
                                                .dragItem(dragItem)
                                                .dropZone(dropZone)
                                                .build();

                                dragItem.getDragMappings().add(mapping);
                                dropZone.getDragMappings().add(mapping);
                        }

                        // Hotspot regions
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

                        // Essay
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

                        activity.getQuestions().add(question);

                        totalMarks += questionRequest.points();
                }

                activity.setTotalMarks(totalMarks);

                Activity savedActivity = activityRepository.save(activity);

                evictActivityCaches(savedActivity.getModuleId(), savedActivity.getActivityId());

                return getActivityById(savedActivity.getActivityId());
        }

        private void validateQuestionData(
                        QuestionCreateRequest request,
                        ActivityType activityType) {

                boolean hasOptions = request.options() != null &&
                                !request.options().isEmpty();

                boolean hasFillBlanks = request.fillBlanks() != null &&
                                !request.fillBlanks().isEmpty();

                boolean hasMatchingPairs = request.matchingPairs() != null &&
                                !request.matchingPairs().isEmpty();

                boolean hasSortingItems = request.sortingItems() != null &&
                                !request.sortingItems().isEmpty();

                boolean hasDragItems = request.dragItems() != null &&
                                !request.dragItems().isEmpty();

                boolean hasDropZones = request.dropZones() != null &&
                                !request.dropZones().isEmpty();

                boolean hasDragMappings = request.dragMappings() != null &&
                                !request.dragMappings().isEmpty();

                boolean hasHotspotRegions = request.hotspotRegions() != null &&
                                !request.hotspotRegions().isEmpty();

                boolean hasEssay = request.essay() != null;

                // -----------------------------------------
                // DRAG & DROP STRUCTURE VALIDATION
                // -----------------------------------------

                if (hasDragItems || hasDropZones || hasDragMappings) {

                        if (!hasDragItems || !hasDropZones || !hasDragMappings) {
                                throw new IllegalArgumentException(
                                                "DRAG_DROP requires dragItems, dropZones and dragMappings");
                        }

                        if (request.dragMappings().size() != request.dragItems().size()) {
                                throw new IllegalArgumentException(
                                                "Each drag item must have exactly one mapping");
                        }
                }

                // -----------------------------------------
                // ONLY ONE ANSWER STRUCTURE
                // -----------------------------------------

                int structureCount = 0;

                if (hasOptions)
                        structureCount++;
                if (hasFillBlanks)
                        structureCount++;
                if (hasMatchingPairs)
                        structureCount++;
                if (hasSortingItems)
                        structureCount++;
                if (hasDragItems)
                        structureCount++;
                if (hasHotspotRegions)
                        structureCount++;
                if (hasEssay)
                        structureCount++;

                if (structureCount == 0) {
                        throw new IllegalArgumentException(
                                        "Question must contain at least one answer structure");
                }

                if (structureCount > 1) {
                        throw new IllegalArgumentException(
                                        "Question cannot contain multiple answer structures");
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
                                                        "FILL_BLANK activity requires fillBlanks");
                                }
                        }

                        // -------------------------------------
                        // MATCHING
                        // -------------------------------------

                        case MATCHING -> {

                                if (!hasMatchingPairs) {
                                        throw new IllegalArgumentException(
                                                        "MATCHING activity requires matchingPairs");
                                }

                                if (request.matchingPairs().size() < 2) {
                                        throw new IllegalArgumentException(
                                                        "MATCHING requires at least two pairs");
                                }
                        }

                        // -------------------------------------
                        // SORTING
                        // -------------------------------------

                        case SORTING -> {

                                if (!hasSortingItems) {
                                        throw new IllegalArgumentException(
                                                        "SORTING activity requires sortingItems");
                                }

                                if (request.sortingItems().size() < 2) {
                                        throw new IllegalArgumentException(
                                                        "SORTING requires at least two items");
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
                                                        "DRAG_DROP activity requires dragItems, dropZones and dragMappings");
                                }

                                validateDragMappings(request);
                        }

                        // -------------------------------------
                        // HOTSPOT
                        // -------------------------------------

                        case HOTSPOT -> {

                                if (!hasHotspotRegions) {
                                        throw new IllegalArgumentException(
                                                        "HOTSPOT activity requires hotspotRegions");
                                }
                        }

                        // -------------------------------------
                        // ESSAY / SHORT ANSWER
                        // -------------------------------------

                        case ESSAY, SHORT_ANSWER -> {

                                if (!hasEssay) {
                                        throw new IllegalArgumentException(
                                                        activityType + " activity requires essay data");
                                }

                                validateEssay(request.essay());
                        }

                        // -------------------------------------
                        // TRUE / FALSE
                        // -------------------------------------

                        case TRUE_FALSE -> {

                                if (!hasOptions) {
                                        throw new IllegalArgumentException(
                                                        "TRUE_FALSE activity requires options");
                                }

                                if (request.options().size() != 2) {
                                        throw new IllegalArgumentException(
                                                        "TRUE_FALSE requires exactly two options");
                                }
                        }

                        // -------------------------------------
                        // MCQ / QUIZ
                        // -------------------------------------

                        case MCQ, QUIZ, TIMED_QUIZ, CHALLENGE_QUIZ -> {

                                if (!hasOptions) {
                                        throw new IllegalArgumentException(
                                                        activityType + " activity requires options");
                                }

                                if (request.options().size() < 2) {
                                        throw new IllegalArgumentException(
                                                        activityType + " requires at least two options");
                                }

                                long correctCount = request.options()
                                                .stream()
                                                .filter(OptionCreateRequest::correct)
                                                .count();

                                if (correctCount == 0) {
                                        throw new IllegalArgumentException(
                                                        activityType + " requires at least one correct option");
                                }
                        }

                        // -------------------------------------
                        // POLL
                        // -------------------------------------

                        case POLL -> {

                                if (!hasOptions) {
                                        throw new IllegalArgumentException(
                                                        "POLL activity requires options");
                                }

                                if (request.options().size() < 2) {
                                        throw new IllegalArgumentException(
                                                        "POLL requires at least two options");
                                }
                        }

                        // -------------------------------------
                        // PROBLEM SOLVING / APPLICATION
                        // -------------------------------------

                        case PROBLEM_SOLVING, APPLICATION_BASED -> {

                                if (!hasOptions && !hasEssay) {
                                        throw new IllegalArgumentException(
                                                        activityType + " requires options or essay data");
                                }

                                if (hasOptions && request.options().size() < 2) {
                                        throw new IllegalArgumentException(
                                                        activityType + " requires at least two options");
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
                                                                + expectedSize);
                        }
                }
        }

        private void validateDragMappings(QuestionCreateRequest request) {

                int dragItemCount = request.dragItems().size();
                int dropZoneCount = request.dropZones().size();

                java.util.Set<Integer> dragIndexes = new java.util.HashSet<>();
                java.util.Set<Integer> dropZoneIndexes = new java.util.HashSet<>();

                for (DragMappingCreateRequest mapping : request.dragMappings()) {

                        int dragIndex = mapping.dragItemIndex();
                        int dropZoneIndex = mapping.dropZoneIndex();

                        if (dragIndex < 1 || dragIndex > dragItemCount) {
                                throw new IllegalArgumentException(
                                                "Invalid drag item index: " + dragIndex);
                        }

                        if (dropZoneIndex < 1 || dropZoneIndex > dropZoneCount) {
                                throw new IllegalArgumentException(
                                                "Invalid drop zone index: " + dropZoneIndex);
                        }

                        if (!dragIndexes.add(dragIndex)) {
                                throw new IllegalArgumentException(
                                                "A drag item cannot have multiple mappings");
                        }

                        dropZoneIndexes.add(dropZoneIndex);
                }
        }

        private void validateEssay(EssayCreateRequest essay) {

                if (essay.minWordCount() != null &&
                                essay.maxWordCount() != null &&
                                essay.minWordCount() > essay.maxWordCount()) {

                        throw new IllegalArgumentException(
                                        "Minimum word count cannot be greater than maximum word count");
                }
        }

        @Override
        @Transactional
        public void deleteActivity(Long activityId) {

                Activity activity = activityRepository.findById(activityId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Activity not found with id: " + activityId));

                Long moduleId = activity.getModuleId();
                activityRepository.delete(activity);
                evictActivityCaches(moduleId, activityId);
        }

        private void evictActivityCaches(Long moduleId, Long activityId) {
                redisCacheService.delete("activity:all");
                if (activityId != null) {
                        redisCacheService.delete("activity:id:" + activityId);
                }
                if (moduleId != null) {
                        redisCacheService.delete("activity:module:" + moduleId);
                }
                redisCacheService.deletePattern("activity:module:*");
        }

}