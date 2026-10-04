package com.fxexposure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxexposure.config.SecurityConfig;
import com.fxexposure.dto.LoginRequestDto;
import com.fxexposure.dto.LoginResponseDto;
import com.fxexposure.dto.UserRequestDto;
import com.fxexposure.dto.UserResponseDto;
import com.fxexposure.entity.Role;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.exception.GlobalExceptionHandler;
import com.fxexposure.security.CustomUserDetailsService;
import com.fxexposure.security.JwtAuthenticationEntryPoint;
import com.fxexposure.security.JwtAuthenticationFilter;
import com.fxexposure.security.JwtService;
import com.fxexposure.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/auth/register - Should be publicly accessible without JWT and return 201")
    void register_PublicAccess_Success() throws Exception {
        UserRequestDto request = UserRequestDto.builder()
                .username("analyst_jane")
                .email("jane@fxexposure.com")
                .password("Secr3tPassword!")
                .role(Role.ANALYST)
                .build();

        UserResponseDto response = UserResponseDto.builder()
                .id(10L)
                .username("analyst_jane")
                .email("jane@fxexposure.com")
                .role(Role.ANALYST)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(authenticationService.register(any(UserRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.username").value("analyst_jane"))
                .andExpect(jsonPath("$.role").value("ANALYST"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 409 Conflict when duplicate username")
    void register_DuplicateUsername_Returns409() throws Exception {
        UserRequestDto request = UserRequestDto.builder()
                .username("existing_jane")
                .email("jane@fxexposure.com")
                .password("Secr3tPassword!")
                .role(Role.ANALYST)
                .build();

        when(authenticationService.register(any(UserRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Username is already taken: existing_jane"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username is already taken: existing_jane"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should be publicly accessible without JWT and return token")
    void login_PublicAccess_Success() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .username("analyst_jane")
                .password("Secr3tPassword!")
                .build();

        LoginResponseDto response = LoginResponseDto.builder()
                .token("mocked.jwt.token.here")
                .username("analyst_jane")
                .role(Role.ANALYST)
                .tokenType("Bearer")
                .build();

        when(authenticationService.login(any(LoginRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked.jwt.token.here"))
                .andExpect(jsonPath("$.username").value("analyst_jane"))
                .andExpect(jsonPath("$.role").value("ANALYST"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 401 Unauthorized on bad credentials")
    void login_BadCredentials_Returns401() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .username("analyst_jane")
                .password("WrongPassword")
                .build();

        when(authenticationService.login(any(LoginRequestDto.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}
