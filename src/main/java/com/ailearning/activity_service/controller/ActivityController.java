package com.ailearning.activity_service.controller;

import com.ailearning.activity_service.dto.request.ActivityGenerationRequest;
import com.ailearning.activity_service.dto.response.ActivityGenerationResponse;
import com.ailearning.activity_service.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping("/generate")
    public ResponseEntity<ActivityGenerationResponse> generateActivity(
            @RequestBody ActivityGenerationRequest request) {

        ActivityGenerationResponse response =
                activityService.generateActivity(request);

        return ResponseEntity.ok(response);
    }
}