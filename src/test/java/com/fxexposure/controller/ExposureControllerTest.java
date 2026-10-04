package com.fxexposure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxexposure.config.SecurityConfig;
import com.fxexposure.dto.ExposureRequestDto;
import com.fxexposure.dto.ExposureResponseDto;
import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.entity.ExposureType;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.exception.GlobalExceptionHandler;
import com.fxexposure.exception.ResourceNotFoundException;
import com.fxexposure.security.CustomUserDetailsService;
import com.fxexposure.security.JwtAuthenticationEntryPoint;
import com.fxexposure.security.JwtAuthenticationFilter;
import com.fxexposure.security.JwtService;
import com.fxexposure.service.ExposureService;
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

@WebMvcTest(ExposureController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class ExposureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExposureService exposureService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private ExposureResponseDto createSampleResponseDto() {
        return ExposureResponseDto.builder()
                .id(1L)
                .exposureReference("EXP-USD-0001")
                .exposureType(ExposureType.RECEIVABLE)
                .currency("USD")
                .baseCurrency("INR")
                .amount(new BigDecimal("5000000.00"))
                .exposureDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2027, 1, 2))
                .description("USD receivable from overseas customer")
                .status(ExposureStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ExposureRequestDto createSampleRequestDto() {
        return ExposureRequestDto.builder()
                .exposureReference("EXP-USD-0001")
                .exposureType(ExposureType.RECEIVABLE)
                .currency("USD")
                .baseCurrency("INR")
                .amount(new BigDecimal("5000000.00"))
                .exposureDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2027, 1, 2))
                .description("USD receivable from overseas customer")
                .status(ExposureStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("GET /api/exposures - Should return 401 when request lacks JWT")
    void getAllExposures_WithoutJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/exposures"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/exposures - Should return 200 with list of exposures when authorized with valid JWT")
    void getAllExposures_WithValidJwt_Returns200() throws Exception {
        String token = "valid.jwt.token";
        UserDetails userDetails = new User("analyst", "password", Collections.emptyList());

        when(jwtService.extractUsername(token)).thenReturn("analyst");
        when(userDetailsService.loadUserByUsername("analyst")).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
        when(exposureService.getAllExposures(null, null, null)).thenReturn(List.of(createSampleResponseDto()));

        mockMvc.perform(get("/api/exposures")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].exposureReference").value("EXP-USD-0001"))
                .andExpect(jsonPath("$[0].amount").value(5000000.00));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/exposures - Should create exposure and return 201 Created")
    void createExposure_Success_Returns201() throws Exception {
        ExposureRequestDto request = createSampleRequestDto();
        ExposureResponseDto response = createSampleResponseDto();

        when(exposureService.createExposure(any(ExposureRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/exposures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.exposureReference").value("EXP-USD-0001"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/exposures - Should return 400 Bad Request on validation failure")
    void createExposure_ValidationFailure_Returns400() throws Exception {
        ExposureRequestDto invalidRequest = ExposureRequestDto.builder()
                .exposureReference("") // blank
                .exposureType(null)    // missing
                .currency("US")        // not 3 chars
                .baseCurrency("")      // blank
                .amount(new BigDecimal("-100.00")) // negative
                .exposureDate(LocalDate.of(2027, 1, 2))
                .maturityDate(LocalDate.of(2026, 1, 1)) // before exposureDate
                .build();

        mockMvc.perform(post("/api/exposures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/exposures - Should return 409 Conflict on duplicate reference")
    void createExposure_DuplicateReference_Returns409() throws Exception {
        ExposureRequestDto request = createSampleRequestDto();

        when(exposureService.createExposure(any(ExposureRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Exposure reference already exists: EXP-USD-0001"));

        mockMvc.perform(post("/api/exposures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Exposure reference already exists: EXP-USD-0001"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/exposures/{id} - Should return exposure details by ID")
    void getExposureById_Success_Returns200() throws Exception {
        when(exposureService.getExposureById(1L)).thenReturn(createSampleResponseDto());

        mockMvc.perform(get("/api/exposures/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.exposureReference").value("EXP-USD-0001"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/exposures/{id} - Should return 404 Not Found when ID does not exist")
    void getExposureById_NotFound_Returns404() throws Exception {
        when(exposureService.getExposureById(99L)).thenThrow(new ResourceNotFoundException("Exposure not found with id: 99"));

        mockMvc.perform(get("/api/exposures/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Exposure not found with id: 99"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/exposures/{id} - Should update exposure and return 200 OK")
    void updateExposure_Success_Returns200() throws Exception {
        ExposureRequestDto request = createSampleRequestDto();
        ExposureResponseDto response = createSampleResponseDto();
        response.setDescription("Updated description");

        when(exposureService.updateExposure(eq(1L), any(ExposureRequestDto.class))).thenReturn(response);

        mockMvc.perform(put("/api/exposures/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/exposures/{id} - Should delete exposure and return 204 No Content")
    void deleteExposure_Success_Returns204() throws Exception {
        doNothing().when(exposureService).deleteExposure(1L);

        mockMvc.perform(delete("/api/exposures/1"))
                .andExpect(status().isNoContent());

        verify(exposureService).deleteExposure(1L);
    }
}
