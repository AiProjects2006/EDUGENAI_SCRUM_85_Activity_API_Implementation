package com.ailearning.activity_service.controller;

import com.ailearning.activity_service.dto.request.ActivityCreateRequest;
import com.ailearning.activity_service.dto.request.ActivityGenerationRequest;
import com.ailearning.activity_service.dto.request.ActivitySubmissionRequest;
import com.ailearning.activity_service.dto.response.*;
import com.ailearning.activity_service.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/submit")
    public ResponseEntity<ActivitySubmissionResponse> submitActivity(
            @RequestBody ActivitySubmissionRequest request) {

        ActivitySubmissionResponse response =
                activityService.submitActivity(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ActivityCreateResponse> createActivity(
            @Valid @RequestBody ActivityCreateRequest request) {

        ActivityCreateResponse response =
                activityService.createActivity(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ActivityHistoryResponse>> getActivityHistory() {

        Long studentId = 1L; // temporary until authentication is connected

        return ResponseEntity.ok(
                activityService.getActivityHistory(studentId)
        );
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<ActivityHistoryDetailResponse> getActivityHistoryById(
            @PathVariable Long id) {

        Long studentId = 1L; // temporary until authentication is connected

        return ResponseEntity.ok(
                activityService.getActivityHistoryById(
                        studentId,
                        id
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<ActivityListResponse>> getAllActivities() {

        return ResponseEntity.ok(
                activityService.getAllActivities()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityDetailResponse> getActivityById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                activityService.getActivityById(id)
        );
    }
}