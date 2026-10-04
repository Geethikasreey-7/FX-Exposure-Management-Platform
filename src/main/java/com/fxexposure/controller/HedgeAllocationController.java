package com.fxexposure.controller;

import com.fxexposure.dto.HedgeAllocationRequestDto;
import com.fxexposure.dto.HedgeAllocationResponseDto;
import com.fxexposure.dto.HedgeSummaryResponseDto;
import com.fxexposure.service.HedgeAllocationService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller managing FX hedge allocations between exposures and derivatives.
 */
@RestController
@RequestMapping("/api/hedge-allocations")
@RequiredArgsConstructor
@Tag(name = "Hedge Allocation Management", description = "Endpoints for managing hedge allocations between exposures and derivatives")
@SecurityRequirement(name = "BearerAuth")
public class HedgeAllocationController {

    private final HedgeAllocationService hedgeAllocationService;

    @PostMapping
    @Operation(summary = "Create a hedge allocation", description = "Allocates a derivative contract amount to an exposure. Validates against remaining limits.")
    @ApiResponse(responseCode = "201", description = "Hedge allocation created successfully")
    @ApiResponse(responseCode = "400", description = "Validation failure or over-allocation")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Exposure or derivative not found")
    @ApiResponse(responseCode = "409", description = "Duplicate allocation reference")
    public ResponseEntity<HedgeAllocationResponseDto> createAllocation(@Valid @RequestBody HedgeAllocationRequestDto requestDto) {
        HedgeAllocationResponseDto created = hedgeAllocationService.createAllocation(requestDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all hedge allocations", description = "Retrieves all existing hedge allocations")
    @ApiResponse(responseCode = "200", description = "List of allocations retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    public ResponseEntity<List<HedgeAllocationResponseDto>> getAllAllocations() {
        List<HedgeAllocationResponseDto> allocations = hedgeAllocationService.getAllAllocations();
        return ResponseEntity.ok(allocations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hedge allocation by ID", description = "Retrieves details of a specific hedge allocation by ID")
    @ApiResponse(responseCode = "200", description = "Allocation found")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Allocation not found")
    public ResponseEntity<HedgeAllocationResponseDto> getAllocationById(@PathVariable Long id) {
        HedgeAllocationResponseDto allocation = hedgeAllocationService.getAllocationById(id);
        return ResponseEntity.ok(allocation);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update hedge allocation", description = "Updates details and amount of an existing hedge allocation")
    @ApiResponse(responseCode = "200", description = "Allocation updated successfully")
    @ApiResponse(responseCode = "400", description = "Validation failure or over-allocation")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Allocation, exposure, or derivative not found")
    @ApiResponse(responseCode = "409", description = "Duplicate allocation reference")
    public ResponseEntity<HedgeAllocationResponseDto> updateAllocation(
            @PathVariable Long id,
            @Valid @RequestBody HedgeAllocationRequestDto requestDto) {
        HedgeAllocationResponseDto updated = hedgeAllocationService.updateAllocation(id, requestDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete hedge allocation", description = "Removes a hedge allocation and updates exposure status")
    @ApiResponse(responseCode = "204", description = "Allocation deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Allocation not found")
    public ResponseEntity<Void> deleteAllocation(@PathVariable Long id) {
        hedgeAllocationService.deleteAllocation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exposure/{exposureId}")
    @Operation(summary = "Get allocations by exposure", description = "Retrieves all hedge allocations linked to a specific exposure")
    @ApiResponse(responseCode = "200", description = "Allocations retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Exposure not found")
    public ResponseEntity<List<HedgeAllocationResponseDto>> getAllocationsByExposure(@PathVariable Long exposureId) {
        List<HedgeAllocationResponseDto> allocations = hedgeAllocationService.getAllocationsByExposure(exposureId);
        return ResponseEntity.ok(allocations);
    }

    @GetMapping("/derivative/{derivativeId}")
    @Operation(summary = "Get allocations by derivative", description = "Retrieves all hedge allocations linked to a specific derivative")
    @ApiResponse(responseCode = "200", description = "Allocations retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Derivative not found")
    public ResponseEntity<List<HedgeAllocationResponseDto>> getAllocationsByDerivative(@PathVariable Long derivativeId) {
        List<HedgeAllocationResponseDto> allocations = hedgeAllocationService.getAllocationsByDerivative(derivativeId);
        return ResponseEntity.ok(allocations);
    }

    @GetMapping("/exposure/{exposureId}/summary")
    @Operation(summary = "Get exposure hedge summary", description = "Calculates total hedged amount, unhedged amount, hedge ratio, and exposure status")
    @ApiResponse(responseCode = "200", description = "Summary calculated successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Exposure not found")
    public ResponseEntity<HedgeSummaryResponseDto> getExposureHedgeSummary(@PathVariable Long exposureId) {
        HedgeSummaryResponseDto summary = hedgeAllocationService.getExposureHedgeSummary(exposureId);
        return ResponseEntity.ok(summary);
    }
}

