package com.ailearning.activity_service.service;

import com.ailearning.activity_service.dto.request.ActivityGenerationRequest;
import com.ailearning.activity_service.dto.request.ActivitySubmissionRequest;
import com.ailearning.activity_service.dto.response.ActivityGenerationResponse;
import com.ailearning.activity_service.dto.response.ActivityHistoryDetailResponse;
import com.ailearning.activity_service.dto.response.ActivityHistoryResponse;
import com.ailearning.activity_service.dto.response.ActivitySubmissionResponse;

import java.util.List;

public interface ActivityService {

    ActivityGenerationResponse generateActivity(ActivityGenerationRequest request);

    ActivitySubmissionResponse submitActivity(ActivitySubmissionRequest request);

    List<ActivityHistoryResponse> getActivityHistory(Long studentId);

    ActivityHistoryDetailResponse getActivityHistoryById(
            Long studentId,
            Long attemptId);

}