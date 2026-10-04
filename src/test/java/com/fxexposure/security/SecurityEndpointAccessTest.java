package com.fxexposure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxexposure.config.SecurityConfig;
import com.fxexposure.controller.AuthenticationController;
import com.fxexposure.controller.UserController;
import com.fxexposure.dto.LoginRequestDto;
import com.fxexposure.dto.LoginResponseDto;
import com.fxexposure.dto.UserRequestDto;
import com.fxexposure.dto.UserResponseDto;
import com.fxexposure.entity.Role;
import com.fxexposure.exception.GlobalExceptionHandler;
import com.fxexposure.service.AuthenticationService;
import com.fxexposure.service.ExposureService;
import com.fxexposure.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthenticationController.class, UserController.class})
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class SecurityEndpointAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private UserService userService;

    @MockBean
    private ExposureService exposureService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Public endpoint /api/auth/register should be accessible without JWT")
    void registerEndpoint_AccessibleWithoutJwt() throws Exception {
        UserRequestDto request = UserRequestDto.builder()
                .username("new_trader")
                .email("trader@fxexposure.com")
                .password("Password123!")
                .role(Role.ANALYST)
                .build();

        UserResponseDto response = UserResponseDto.builder()
                .id(1L)
                .username("new_trader")
                .email("trader@fxexposure.com")
                .role(Role.ANALYST)
                .enabled(true)
                .build();

        when(authenticationService.register(any(UserRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Public endpoint /api/auth/login should be accessible without JWT")
    void loginEndpoint_AccessibleWithoutJwt() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .username("new_trader")
                .password("Password123!")
                .build();

        LoginResponseDto response = LoginResponseDto.builder()
                .token("mocked.jwt.token")
                .username("new_trader")
                .role(Role.ANALYST)
                .tokenType("Bearer")
                .build();

        when(authenticationService.login(any(LoginRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protected endpoint /api/users should return 401 when JWT is missing")
    void usersEndpoint_MissingJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Protected endpoint /api/users should return 401 when JWT is invalid")
    void usersEndpoint_InvalidJwt_Returns401() throws Exception {
        when(jwtService.extractUsername("invalid.jwt.token")).thenThrow(new io.jsonwebtoken.MalformedJwtException("Invalid token"));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Protected endpoint /api/users should succeed (200) with valid JWT")
    void usersEndpoint_ValidJwt_Returns200() throws Exception {
        String validToken = "valid.jwt.token";
        UserDetails userDetails = new User(
                "auth_user",
                "password",
                Collections.emptyList()
        );

        when(jwtService.extractUsername(validToken)).thenReturn("auth_user");
        when(userDetailsService.loadUserByUsername("auth_user")).thenReturn(userDetails);
        when(jwtService.isTokenValid(validToken, userDetails)).thenReturn(true);
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }
}
