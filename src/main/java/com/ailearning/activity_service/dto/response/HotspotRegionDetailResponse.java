package com.ailearning.activity_service.dto.response;

public record HotspotRegionDetailResponse(
        Long regionId,
        Integer xCoordinate,
        Integer yCoordinate,
        Integer width,
        Integer height
) {}