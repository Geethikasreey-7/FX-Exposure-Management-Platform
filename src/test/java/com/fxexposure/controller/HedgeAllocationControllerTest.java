package com.fxexposure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxexposure.config.SecurityConfig;
import com.fxexposure.dto.HedgeAllocationRequestDto;
import com.fxexposure.dto.HedgeAllocationResponseDto;
import com.fxexposure.dto.HedgeSummaryResponseDto;
import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.exception.GlobalExceptionHandler;
import com.fxexposure.exception.ResourceNotFoundException;
import com.fxexposure.security.CustomUserDetailsService;
import com.fxexposure.security.JwtAuthenticationEntryPoint;
import com.fxexposure.security.JwtAuthenticationFilter;
import com.fxexposure.security.JwtService;
import com.fxexposure.service.HedgeAllocationService;
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

@WebMvcTest(HedgeAllocationController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class HedgeAllocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HedgeAllocationService hedgeAllocationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private HedgeAllocationResponseDto createSampleResponseDto() {
        return HedgeAllocationResponseDto.builder()
                .id(1L)
                .allocationReference("ALLOC-001")
                .exposureId(1L)
                .exposureReference("EXP-USD-0001")
                .derivativeId(1L)
                .derivativeReference("DER-USD-0001")
                .allocatedAmount(new BigDecimal("40000.00"))
                .allocationDate(LocalDate.of(2026, 10, 4))
                .description("Sample hedge allocation")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private HedgeAllocationRequestDto createSampleRequestDto() {
        return HedgeAllocationRequestDto.builder()
                .allocationReference("ALLOC-001")
                .exposureId(1L)
                .derivativeId(1L)
                .allocatedAmount(new BigDecimal("40000.00"))
                .allocationDate(LocalDate.of(2026, 10, 4))
                .description("Sample hedge allocation")
                .build();
    }

    @Test
    @DisplayName("1. GET /api/hedge-allocations - Should return 401 when request lacks JWT")
    void getAllAllocations_WithoutJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/hedge-allocations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("2. GET /api/hedge-allocations - Should return 200 with list when authorized")
    void getAllAllocations_WithValidJwt_Returns200() throws Exception {
        String token = "valid.jwt.token";
        UserDetails userDetails = new User("analyst", "password", Collections.emptyList());

        when(jwtService.extractUsername(token)).thenReturn("analyst");
        when(userDetailsService.loadUserByUsername("analyst")).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
        when(hedgeAllocationService.getAllAllocations()).thenReturn(List.of(createSampleResponseDto()));

        mockMvc.perform(get("/api/hedge-allocations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].allocationReference").value("ALLOC-001"))
                .andExpect(jsonPath("$[0].allocatedAmount").value(40000.00));
    }

    @Test
    @WithMockUser
    @DisplayName("3. POST /api/hedge-allocations - Should create allocation and return 201 Created")
    void createAllocation_Success_Returns201() throws Exception {
        HedgeAllocationRequestDto request = createSampleRequestDto();
        HedgeAllocationResponseDto response = createSampleResponseDto();

        when(hedgeAllocationService.createAllocation(any(HedgeAllocationRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/hedge-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.allocationReference").value("ALLOC-001"))
                .andExpect(jsonPath("$.allocatedAmount").value(40000.00));
    }

    @Test
    @WithMockUser
    @DisplayName("4. POST /api/hedge-allocations - Should return 400 Bad Request on validation error")
    void createAllocation_ValidationFailure_Returns400() throws Exception {
        HedgeAllocationRequestDto invalidRequest = HedgeAllocationRequestDto.builder()
                .allocationReference("") // blank
                .exposureId(null)        // null
                .derivativeId(null)      // null
                .allocatedAmount(new BigDecimal("-100.00")) // negative
                .allocationDate(null)    // null
                .build();

        mockMvc.perform(post("/api/hedge-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("5. POST /api/hedge-allocations - Should return 409 Conflict on duplicate reference")
    void createAllocation_DuplicateReference_Returns409() throws Exception {
        HedgeAllocationRequestDto request = createSampleRequestDto();

        when(hedgeAllocationService.createAllocation(any(HedgeAllocationRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Allocation reference already exists: ALLOC-001"));

        mockMvc.perform(post("/api/hedge-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Allocation reference already exists: ALLOC-001"));
    }

    @Test
    @WithMockUser
    @DisplayName("6. GET /api/hedge-allocations/{id} - Should return allocation details by ID")
    void getAllocationById_Success_Returns200() throws Exception {
        when(hedgeAllocationService.getAllocationById(1L)).thenReturn(createSampleResponseDto());

        mockMvc.perform(get("/api/hedge-allocations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.allocationReference").value("ALLOC-001"));
    }

    @Test
    @WithMockUser
    @DisplayName("7. PUT /api/hedge-allocations/{id} - Should update allocation and return 200 OK")
    void updateAllocation_Success_Returns200() throws Exception {
        HedgeAllocationRequestDto request = createSampleRequestDto();
        HedgeAllocationResponseDto response = createSampleResponseDto();
        response.setDescription("Updated allocation description");

        when(hedgeAllocationService.updateAllocation(eq(1L), any(HedgeAllocationRequestDto.class))).thenReturn(response);

        mockMvc.perform(put("/api/hedge-allocations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated allocation description"));
    }

    @Test
    @WithMockUser
    @DisplayName("8. DELETE /api/hedge-allocations/{id} - Should delete allocation and return 204 No Content")
    void deleteAllocation_Success_Returns204() throws Exception {
        doNothing().when(hedgeAllocationService).deleteAllocation(1L);

        mockMvc.perform(delete("/api/hedge-allocations/1"))
                .andExpect(status().isNoContent());

        verify(hedgeAllocationService).deleteAllocation(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("9. GET /api/hedge-allocations/exposure/{exposureId} - Should return allocations by exposure")
    void getAllocationsByExposure_Success_Returns200() throws Exception {
        when(hedgeAllocationService.getAllocationsByExposure(1L)).thenReturn(List.of(createSampleResponseDto()));

        mockMvc.perform(get("/api/hedge-allocations/exposure/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exposureId").value(1L))
                .andExpect(jsonPath("$[0].allocationReference").value("ALLOC-001"));
    }

    @Test
    @WithMockUser
    @DisplayName("10. GET /api/hedge-allocations/derivative/{derivativeId} - Should return allocations by derivative")
    void getAllocationsByDerivative_Success_Returns200() throws Exception {
        when(hedgeAllocationService.getAllocationsByDerivative(1L)).thenReturn(List.of(createSampleResponseDto()));

        mockMvc.perform(get("/api/hedge-allocations/derivative/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].derivativeId").value(1L))
                .andExpect(jsonPath("$[0].allocationReference").value("ALLOC-001"));
    }

    @Test
    @WithMockUser
    @DisplayName("11. GET /api/hedge-allocations/exposure/{exposureId}/summary - Should return exposure hedge summary")
    void getExposureHedgeSummary_Success_Returns200() throws Exception {
        HedgeSummaryResponseDto summary = HedgeSummaryResponseDto.builder()
                .exposureId(1L)
                .exposureReference("EXP-USD-0001")
                .exposureAmount(new BigDecimal("100000.00"))
                .totalHedgedAmount(new BigDecimal("60000.00"))
                .unhedgedAmount(new BigDecimal("40000.00"))
                .hedgeRatio(new BigDecimal("60.00"))
                .exposureStatus(ExposureStatus.PARTIALLY_HEDGED)
                .build();

        when(hedgeAllocationService.getExposureHedgeSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/hedge-allocations/exposure/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exposureId").value(1L))
                .andExpect(jsonPath("$.exposureAmount").value(100000.00))
                .andExpect(jsonPath("$.totalHedgedAmount").value(60000.00))
                .andExpect(jsonPath("$.unhedgedAmount").value(40000.00))
                .andExpect(jsonPath("$.hedgeRatio").value(60.00))
                .andExpect(jsonPath("$.exposureStatus").value("PARTIALLY_HEDGED"));
    }
}

