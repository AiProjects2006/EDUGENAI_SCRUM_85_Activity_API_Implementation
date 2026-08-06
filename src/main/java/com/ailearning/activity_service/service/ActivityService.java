package com.ailearning.activity_service.service;

import com.ailearning.activity_service.dto.request.ActivityGenerationRequest;
import com.ailearning.activity_service.dto.response.ActivityGenerationResponse;

public interface ActivityService {

    ActivityGenerationResponse generateActivity(ActivityGenerationRequest request);

}