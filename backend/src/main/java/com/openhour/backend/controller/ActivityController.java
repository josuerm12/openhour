package com.openhour.backend.controller;

import com.openhour.backend.dto.ActivityLogResponse;
import com.openhour.backend.service.AdminAuthService;
import com.openhour.backend.service.ActivityService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {
    private final ActivityService activityService;
    private final AdminAuthService adminAuthService;

    public ActivityController(ActivityService activityService, AdminAuthService adminAuthService) {
        this.activityService = activityService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping
    public List<ActivityLogResponse> latest(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        adminAuthService.requireAdmin(adminToken);
        return activityService.latest();
    }
}
