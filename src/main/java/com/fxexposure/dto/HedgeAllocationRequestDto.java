package com.fxexposure.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for creating or updating a hedge allocation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HedgeAllocationRequestDto {

    @NotBlank(message = "Allocation reference cannot be blank")
    @Size(min = 3, max = 50, message = "Allocation reference must be between 3 and 50 characters")
    private String allocationReference;

    @NotNull(message = "Exposure ID is required")
    private Long exposureId;

    @NotNull(message = "Derivative ID is required")
    private Long derivativeId;

    @NotNull(message = "Allocated amount is required")
    @DecimalMin(value = "0.01", message = "Allocated amount must be greater than zero")
    private BigDecimal allocatedAmount;

    @NotNull(message = "Allocation date is required")
    private LocalDate allocationDate;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}

