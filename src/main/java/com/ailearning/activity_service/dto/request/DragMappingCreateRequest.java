package com.ailearning.activity_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DragMappingCreateRequest(

        @NotNull(message = "Drag item index is required")
        @Positive(message = "Drag item index must be positive")
        Integer dragItemIndex,

        @NotNull(message = "Drop zone index is required")
        @Positive(message = "Drop zone index must be positive")
        Integer dropZoneIndex
) {
}