
package com.ailearning.activity_service.dto.request;

import com.ailearning.activity_service.dto.response.ActivityGenerationResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivitySubmissionRequest {

    private Long activityId;

    private ActivityGenerationResponse generatedActivity;

    private List<AnswerRequest> answers;
}
