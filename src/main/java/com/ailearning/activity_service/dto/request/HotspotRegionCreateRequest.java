package com.ailearning.activity_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record HotspotRegionCreateRequest(

        @NotNull(message = "X coordinate is required")
        @PositiveOrZero(message = "X coordinate cannot be negative")
        Integer xCoordinate,

        @NotNull(message = "Y coordinate is required")
        @PositiveOrZero(message = "Y coordinate cannot be negative")
        Integer yCoordinate,

        @NotNull(message = "Width is required")
        @Positive(message = "Width must be positive")
        Integer width,

        @NotNull(message = "Height is required")
        @Positive(message = "Height must be positive")
        Integer height
) {
}