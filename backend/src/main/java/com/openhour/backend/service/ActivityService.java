package com.openhour.backend.service;

import com.openhour.backend.dto.ActivityLogResponse;
import com.openhour.backend.model.ActivityLog;
import com.openhour.backend.repository.ActivityLogRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {
    private final ActivityLogRepository activityLogRepository;

    public ActivityService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void record(String message) {
        activityLogRepository.save(new ActivityLog(message));
    }

    public List<ActivityLogResponse> latest() {
        return activityLogRepository.findTop30ByOrderByCreatedAtDesc().stream()
                .map(ActivityLogResponse::from)
                .toList();
    }
}
