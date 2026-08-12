package com.ailearning.activity_service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptQuestionResponse {

    private Long attemptQuestionId;

    private String questionType;

    private String questionText;

    private String instructions;

    private String studentAnswer;

    private String correctAnswer;

    private Boolean isCorrect;

    private String feedback;
}
