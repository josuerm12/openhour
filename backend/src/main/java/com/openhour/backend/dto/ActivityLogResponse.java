package com.openhour.backend.dto;

import com.openhour.backend.model.ActivityLog;
import java.time.Instant;

public record ActivityLogResponse(Long id, String message, Instant createdAt) {
    public static ActivityLogResponse from(ActivityLog log) {
        return new ActivityLogResponse(log.getId(), log.getMessage(), log.getCreatedAt());
    }
}
