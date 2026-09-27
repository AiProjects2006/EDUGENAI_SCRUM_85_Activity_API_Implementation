package com.ailearning.activity_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DragItemCreateRequest(

        @NotBlank(message = "Drag item text is required")
        String itemText,

        String imageUrl
) {
}