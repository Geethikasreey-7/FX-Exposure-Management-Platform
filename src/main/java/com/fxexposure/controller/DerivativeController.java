package com.fxexposure.controller;

import com.fxexposure.dto.DerivativeRequestDto;
import com.fxexposure.dto.DerivativeResponseDto;
import com.fxexposure.entity.DerivativeStatus;
import com.fxexposure.entity.DerivativeType;
import com.fxexposure.service.DerivativeService;
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
 * REST controller for managing Foreign Exchange (FX) derivative contracts.
 */
@RestController
@RequestMapping("/api/derivatives")
@RequiredArgsConstructor
@Tag(name = "Derivative Management", description = "Endpoints for managing foreign exchange derivative contracts")
@SecurityRequirement(name = "BearerAuth")
public class DerivativeController {

    private final DerivativeService derivativeService;

    @PostMapping
    @Operation(summary = "Create an FX derivative", description = "Registers a new FX derivative contract linked to an existing exposure. Defaults status to ACTIVE if omitted.")
    @ApiResponse(responseCode = "201", description = "Derivative successfully created")
    @ApiResponse(responseCode = "400", description = "Validation failure or invalid date/amount")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Referenced exposure not found")
    @ApiResponse(responseCode = "409", description = "Duplicate derivative reference")
    public ResponseEntity<DerivativeResponseDto> createDerivative(@Valid @RequestBody DerivativeRequestDto requestDto) {
        DerivativeResponseDto created = derivativeService.createDerivative(requestDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all derivatives", description = "Retrieves all derivatives with optional filters by type, status, exposure ID, or currencies")
    @ApiResponse(responseCode = "200", description = "List of derivatives successfully retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    public ResponseEntity<List<DerivativeResponseDto>> getAllDerivatives(
            @Parameter(description = "Filter by derivative type (SPOT, FORWARD, SWAP, OPTION)")
            @RequestParam(required = false) DerivativeType derivativeType,
            @Parameter(description = "Filter by status (ACTIVE, MATURED, SETTLED, CANCELLED)")
            @RequestParam(required = false) DerivativeStatus status,
            @Parameter(description = "Filter by linked exposure ID")
            @RequestParam(required = false) Long exposureId,
            @Parameter(description = "Filter by 3-letter base currency (e.g. USD)")
            @RequestParam(required = false) String baseCurrency,
            @Parameter(description = "Filter by 3-letter quote currency (e.g. INR)")
            @RequestParam(required = false) String quoteCurrency) {

        List<DerivativeResponseDto> derivatives = derivativeService.getAllDerivatives(derivativeType, status, exposureId, baseCurrency, quoteCurrency);
        return ResponseEntity.ok(derivatives);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get derivative by ID", description = "Retrieves a single derivative contract by its database ID")
    @ApiResponse(responseCode = "200", description = "Derivative found and returned")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Derivative not found")
    public ResponseEntity<DerivativeResponseDto> getDerivativeById(@PathVariable Long id) {
        DerivativeResponseDto derivative = derivativeService.getDerivativeById(id);
        return ResponseEntity.ok(derivative);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update derivative", description = "Updates details of an existing FX derivative contract by ID")
    @ApiResponse(responseCode = "200", description = "Derivative updated successfully")
    @ApiResponse(responseCode = "400", description = "Validation failure or invalid date/amount")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Derivative or referenced exposure not found")
    @ApiResponse(responseCode = "409", description = "Derivative reference already in use by another record")
    public ResponseEntity<DerivativeResponseDto> updateDerivative(
            @PathVariable Long id,
            @Valid @RequestBody DerivativeRequestDto requestDto) {
        DerivativeResponseDto updated = derivativeService.updateDerivative(id, requestDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete derivative", description = "Deletes an FX derivative contract by ID without deleting its associated exposure")
    @ApiResponse(responseCode = "204", description = "Derivative deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    @ApiResponse(responseCode = "404", description = "Derivative not found")
    public ResponseEntity<Void> deleteDerivative(@PathVariable Long id) {
        derivativeService.deleteDerivative(id);
        return ResponseEntity.noContent().build();
    }
}

