package com.ailearning.activity_service.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerRequest {

    private Integer questionNumber;

    private String studentAnswer;
}