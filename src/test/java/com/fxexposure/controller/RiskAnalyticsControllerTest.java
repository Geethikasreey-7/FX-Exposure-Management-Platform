package com.fxexposure.controller;

import com.fxexposure.config.SecurityConfig;
import com.fxexposure.dto.CurrencyRiskResponseDto;
import com.fxexposure.dto.ExposureRiskResponseDto;
import com.fxexposure.dto.RiskSummaryResponseDto;
import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.entity.ExposureType;
import com.fxexposure.exception.GlobalExceptionHandler;
import com.fxexposure.exception.ResourceNotFoundException;
import com.fxexposure.security.CustomUserDetailsService;
import com.fxexposure.security.JwtAuthenticationEntryPoint;
import com.fxexposure.security.JwtAuthenticationFilter;
import com.fxexposure.security.JwtService;
import com.fxexposure.service.RiskAnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiskAnalyticsController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class RiskAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskAnalyticsService riskAnalyticsService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("1. GET /api/risk-analytics/summary - Should return 401 when request lacks JWT")
    void getSummary_WithoutJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/risk-analytics/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("2. GET /api/risk-analytics/summary - Should return 200 with summary when authorized")
    void getSummary_WithValidJwt_Returns200() throws Exception {
        String token = "valid.jwt.token";
        UserDetails userDetails = new User("analyst", "password", Collections.emptyList());

        when(jwtService.extractUsername(token)).thenReturn("analyst");
        when(userDetailsService.loadUserByUsername("analyst")).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        RiskSummaryResponseDto summaryDto = RiskSummaryResponseDto.builder()
                .totalExposureAmount(new BigDecimal("1000000.00"))
                .totalHedgedAmount(new BigDecimal("700000.00"))
                .totalUnhedgedAmount(new BigDecimal("300000.00"))
                .overallHedgeRatio(new BigDecimal("70.00"))
                .totalExposureCount(1)
                .totalHedgedExposureCount(1)
                .totalPartiallyHedgedExposureCount(1)
                .totalOpenExposureCount(0)
                .totalFullyHedgedExposureCount(0)
                .build();

        when(riskAnalyticsService.getOverallRiskSummary()).thenReturn(summaryDto);

        mockMvc.perform(get("/api/risk-analytics/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExposureAmount").value(1000000.00))
                .andExpect(jsonPath("$.totalHedgedAmount").value(700000.00))
                .andExpect(jsonPath("$.overallHedgeRatio").value(70.00))
                .andExpect(jsonPath("$.totalExposureCount").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("3. GET /api/risk-analytics/currencies - Should return 200 with currency list")
    void getRiskByCurrency_Returns200() throws Exception {
        CurrencyRiskResponseDto usdRisk = CurrencyRiskResponseDto.builder()
                .currency("USD")
                .totalExposureAmount(new BigDecimal("1000000.00"))
                .totalHedgedAmount(new BigDecimal("700000.00"))
                .totalUnhedgedAmount(new BigDecimal("300000.00"))
                .hedgeRatio(new BigDecimal("70.00"))
                .exposureCount(1)
                .build();

        when(riskAnalyticsService.getRiskByCurrency()).thenReturn(List.of(usdRisk));

        mockMvc.perform(get("/api/risk-analytics/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].totalExposureAmount").value(1000000.00))
                .andExpect(jsonPath("$[0].hedgeRatio").value(70.00));
    }

    @Test
    @WithMockUser
    @DisplayName("4. GET /api/risk-analytics/exposures - Should return 200 with exposure list")
    void getExposureRiskAnalytics_Returns200() throws Exception {
        ExposureRiskResponseDto expRisk = ExposureRiskResponseDto.builder()
                .exposureId(1L)
                .exposureReference("EXP-USD-001")
                .exposureType(ExposureType.RECEIVABLE)
                .currency("USD")
                .exposureAmount(new BigDecimal("1000000.00"))
                .hedgedAmount(new BigDecimal("700000.00"))
                .unhedgedAmount(new BigDecimal("300000.00"))
                .hedgeRatio(new BigDecimal("70.00"))
                .status(ExposureStatus.PARTIALLY_HEDGED)
                .build();

        when(riskAnalyticsService.getExposureRiskAnalytics()).thenReturn(List.of(expRisk));

        mockMvc.perform(get("/api/risk-analytics/exposures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exposureId").value(1L))
                .andExpect(jsonPath("$[0].exposureReference").value("EXP-USD-001"))
                .andExpect(jsonPath("$[0].status").value("PARTIALLY_HEDGED"));
    }

    @Test
    @WithMockUser
    @DisplayName("5. GET /api/risk-analytics/exposures/{id} - Should return 200 with single exposure")
    void getExposureRiskAnalyticsById_Success_Returns200() throws Exception {
        ExposureRiskResponseDto expRisk = ExposureRiskResponseDto.builder()
                .exposureId(1L)
                .exposureReference("EXP-USD-001")
                .exposureType(ExposureType.RECEIVABLE)
                .currency("USD")
                .exposureAmount(new BigDecimal("1000000.00"))
                .hedgedAmount(new BigDecimal("700000.00"))
                .unhedgedAmount(new BigDecimal("300000.00"))
                .hedgeRatio(new BigDecimal("70.00"))
                .status(ExposureStatus.PARTIALLY_HEDGED)
                .build();

        when(riskAnalyticsService.getExposureRiskAnalyticsById(1L)).thenReturn(expRisk);

        mockMvc.perform(get("/api/risk-analytics/exposures/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exposureId").value(1L))
                .andExpect(jsonPath("$.exposureReference").value("EXP-USD-001"))
                .andExpect(jsonPath("$.hedgeRatio").value(70.00));
    }

    @Test
    @WithMockUser
    @DisplayName("6. GET /api/risk-analytics/exposures/{id} - Should return 404 when exposure not found")
    void getExposureRiskAnalyticsById_NotFound_Returns404() throws Exception {
        when(riskAnalyticsService.getExposureRiskAnalyticsById(99L))
                .thenThrow(new ResourceNotFoundException("Exposure not found with id: 99"));

        mockMvc.perform(get("/api/risk-analytics/exposures/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Exposure not found with id: 99"));
    }
}

