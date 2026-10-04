package com.fxexposure.dto;

import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.entity.ExposureType;
import jakarta.validation.constraints.AssertTrue;
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
 * Data Transfer Object for creating or updating an FX exposure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExposureRequestDto {

    @NotBlank(message = "Exposure reference cannot be blank")
    @Size(min = 3, max = 50, message = "Exposure reference must be between 3 and 50 characters")
    private String exposureReference;

    @NotNull(message = "Exposure type is required (RECEIVABLE, PAYABLE, FORECAST, ASSET, LIABILITY)")
    private ExposureType exposureType;

    @NotBlank(message = "Currency cannot be blank")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO currency code")
    private String currency;

    @NotBlank(message = "Base currency cannot be blank")
    @Size(min = 3, max = 3, message = "Base currency must be a 3-letter ISO currency code")
    private String baseCurrency;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Exposure date is required")
    private LocalDate exposureDate;

    @NotNull(message = "Maturity date is required")
    private LocalDate maturityDate;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private ExposureStatus status;

    @AssertTrue(message = "Maturity date cannot be before exposure date")
    public boolean isMaturityDateValid() {
        if (exposureDate == null || maturityDate == null) {
            return true;
        }
        return !maturityDate.isBefore(exposureDate);
    }
}
