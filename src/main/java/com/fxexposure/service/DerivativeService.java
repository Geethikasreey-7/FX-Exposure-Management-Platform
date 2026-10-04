package com.fxexposure.service;

import com.fxexposure.dto.DerivativeRequestDto;
import com.fxexposure.dto.DerivativeResponseDto;
import com.fxexposure.entity.Derivative;
import com.fxexposure.entity.DerivativeStatus;
import com.fxexposure.entity.DerivativeType;
import com.fxexposure.entity.Exposure;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.exception.ResourceNotFoundException;
import com.fxexposure.repository.DerivativeRepository;
import com.fxexposure.repository.ExposureRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service handling business logic and operations for FX Derivative contracts.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DerivativeService {

    private final DerivativeRepository derivativeRepository;
    private final ExposureRepository exposureRepository;

    /**
     * Create a new FX derivative contract after validating constraints and establishing Exposure relationship.
     *
     * @param requestDto derivative creation payload
     * @return DerivativeResponseDto representing the created derivative
     */
    public DerivativeResponseDto createDerivative(DerivativeRequestDto requestDto) {
        validateDerivativeRequest(requestDto);

        if (derivativeRepository.existsByDerivativeReference(requestDto.getDerivativeReference().trim())) {
            throw new DuplicateResourceException("Derivative reference already exists: " + requestDto.getDerivativeReference());
        }

        Exposure exposure = exposureRepository.findById(requestDto.getExposureId())
                .orElseThrow(() -> new ResourceNotFoundException("Exposure not found with id: " + requestDto.getExposureId()));

        DerivativeStatus status = requestDto.getStatus() != null ? requestDto.getStatus() : DerivativeStatus.ACTIVE;

        Derivative derivative = Derivative.builder()
                .derivativeReference(requestDto.getDerivativeReference().trim())
                .derivativeType(requestDto.getDerivativeType())
                .exposure(exposure)
                .baseCurrency(requestDto.getBaseCurrency().trim().toUpperCase())
                .quoteCurrency(requestDto.getQuoteCurrency().trim().toUpperCase())
                .notionalAmount(requestDto.getNotionalAmount())
                .tradeDate(requestDto.getTradeDate())
                .maturityDate(requestDto.getMaturityDate())
                .spotRate(requestDto.getSpotRate())
                .forwardRate(requestDto.getForwardRate())
                .strikeRate(requestDto.getStrikeRate())
                .optionType(requestDto.getOptionType())
                .status(status)
                .settlementDate(requestDto.getSettlementDate())
                .description(requestDto.getDescription())
                .build();

        Derivative savedDerivative = derivativeRepository.save(derivative);
        return mapToResponseDto(savedDerivative);
    }

    /**
     * Retrieve all derivative contracts, optionally filtered by type, status, exposure ID, or currencies.
     *
     * @param derivativeType optional derivative type filter
     * @param status         optional status filter
     * @param exposureId     optional exposure ID filter
     * @param baseCurrency   optional 3-letter base currency filter
     * @param quoteCurrency  optional 3-letter quote currency filter
     * @return list of matching DerivativeResponseDto objects
     */
    @Transactional(readOnly = true)
    public List<DerivativeResponseDto> getAllDerivatives(DerivativeType derivativeType,
                                                         DerivativeStatus status,
                                                         Long exposureId,
                                                         String baseCurrency,
                                                         String quoteCurrency) {
        Specification<Derivative> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (derivativeType != null) {
                predicates.add(cb.equal(root.get("derivativeType"), derivativeType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (exposureId != null) {
                predicates.add(cb.equal(root.get("exposure").get("id"), exposureId));
            }
            if (baseCurrency != null && !baseCurrency.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("baseCurrency")), baseCurrency.trim().toUpperCase()));
            }
            if (quoteCurrency != null && !quoteCurrency.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("quoteCurrency")), quoteCurrency.trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return derivativeRepository.findAll(spec)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    /**
     * Retrieve a specific derivative contract by its primary key.
     *
     * @param id derivative database ID
     * @return DerivativeResponseDto
     */
    @Transactional(readOnly = true)
    public DerivativeResponseDto getDerivativeById(Long id) {
        Derivative derivative = derivativeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Derivative not found with id: " + id));
        return mapToResponseDto(derivative);
    }

    /**
     * Update an existing derivative contract.
     *
     * @param id         derivative database ID
     * @param requestDto updated details
     * @return DerivativeResponseDto
     */
    public DerivativeResponseDto updateDerivative(Long id, DerivativeRequestDto requestDto) {
        validateDerivativeRequest(requestDto);

        Derivative existingDerivative = derivativeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Derivative not found with id: " + id));

        // Check if derivativeReference was changed and conflicts with another record
        derivativeRepository.findByDerivativeReference(requestDto.getDerivativeReference().trim())
                .filter(d -> !d.getId().equals(id))
                .ifPresent(d -> {
                    throw new DuplicateResourceException("Derivative reference already in use by another record: " + requestDto.getDerivativeReference());
                });

        // Verify exposure exists
        Exposure exposure = exposureRepository.findById(requestDto.getExposureId())
                .orElseThrow(() -> new ResourceNotFoundException("Exposure not found with id: " + requestDto.getExposureId()));

        existingDerivative.setDerivativeReference(requestDto.getDerivativeReference().trim());
        existingDerivative.setDerivativeType(requestDto.getDerivativeType());
        existingDerivative.setExposure(exposure);
        existingDerivative.setBaseCurrency(requestDto.getBaseCurrency().trim().toUpperCase());
        existingDerivative.setQuoteCurrency(requestDto.getQuoteCurrency().trim().toUpperCase());
        existingDerivative.setNotionalAmount(requestDto.getNotionalAmount());
        existingDerivative.setTradeDate(requestDto.getTradeDate());
        existingDerivative.setMaturityDate(requestDto.getMaturityDate());
        existingDerivative.setSpotRate(requestDto.getSpotRate());
        existingDerivative.setForwardRate(requestDto.getForwardRate());
        existingDerivative.setStrikeRate(requestDto.getStrikeRate());
        existingDerivative.setOptionType(requestDto.getOptionType());
        existingDerivative.setSettlementDate(requestDto.getSettlementDate());
        existingDerivative.setDescription(requestDto.getDescription());

        // Preserve existing status unless a new valid status is explicitly supplied
        if (requestDto.getStatus() != null) {
            existingDerivative.setStatus(requestDto.getStatus());
        }

        Derivative updatedDerivative = derivativeRepository.save(existingDerivative);
        return mapToResponseDto(updatedDerivative);
    }

    /**
     * Delete a derivative contract by its ID without cascading delete to the associated Exposure.
     *
     * @param id derivative database ID
     */
    public void deleteDerivative(Long id) {
        if (!derivativeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Derivative not found with id: " + id);
        }
        derivativeRepository.deleteById(id);
    }

    /**
     * Common service validation for monetary amounts and date consistency.
     */
    private void validateDerivativeRequest(DerivativeRequestDto dto) {
        if (dto.getNotionalAmount() == null || dto.getNotionalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Notional amount must be greater than zero");
        }
        if (dto.getTradeDate() == null || dto.getMaturityDate() == null) {
            throw new IllegalArgumentException("Trade date and maturity date are required");
        }
        if (dto.getMaturityDate().isBefore(dto.getTradeDate())) {
            throw new IllegalArgumentException("Maturity date cannot be before trade date");
        }
    }

    /**
     * Maps a Derivative entity to DerivativeResponseDto.
     */
    private DerivativeResponseDto mapToResponseDto(Derivative derivative) {
        return DerivativeResponseDto.builder()
                .id(derivative.getId())
                .derivativeReference(derivative.getDerivativeReference())
                .derivativeType(derivative.getDerivativeType())
                .exposureId(derivative.getExposure() != null ? derivative.getExposure().getId() : null)
                .exposureReference(derivative.getExposure() != null ? derivative.getExposure().getExposureReference() : null)
                .baseCurrency(derivative.getBaseCurrency())
                .quoteCurrency(derivative.getQuoteCurrency())
                .notionalAmount(derivative.getNotionalAmount())
                .tradeDate(derivative.getTradeDate())
                .maturityDate(derivative.getMaturityDate())
                .spotRate(derivative.getSpotRate())
                .forwardRate(derivative.getForwardRate())
                .strikeRate(derivative.getStrikeRate())
                .optionType(derivative.getOptionType())
                .status(derivative.getStatus())
                .settlementDate(derivative.getSettlementDate())
                .description(derivative.getDescription())
                .createdAt(derivative.getCreatedAt())
                .updatedAt(derivative.getUpdatedAt())
                .build();
    }
}

