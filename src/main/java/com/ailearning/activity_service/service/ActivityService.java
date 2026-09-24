package com.ailearning.activity_service.service;

import com.ailearning.activity_service.dto.request.ActivityCreateRequest;
import com.ailearning.activity_service.dto.request.ActivityGenerationRequest;
import com.ailearning.activity_service.dto.request.ActivitySubmissionRequest;
import com.ailearning.activity_service.dto.response.ActivityCreateResponse;
import com.ailearning.activity_service.dto.response.*;

import java.util.List;

public interface ActivityService {

    ActivityGenerationResponse generateActivity(ActivityGenerationRequest request);

    ActivitySubmissionResponse submitActivity(ActivitySubmissionRequest request);

    List<ActivityHistoryResponse> getActivityHistory(Long studentId);

    ActivityHistoryDetailResponse getActivityHistoryById(
            Long studentId,
            Long attemptId);

    ActivityCreateResponse createActivity(ActivityCreateRequest request);

    List<ActivityListResponse> getAllActivities();

}