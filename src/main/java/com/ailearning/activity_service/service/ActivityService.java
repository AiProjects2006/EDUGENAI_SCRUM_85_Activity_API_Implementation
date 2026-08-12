package com.ailearning.activity_service.service;

import com.ailearning.activity_service.dto.request.ActivityGenerationRequest;
import com.ailearning.activity_service.dto.request.ActivitySubmissionRequest;
import com.ailearning.activity_service.dto.response.ActivityGenerationResponse;
import com.ailearning.activity_service.dto.response.ActivitySubmissionResponse;

public interface ActivityService {

    ActivityGenerationResponse generateActivity(ActivityGenerationRequest request);
    ActivitySubmissionResponse submitActivity(ActivitySubmissionRequest request);

}