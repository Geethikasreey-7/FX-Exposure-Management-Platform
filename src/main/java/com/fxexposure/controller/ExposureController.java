package com.fxexposure.controller;

import com.fxexposure.dto.ExposureRequestDto;
import com.fxexposure.dto.ExposureResponseDto;
import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.entity.ExposureType;
import com.fxexposure.service.ExposureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing Foreign Exchange (FX) exposures.
 */
@RestController
@RequestMapping("/api/exposures")
@RequiredArgsConstructor
@Tag(name = "Exposure Management", description = "Endpoints for managing foreign exchange exposures")
@SecurityRequirement(name = "BearerAuth")
public class ExposureController {

    private final ExposureService exposureService;

    @PostMapping
    @Operation(summary = "Create an FX exposure", description = "Registers a new FX exposure. Defaults status to OPEN if omitted.")
    @ApiResponse(responseCode = "201", description = "Exposure successfully created")
    @ApiResponse(responseCode = "400", description = "Validation failure or invalid date range/amount")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "409", description = "Duplicate exposure reference")
    public ResponseEntity<ExposureResponseDto> createExposure(@Valid @RequestBody ExposureRequestDto requestDto) {
        ExposureResponseDto created = exposureService.createExposure(requestDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all exposures", description = "Retrieves all exposures with optional filters by currency, status, or exposure type")
    @ApiResponse(responseCode = "200", description = "List of exposures successfully retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    public ResponseEntity<List<ExposureResponseDto>> getAllExposures(
            @Parameter(description = "Filter by currency (e.g. USD, EUR)")
            @RequestParam(required = false) String currency,
            @Parameter(description = "Filter by status (OPEN, PARTIALLY_HEDGED, FULLY_HEDGED, CLOSED)")
            @RequestParam(required = false) ExposureStatus status,
            @Parameter(description = "Filter by type (RECEIVABLE, PAYABLE, FORECAST, ASSET, LIABILITY)")
            @RequestParam(required = false) ExposureType exposureType) {

        List<ExposureResponseDto> exposures = exposureService.getAllExposures(currency, status, exposureType);
        return ResponseEntity.ok(exposures);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exposure by ID", description = "Retrieves a single exposure by its database ID")
    @ApiResponse(responseCode = "200", description = "Exposure found and returned")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Exposure not found")
    public ResponseEntity<ExposureResponseDto> getExposureById(@PathVariable Long id) {
        ExposureResponseDto exposure = exposureService.getExposureById(id);
        return ResponseEntity.ok(exposure);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update exposure", description = "Updates details of an existing FX exposure by ID")
    @ApiResponse(responseCode = "200", description = "Exposure updated successfully")
    @ApiResponse(responseCode = "400", description = "Validation failure or invalid date range/amount")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Exposure not found")
    @ApiResponse(responseCode = "409", description = "Exposure reference already in use by another record")
    public ResponseEntity<ExposureResponseDto> updateExposure(
            @PathVariable Long id,
            @Valid @RequestBody ExposureRequestDto requestDto) {
        ExposureResponseDto updated = exposureService.updateExposure(id, requestDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete exposure", description = "Deletes an FX exposure record by ID")
    @ApiResponse(responseCode = "204", description = "Exposure deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Exposure not found")
    public ResponseEntity<Void> deleteExposure(@PathVariable Long id) {
        exposureService.deleteExposure(id);
        return ResponseEntity.noContent().build();
    }
}
