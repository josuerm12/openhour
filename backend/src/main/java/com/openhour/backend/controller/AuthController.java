package com.openhour.backend.controller;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final String adminUsername;
    private final String adminPassword;

    public AuthController(
            @Value("${ADMIN_USERNAME:owner}") String adminUsername,
            @Value("${ADMIN_PASSWORD:openhour}") String adminPassword
    ) {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @PostMapping("/login")
    public Map<String, Boolean> login(@RequestBody LoginRequest request) {
        boolean authenticated = adminUsername.equals(request.username()) && adminPassword.equals(request.password());
        return Map.of("authenticated", authenticated);
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }
}
