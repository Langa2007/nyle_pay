package com.nyle.nylepay.controllers;

import com.nyle.nylepay.models.User;
import com.nyle.nylepay.services.UserService;
import com.nyle.nylepay.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("controller@nylepay.com");
        testUser.setFullName("Controller User");
    }

    @Test
    void testGetUserProfile_Success() {
        when(userService.getUserById(anyLong())).thenReturn(Optional.of(testUser));

        ResponseEntity<ApiResponse<java.util.Map<String, Object>>> response = userController.getUserProfile(1L);
        
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
    }
}
