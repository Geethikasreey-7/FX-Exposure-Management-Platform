package com.fxexposure.service;

import com.fxexposure.dto.HedgeAllocationRequestDto;
import com.fxexposure.dto.HedgeAllocationResponseDto;
import com.fxexposure.dto.HedgeSummaryResponseDto;
import com.fxexposure.entity.Derivative;
import com.fxexposure.entity.Exposure;
import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.entity.HedgeAllocation;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.exception.ResourceNotFoundException;
import com.fxexposure.repository.DerivativeRepository;
import com.fxexposure.repository.ExposureRepository;
import com.fxexposure.repository.HedgeAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service managing hedge allocation business logic, validation, and exposure hedge status recalculations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HedgeAllocationService {

    private final HedgeAllocationRepository hedgeAllocationRepository;
    private final ExposureRepository exposureRepository;
    private final DerivativeRepository derivativeRepository;

    /**
     * Create a new hedge allocation after validating constraints and available amounts.
     */
    public HedgeAllocationResponseDto createAllocation(HedgeAllocationRequestDto requestDto) {
        validateRequest(requestDto);

        if (hedgeAllocationRepository.existsByAllocationReference(requestDto.getAllocationReference().trim())) {
            throw new DuplicateResourceException("Allocation reference already exists: " + requestDto.getAllocationReference());
        }

        Exposure exposure = exposureRepository.findById(requestDto.getExposureId())
                .orElseThrow(() -> new ResourceNotFoundException("Exposure not found with id: " + requestDto.getExposureId()));

        Derivative derivative = derivativeRepository.findById(requestDto.getDerivativeId())
                .orElseThrow(() -> new ResourceNotFoundException("Derivative not found with id: " + requestDto.getDerivativeId()));

        validateCurrencyCompatibility(exposure, derivative);

        // Validate available exposure amount
        BigDecimal currentExposureHedged = hedgeAllocationRepository.sumAllocatedAmountByExposureId(exposure.getId());
        BigDecimal availableExposure = exposure.getAmount().subtract(currentExposureHedged);
        if (requestDto.getAllocatedAmount().compareTo(availableExposure) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Allocated amount (%s) exceeds available exposure amount (%s)",
                    requestDto.getAllocatedAmount(), availableExposure));
        }

        // Validate available derivative notional
        BigDecimal currentDerivativeAllocated = hedgeAllocationRepository.sumAllocatedAmountByDerivativeId(derivative.getId());
        BigDecimal availableDerivative = derivative.getNotionalAmount().subtract(currentDerivativeAllocated);
        if (requestDto.getAllocatedAmount().compareTo(availableDerivative) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Allocated amount (%s) exceeds available derivative notional amount (%s)",
                    requestDto.getAllocatedAmount(), availableDerivative));
        }

        HedgeAllocation allocation = HedgeAllocation.builder()
                .allocationReference(requestDto.getAllocationReference().trim())
                .exposure(exposure)
                .derivative(derivative)
                .allocatedAmount(requestDto.getAllocatedAmount())
                .allocationDate(requestDto.getAllocationDate())
                .description(requestDto.getDescription())
                .build();

        HedgeAllocation saved = hedgeAllocationRepository.save(allocation);

        // Recalculate exposure hedge status
        updateExposureStatus(exposure);

        return mapToResponseDto(saved);
    }

    /**
     * Retrieve all hedge allocations.
     */
    @Transactional(readOnly = true)
    public List<HedgeAllocationResponseDto> getAllAllocations() {
        return hedgeAllocationRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    /**
     * Retrieve a specific hedge allocation by ID.
     */
    @Transactional(readOnly = true)
    public HedgeAllocationResponseDto getAllocationById(Long id) {
        HedgeAllocation allocation = hedgeAllocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hedge allocation not found with id: " + id));
        return mapToResponseDto(allocation);
    }

    /**
     * Retrieve all allocations associated with an exposure.
     */
    @Transactional(readOnly = true)
    public List<HedgeAllocationResponseDto> getAllocationsByExposure(Long exposureId) {
        if (!exposureRepository.existsById(exposureId)) {
            throw new ResourceNotFoundException("Exposure not found with id: " + exposureId);
        }
        return hedgeAllocationRepository.findByExposureId(exposureId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    /**
     * Retrieve all allocations associated with a derivative.
     */
    @Transactional(readOnly = true)
    public List<HedgeAllocationResponseDto> getAllocationsByDerivative(Long derivativeId) {
        if (!derivativeRepository.existsById(derivativeId)) {
            throw new ResourceNotFoundException("Derivative not found with id: " + derivativeId);
        }
        return hedgeAllocationRepository.findByDerivativeId(derivativeId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    /**
     * Update an existing hedge allocation.
     */
    public HedgeAllocationResponseDto updateAllocation(Long id, HedgeAllocationRequestDto requestDto) {
        validateRequest(requestDto);

        HedgeAllocation existing = hedgeAllocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hedge allocation not found with id: " + id));

        hedgeAllocationRepository.findByAllocationReference(requestDto.getAllocationReference().trim())
                .filter(a -> !a.getId().equals(id))
                .ifPresent(a -> {
                    throw new DuplicateResourceException("Allocation reference already in use by another record: " + requestDto.getAllocationReference());
                });

        Exposure exposure = exposureRepository.findById(requestDto.getExposureId())
                .orElseThrow(() -> new ResourceNotFoundException("Exposure not found with id: " + requestDto.getExposureId()));

        Derivative derivative = derivativeRepository.findById(requestDto.getDerivativeId())
                .orElseThrow(() -> new ResourceNotFoundException("Derivative not found with id: " + requestDto.getDerivativeId()));

        validateCurrencyCompatibility(exposure, derivative);

        // Validate available exposure amount (excluding current allocation)
        BigDecimal currentExposureHedged = hedgeAllocationRepository.sumAllocatedAmountByExposureIdExcluding(exposure.getId(), id);
        BigDecimal availableExposure = exposure.getAmount().subtract(currentExposureHedged);
        if (requestDto.getAllocatedAmount().compareTo(availableExposure) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Allocated amount (%s) exceeds available exposure amount (%s)",
                    requestDto.getAllocatedAmount(), availableExposure));
        }

        // Validate available derivative notional (excluding current allocation)
        BigDecimal currentDerivativeAllocated = hedgeAllocationRepository.sumAllocatedAmountByDerivativeIdExcluding(derivative.getId(), id);
        BigDecimal availableDerivative = derivative.getNotionalAmount().subtract(currentDerivativeAllocated);
        if (requestDto.getAllocatedAmount().compareTo(availableDerivative) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Allocated amount (%s) exceeds available derivative notional amount (%s)",
                    requestDto.getAllocatedAmount(), availableDerivative));
        }

        Exposure oldExposure = existing.getExposure();

        existing.setAllocationReference(requestDto.getAllocationReference().trim());
        existing.setExposure(exposure);
        existing.setDerivative(derivative);
        existing.setAllocatedAmount(requestDto.getAllocatedAmount());
        existing.setAllocationDate(requestDto.getAllocationDate());
        existing.setDescription(requestDto.getDescription());

        HedgeAllocation updated = hedgeAllocationRepository.save(existing);

        // Recalculate status for current exposure
        updateExposureStatus(exposure);

        // If exposure changed, recalculate status for old exposure as well
        if (!oldExposure.getId().equals(exposure.getId())) {
            updateExposureStatus(oldExposure);
        }

        return mapToResponseDto(updated);
    }

    /**
     * Delete a hedge allocation and recalculate exposure status.
     */
    public void deleteAllocation(Long id) {
        HedgeAllocation allocation = hedgeAllocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hedge allocation not found with id: " + id));

        Exposure exposure = allocation.getExposure();
        hedgeAllocationRepository.delete(allocation);
        hedgeAllocationRepository.flush();

        updateExposureStatus(exposure);
    }

    /**
     * Retrieve the hedge summary for an exposure.
     */
    @Transactional(readOnly = true)
    public HedgeSummaryResponseDto getExposureHedgeSummary(Long exposureId) {
        Exposure exposure = exposureRepository.findById(exposureId)
                .orElseThrow(() -> new ResourceNotFoundException("Exposure not found with id: " + exposureId));

        BigDecimal totalHedged = hedgeAllocationRepository.sumAllocatedAmountByExposureId(exposureId);
        BigDecimal unhedged = exposure.getAmount().subtract(totalHedged);
        if (unhedged.compareTo(BigDecimal.ZERO) < 0) {
            unhedged = BigDecimal.ZERO;
        }

        BigDecimal hedgeRatio = BigDecimal.ZERO;
        if (exposure.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            hedgeRatio = totalHedged.multiply(BigDecimal.valueOf(100))
                    .divide(exposure.getAmount(), 2, RoundingMode.HALF_UP);
        }

        return HedgeSummaryResponseDto.builder()
                .exposureId(exposure.getId())
                .exposureReference(exposure.getExposureReference())
                .exposureAmount(exposure.getAmount())
                .totalHedgedAmount(totalHedged)
                .unhedgedAmount(unhedged)
                .hedgeRatio(hedgeRatio)
                .exposureStatus(exposure.getStatus())
                .build();
    }

    /**
     * Common validation for request DTO.
     */
    private void validateRequest(HedgeAllocationRequestDto dto) {
        if (dto.getAllocatedAmount() == null || dto.getAllocatedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Allocated amount must be greater than zero");
        }
        if (dto.getAllocationDate() == null) {
            throw new IllegalArgumentException("Allocation date is required");
        }
    }

    /**
     * Verify that the derivative shares currency context with the exposure.
     */
    private void validateCurrencyCompatibility(Exposure exposure, Derivative derivative) {
        boolean match = derivative.getBaseCurrency().equalsIgnoreCase(exposure.getCurrency())
                || derivative.getQuoteCurrency().equalsIgnoreCase(exposure.getCurrency())
                || derivative.getBaseCurrency().equalsIgnoreCase(exposure.getBaseCurrency())
                || derivative.getQuoteCurrency().equalsIgnoreCase(exposure.getBaseCurrency());

        if (!match) {
            throw new IllegalArgumentException(String.format(
                    "Derivative currency pair (%s/%s) does not match exposure currency (%s/%s)",
                    derivative.getBaseCurrency(), derivative.getQuoteCurrency(),
                    exposure.getCurrency(), exposure.getBaseCurrency()));
        }
    }

    /**
     * Recalculate and update the exposure status based on total allocated hedge amount.
     */
    private void updateExposureStatus(Exposure exposure) {
        BigDecimal totalHedged = hedgeAllocationRepository.sumAllocatedAmountByExposureId(exposure.getId());

        if (totalHedged.compareTo(BigDecimal.ZERO) == 0) {
            exposure.setStatus(ExposureStatus.OPEN);
        } else if (totalHedged.compareTo(exposure.getAmount()) >= 0) {
            exposure.setStatus(ExposureStatus.FULLY_HEDGED);
        } else {
            exposure.setStatus(ExposureStatus.PARTIALLY_HEDGED);
        }

        exposureRepository.save(exposure);
    }

    /**
     * Map HedgeAllocation entity to HedgeAllocationResponseDto.
     */
    private HedgeAllocationResponseDto mapToResponseDto(HedgeAllocation allocation) {
        return HedgeAllocationResponseDto.builder()
                .id(allocation.getId())
                .allocationReference(allocation.getAllocationReference())
                .exposureId(allocation.getExposure() != null ? allocation.getExposure().getId() : null)
                .exposureReference(allocation.getExposure() != null ? allocation.getExposure().getExposureReference() : null)
                .derivativeId(allocation.getDerivative() != null ? allocation.getDerivative().getId() : null)
                .derivativeReference(allocation.getDerivative() != null ? allocation.getDerivative().getDerivativeReference() : null)
                .allocatedAmount(allocation.getAllocatedAmount())
                .allocationDate(allocation.getAllocationDate())
                .description(allocation.getDescription())
                .createdAt(allocation.getCreatedAt())
                .updatedAt(allocation.getUpdatedAt())
                .build();
    }
}

