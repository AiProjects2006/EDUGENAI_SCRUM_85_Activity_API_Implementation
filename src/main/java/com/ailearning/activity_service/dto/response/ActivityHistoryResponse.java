package com.ailearning.activity_service.dto.response;

import com.ailearning.activity_service.enums.ActivitySource;
import com.ailearning.activity_service.enums.AttemptStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityHistoryResponse {

    private Long attemptId;

    private ActivitySource source;

    private Long activityId;

    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    private AttemptStatus status;
}