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
}
