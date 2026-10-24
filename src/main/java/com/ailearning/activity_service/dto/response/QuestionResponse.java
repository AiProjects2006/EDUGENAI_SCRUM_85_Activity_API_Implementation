package com.ailearning.activity_service.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionResponse {

    private String questionText;

    private String instruction;

    private Integer points;

    private Integer displayOrder;

    private List<OptionResponse> options;

    private String correctAnswer;

    private String activityType;

    private String icon;

    private List<String> categories;

    private List<Object> items;

    private List<String> itemsToOrder;

    private String imageUrl;

    private String passage;

    private Integer timeLimitSeconds;

    private String correctFeedback;

    private String incorrectFeedback;
}

