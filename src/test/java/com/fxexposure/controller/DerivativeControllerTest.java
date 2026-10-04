package com.fxexposure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxexposure.config.SecurityConfig;
import com.fxexposure.dto.DerivativeRequestDto;
import com.fxexposure.dto.DerivativeResponseDto;
import com.fxexposure.entity.DerivativeStatus;
import com.fxexposure.entity.DerivativeType;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.exception.GlobalExceptionHandler;
import com.fxexposure.exception.ResourceNotFoundException;
import com.fxexposure.security.CustomUserDetailsService;
import com.fxexposure.security.JwtAuthenticationEntryPoint;
import com.fxexposure.security.JwtAuthenticationFilter;
import com.fxexposure.security.JwtService;
import com.fxexposure.service.DerivativeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DerivativeController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class DerivativeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DerivativeService derivativeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private DerivativeResponseDto createSampleResponseDto() {
        return DerivativeResponseDto.builder()
                .id(1L)
                .derivativeReference("FWD-USD-0001")
                .derivativeType(DerivativeType.FORWARD)
                .exposureId(1L)
                .exposureReference("EXP-USD-0001")
                .baseCurrency("USD")
                .quoteCurrency("INR")
                .notionalAmount(new BigDecimal("1000000.00"))
                .tradeDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2026, 12, 31))
                .spotRate(new BigDecimal("83.500000"))
                .forwardRate(new BigDecimal("84.100000"))
                .status(DerivativeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private DerivativeRequestDto createSampleRequestDto() {
        return DerivativeRequestDto.builder()
                .derivativeReference("FWD-USD-0001")
                .derivativeType(DerivativeType.FORWARD)
                .exposureId(1L)
                .baseCurrency("USD")
                .quoteCurrency("INR")
                .notionalAmount(new BigDecimal("1000000.00"))
                .tradeDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2026, 12, 31))
                .spotRate(new BigDecimal("83.500000"))
                .forwardRate(new BigDecimal("84.100000"))
                .status(DerivativeStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("1. GET /api/derivatives - Should return 401 when request lacks JWT")
    void getAllDerivatives_WithoutJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/derivatives"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("2. GET /api/derivatives - Should return 200 with list of derivatives when authorized with valid JWT")
    void getAllDerivatives_WithValidJwt_Returns200() throws Exception {
        String token = "valid.jwt.token";
        UserDetails userDetails = new User("analyst", "password", Collections.emptyList());

        when(jwtService.extractUsername(token)).thenReturn("analyst");
        when(userDetailsService.loadUserByUsername("analyst")).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
        when(derivativeService.getAllDerivatives(null, null, null, null, null))
                .thenReturn(List.of(createSampleResponseDto()));

        mockMvc.perform(get("/api/derivatives")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].derivativeReference").value("FWD-USD-0001"))
                .andExpect(jsonPath("$[0].notionalAmount").value(1000000.00));
    }

    @Test
    @WithMockUser
    @DisplayName("3. POST /api/derivatives - Should create derivative and return 201 Created")
    void createDerivative_Success_Returns201() throws Exception {
        DerivativeRequestDto request = createSampleRequestDto();
        DerivativeResponseDto response = createSampleResponseDto();

        when(derivativeService.createDerivative(any(DerivativeRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/derivatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.derivativeReference").value("FWD-USD-0001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser
    @DisplayName("4. POST /api/derivatives - Should return 400 Bad Request on validation failure")
    void createDerivative_ValidationFailure_Returns400() throws Exception {
        DerivativeRequestDto invalidRequest = DerivativeRequestDto.builder()
                .derivativeReference("") // blank
                .derivativeType(null)    // null
                .exposureId(null)        // null
                .baseCurrency("US")      // not 3 chars
                .quoteCurrency("")       // blank
                .notionalAmount(new BigDecimal("-500.00")) // negative
                .tradeDate(LocalDate.of(2026, 12, 31))
                .maturityDate(LocalDate.of(2026, 10, 1)) // before tradeDate
                .build();

        mockMvc.perform(post("/api/derivatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("5. POST /api/derivatives - Should return 409 Conflict on duplicate reference")
    void createDerivative_DuplicateReference_Returns409() throws Exception {
        DerivativeRequestDto request = createSampleRequestDto();

        when(derivativeService.createDerivative(any(DerivativeRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Derivative reference already exists: FWD-USD-0001"));

        mockMvc.perform(post("/api/derivatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Derivative reference already exists: FWD-USD-0001"));
    }

    @Test
    @WithMockUser
    @DisplayName("6. POST /api/derivatives - Should return 404 Not Found when Exposure missing")
    void createDerivative_ExposureNotFound_Returns404() throws Exception {
        DerivativeRequestDto request = createSampleRequestDto();

        when(derivativeService.createDerivative(any(DerivativeRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Exposure not found with id: 1"));

        mockMvc.perform(post("/api/derivatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Exposure not found with id: 1"));
    }

    @Test
    @WithMockUser
    @DisplayName("7. GET /api/derivatives/{id} - Should return derivative details by ID")
    void getDerivativeById_Success_Returns200() throws Exception {
        when(derivativeService.getDerivativeById(1L)).thenReturn(createSampleResponseDto());

        mockMvc.perform(get("/api/derivatives/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.derivativeReference").value("FWD-USD-0001"));
    }

    @Test
    @WithMockUser
    @DisplayName("8. GET /api/derivatives/{id} - Should return 404 Not Found when ID does not exist")
    void getDerivativeById_NotFound_Returns404() throws Exception {
        when(derivativeService.getDerivativeById(99L))
                .thenThrow(new ResourceNotFoundException("Derivative not found with id: 99"));

        mockMvc.perform(get("/api/derivatives/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Derivative not found with id: 99"));
    }

    @Test
    @WithMockUser
    @DisplayName("9. PUT /api/derivatives/{id} - Should update derivative and return 200 OK")
    void updateDerivative_Success_Returns200() throws Exception {
        DerivativeRequestDto request = createSampleRequestDto();
        DerivativeResponseDto response = createSampleResponseDto();
        response.setDescription("Updated contract description");

        when(derivativeService.updateDerivative(eq(1L), any(DerivativeRequestDto.class))).thenReturn(response);

        mockMvc.perform(put("/api/derivatives/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated contract description"));
    }

    @Test
    @WithMockUser
    @DisplayName("10. DELETE /api/derivatives/{id} - Should delete derivative and return 204 No Content")
    void deleteDerivative_Success_Returns204() throws Exception {
        doNothing().when(derivativeService).deleteDerivative(1L);

        mockMvc.perform(delete("/api/derivatives/1"))
                .andExpect(status().isNoContent());

        verify(derivativeService).deleteDerivative(1L);
    }
}

