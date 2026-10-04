package com.fxexposure.controller;

import com.fxexposure.dto.CurrencyRiskResponseDto;
import com.fxexposure.dto.ExposureRiskResponseDto;
import com.fxexposure.dto.RiskSummaryResponseDto;
import com.fxexposure.service.RiskAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing portfolio-level and exposure-level FX risk analytics.
 */
@RestController
@RequestMapping("/api/risk-analytics")
@RequiredArgsConstructor
@Tag(name = "Risk Analytics", description = "Endpoints for portfolio and exposure-level foreign exchange risk analytics")
@SecurityRequirement(name = "BearerAuth")
public class RiskAnalyticsController {

    private final RiskAnalyticsService riskAnalyticsService;

    @GetMapping("/summary")
    @Operation(summary = "Get portfolio risk summary", description = "Calculates overall portfolio FX exposure, hedged amount, unhedged amount, and aggregate hedge ratio")
    @ApiResponse(responseCode = "200", description = "Portfolio risk summary successfully retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    public ResponseEntity<RiskSummaryResponseDto> getOverallRiskSummary() {
        RiskSummaryResponseDto summary = riskAnalyticsService.getOverallRiskSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/currencies")
    @Operation(summary = "Get risk by currency", description = "Calculates total exposure, hedged, unhedged, and hedge ratio grouped by currency")
    @ApiResponse(responseCode = "200", description = "Currency risk breakdown successfully retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    public ResponseEntity<List<CurrencyRiskResponseDto>> getRiskByCurrency() {
        List<CurrencyRiskResponseDto> currencyRisks = riskAnalyticsService.getRiskByCurrency();
        return ResponseEntity.ok(currencyRisks);
    }

    @GetMapping("/exposures")
    @Operation(summary = "Get exposure-level risk analytics", description = "Calculates risk and hedging metrics for all individual exposures")
    @ApiResponse(responseCode = "200", description = "Exposure risk metrics successfully retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    public ResponseEntity<List<ExposureRiskResponseDto>> getExposureRiskAnalytics() {
        List<ExposureRiskResponseDto> exposureRisks = riskAnalyticsService.getExposureRiskAnalytics();
        return ResponseEntity.ok(exposureRisks);
    }

    @GetMapping("/exposures/{exposureId}")
    @Operation(summary = "Get single exposure risk analytics", description = "Calculates risk and hedging metrics for a specific exposure by ID")
    @ApiResponse(responseCode = "200", description = "Exposure risk metrics successfully retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Exposure not found")
    public ResponseEntity<ExposureRiskResponseDto> getExposureRiskAnalyticsById(@PathVariable Long exposureId) {
        ExposureRiskResponseDto exposureRisk = riskAnalyticsService.getExposureRiskAnalyticsById(exposureId);
        return ResponseEntity.ok(exposureRisk);
    }
}

