package com.openhour.backend.service;

import com.openhour.backend.exception.BadRequestException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {
    private final String adminUsername;
    private final String adminPassword;
    private final String adminToken;

    public AdminAuthService(
            @Value("${ADMIN_USERNAME:}") String adminUsername,
            @Value("${ADMIN_PASSWORD:}") String adminPassword,
            @Value("${ADMIN_TOKEN:}") String adminToken
    ) {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminToken = adminToken;
    }

    public String login(String username, String password) {
        requireConfigured();
        if (!Objects.equals(adminUsername, username) || !Objects.equals(adminPassword, password)) {
            throw new BadRequestException("Invalid owner credentials.");
        }
        return adminToken;
    }

    public void requireAdmin(String token) {
        requireConfigured();
        if (!Objects.equals(adminToken, token)) {
            throw new BadRequestException("Owner access is required.");
        }
    }

    private void requireConfigured() {
        if (adminUsername.isBlank() || adminPassword.isBlank() || adminToken.isBlank()) {
            throw new BadRequestException("Owner access is not configured.");
        }
    }
}
