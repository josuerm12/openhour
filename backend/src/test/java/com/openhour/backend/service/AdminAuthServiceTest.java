package com.openhour.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.openhour.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class AdminAuthServiceTest {
    @Test
    void loginRejectsBadCredentials() {
        AdminAuthService adminAuthService = new AdminAuthService("owner", "secret", "admin-token");

        assertThatThrownBy(() -> adminAuthService.login("owner", "wrong"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid owner credentials");
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        AdminAuthService adminAuthService = new AdminAuthService("owner", "secret", "admin-token");

        String token = adminAuthService.login("owner", "secret");

        assertThat(token).isEqualTo("admin-token");
    }
}
