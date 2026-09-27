package com.ailearning.activity_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DropZoneCreateRequest(

        @NotBlank(message = "Drop zone label is required")
        String zoneLabel
) {
}