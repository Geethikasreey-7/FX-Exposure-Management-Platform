package com.fxexposure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxexposure.config.SecurityConfig;
import com.fxexposure.dto.UserRequestDto;
import com.fxexposure.dto.UserResponseDto;
import com.fxexposure.entity.Role;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.exception.GlobalExceptionHandler;
import com.fxexposure.exception.ResourceNotFoundException;
import com.fxexposure.security.CustomUserDetailsService;
import com.fxexposure.security.JwtAuthenticationEntryPoint;
import com.fxexposure.security.JwtAuthenticationFilter;
import com.fxexposure.security.JwtService;
import com.fxexposure.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@WithMockUser(username = "admin_user", roles = {"ADMIN"})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/users - Should return list of users")
    void getAllUsers_ReturnsList() throws Exception {
        UserResponseDto userDto = UserResponseDto.builder()
                .id(1L)
                .username("analyst_user")
                .email("analyst@fxexposure.com")
                .role(Role.ANALYST)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.getAllUsers()).thenReturn(List.of(userDto));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].username").value("analyst_user"))
                .andExpect(jsonPath("$[0].email").value("analyst@fxexposure.com"))
                .andExpect(jsonPath("$[0].role").value("ANALYST"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/users/{id} - Should return user by ID")
    void getUserById_ReturnsUser() throws Exception {
        UserResponseDto userDto = UserResponseDto.builder()
                .id(1L)
                .username("manager_user")
                .email("manager@fxexposure.com")
                .role(Role.MANAGER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.getUserById(1L)).thenReturn(userDto);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("manager_user"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Should return 404 when user not found")
    void getUserById_NotFound_Returns404() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    @Test
    @DisplayName("POST /api/users - Should create user successfully and return 201")
    void createUser_ValidPayload_Returns201() throws Exception {
        UserRequestDto request = UserRequestDto.builder()
                .username("new_admin")
                .email("admin@fxexposure.com")
                .password("supersecret123")
                .role(Role.ADMIN)
                .build();

        UserResponseDto response = UserResponseDto.builder()
                .id(2L)
                .username("new_admin")
                .email("admin@fxexposure.com")
                .role(Role.ADMIN)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.createUser(any(UserRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.username").value("new_admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/users - Should return 400 when validation fails")
    void createUser_InvalidPayload_Returns400() throws Exception {
        UserRequestDto invalidRequest = UserRequestDto.builder()
                .username("")
                .email("not-an-email")
                .password("123")
                .role(null)
                .build();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("POST /api/users - Should return 409 when duplicate username/email")
    void createUser_Duplicate_Returns409() throws Exception {
        UserRequestDto request = UserRequestDto.builder()
                .username("existing_user")
                .email("existing@fxexposure.com")
                .password("password123")
                .role(Role.ANALYST)
                .build();

        when(userService.createUser(any(UserRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Username is already taken: existing_user"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username is already taken: existing_user"));
    }

    @Test
    @DisplayName("PUT /api/users/{id} - Should update user and return 200")
    void updateUser_Success_Returns200() throws Exception {
        UserRequestDto request = UserRequestDto.builder()
                .username("updated_name")
                .email("updated@fxexposure.com")
                .password("newpassword123")
                .role(Role.MANAGER)
                .build();

        UserResponseDto response = UserResponseDto.builder()
                .id(1L)
                .username("updated_name")
                .email("updated@fxexposure.com")
                .role(Role.MANAGER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.updateUser(eq(1L), any(UserRequestDto.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updated_name"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Should return 204 No Content")
    void deleteUser_Success_Returns204() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }
}
