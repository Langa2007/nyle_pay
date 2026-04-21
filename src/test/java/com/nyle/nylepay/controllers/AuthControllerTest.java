package com.nyle.nylepay.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void register_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        Map<String, String> loginRequest = Map.of(
            "email", "nobody@test.com",
            "password", "wrongpass"
        );

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_invalidToken_returns400or401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh-token")
                .param("refreshToken", "invalid-token"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void logout_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void forgotPassword_nonExistentEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                .param("email", "nonexistent@test.com"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void adminMetrics_withAdminRole_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/metrics")
                .with(user("admin@nylepay.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalUsers").exists());
    }

    @Test
    void adminMetrics_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/api/admin/metrics"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void adminMetrics_withUserRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/metrics")
                .with(user("user@nylepay.com").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminUsers_withAdminRole_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .with(user("admin@nylepay.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void adminTransactions_withAdminRole_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/transactions")
                .with(user("admin@nylepay.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray());
    }
}
