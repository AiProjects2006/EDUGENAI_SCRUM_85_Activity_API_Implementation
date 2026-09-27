package com.ailearning.activity_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record QuestionCreateRequest(

        @NotBlank(message = "Question text is required")
        String questionText,

        String instructions,

        String imageUrl,

        @NotNull(message = "Points are required")
        @Positive(message = "Points must be positive")
        Integer points,

        @NotNull(message = "Display order is required")
        @Positive(message = "Display order must be positive")
        Integer displayOrder,

        @Valid
        List<OptionCreateRequest> options,

        @Valid
        List<FillBlankCreateRequest> fillBlanks,

        @Valid
        List<MatchingPairCreateRequest> matchingPairs,

        @Valid
        List<SortingItemCreateRequest> sortingItems,

        @Valid
        List<DragItemCreateRequest> dragItems,

        @Valid
        List<DropZoneCreateRequest> dropZones,

        @Valid
        List<DragMappingCreateRequest> dragMappings,

        @Valid
        List<HotspotRegionCreateRequest> hotspotRegions,

        @Valid
        EssayCreateRequest essay
) {
}