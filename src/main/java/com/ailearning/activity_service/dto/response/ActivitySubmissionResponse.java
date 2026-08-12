package com.ailearning.activity_service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivitySubmissionResponse {

    private String message;

    private String analysisStatus;

    private Boolean activitySaved;
}