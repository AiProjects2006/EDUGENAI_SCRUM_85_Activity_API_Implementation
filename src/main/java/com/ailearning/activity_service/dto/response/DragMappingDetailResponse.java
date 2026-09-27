package com.ailearning.activity_service.dto.response;

public record DragMappingDetailResponse(
        Long mappingId,
        Long dragItemId,
        Long dropZoneId
) {}