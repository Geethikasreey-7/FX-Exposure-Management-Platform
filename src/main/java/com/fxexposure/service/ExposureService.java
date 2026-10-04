package com.fxexposure.service;

import com.fxexposure.dto.ExposureRequestDto;
import com.fxexposure.dto.ExposureResponseDto;
import com.fxexposure.entity.Exposure;
import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.entity.ExposureType;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.exception.ResourceNotFoundException;
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
 * Service handling business logic and operations for FX Exposures.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExposureService {

    private final ExposureRepository exposureRepository;

    /**
     * Create a new FX exposure after enforcing uniqueness and business rules.
     *
     * @param requestDto exposure payload
     * @return ExposureResponseDto representing the created exposure
     */
    public ExposureResponseDto createExposure(ExposureRequestDto requestDto) {
        validateExposureRequest(requestDto);

        if (exposureRepository.existsByExposureReference(requestDto.getExposureReference())) {
            throw new DuplicateResourceException("Exposure reference already exists: " + requestDto.getExposureReference());
        }

        ExposureStatus status = requestDto.getStatus() != null ? requestDto.getStatus() : ExposureStatus.OPEN;

        Exposure exposure = Exposure.builder()
                .exposureReference(requestDto.getExposureReference().trim())
                .exposureType(requestDto.getExposureType())
                .currency(requestDto.getCurrency().trim().toUpperCase())
                .baseCurrency(requestDto.getBaseCurrency().trim().toUpperCase())
                .amount(requestDto.getAmount())
                .exposureDate(requestDto.getExposureDate())
                .maturityDate(requestDto.getMaturityDate())
                .description(requestDto.getDescription())
                .status(status)
                .build();

        Exposure savedExposure = exposureRepository.save(exposure);
        return mapToResponseDto(savedExposure);
    }

    /**
     * Retrieve all exposures, optionally filtered by currency, status, or exposure type.
     *
     * @param currency     optional 3-letter currency filter
     * @param status       optional status filter
     * @param exposureType optional exposure type filter
     * @return list of matching ExposureResponseDto objects
     */
    @Transactional(readOnly = true)
    public List<ExposureResponseDto> getAllExposures(String currency, ExposureStatus status, ExposureType exposureType) {
        Specification<Exposure> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (currency != null && !currency.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("currency")), currency.trim().toUpperCase()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (exposureType != null) {
                predicates.add(cb.equal(root.get("exposureType"), exposureType));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return exposureRepository.findAll(spec)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    /**
     * Retrieve a specific exposure by its primary key.
     *
     * @param id exposure database ID
     * @return ExposureResponseDto
     */
    @Transactional(readOnly = true)
    public ExposureResponseDto getExposureById(Long id) {
        Exposure exposure = exposureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exposure not found with id: " + id));
        return mapToResponseDto(exposure);
    }

    /**
     * Update an existing exposure.
     *
     * @param id         exposure database ID
     * @param requestDto updated details
     * @return ExposureResponseDto
     */
    public ExposureResponseDto updateExposure(Long id, ExposureRequestDto requestDto) {
        validateExposureRequest(requestDto);

        Exposure existingExposure = exposureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exposure not found with id: " + id));

        // Check if exposureReference was changed and conflicts with another record
        exposureRepository.findByExposureReference(requestDto.getExposureReference().trim())
                .filter(e -> !e.getId().equals(id))
                .ifPresent(e -> {
                    throw new DuplicateResourceException("Exposure reference already in use by another record: " + requestDto.getExposureReference());
                });

        existingExposure.setExposureReference(requestDto.getExposureReference().trim());
        existingExposure.setExposureType(requestDto.getExposureType());
        existingExposure.setCurrency(requestDto.getCurrency().trim().toUpperCase());
        existingExposure.setBaseCurrency(requestDto.getBaseCurrency().trim().toUpperCase());
        existingExposure.setAmount(requestDto.getAmount());
        existingExposure.setExposureDate(requestDto.getExposureDate());
        existingExposure.setMaturityDate(requestDto.getMaturityDate());
        existingExposure.setDescription(requestDto.getDescription());

        if (requestDto.getStatus() != null) {
            existingExposure.setStatus(requestDto.getStatus());
        }

        Exposure updatedExposure = exposureRepository.save(existingExposure);
        return mapToResponseDto(updatedExposure);
    }

    /**
     * Delete an exposure by its ID.
     *
     * @param id exposure database ID
     */
    public void deleteExposure(Long id) {
        if (!exposureRepository.existsById(id)) {
            throw new ResourceNotFoundException("Exposure not found with id: " + id);
        }
        exposureRepository.deleteById(id);
    }

    /**
     * Common service validation for monetary amounts and date consistency.
     */
    private void validateExposureRequest(ExposureRequestDto dto) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (dto.getExposureDate() == null || dto.getMaturityDate() == null) {
            throw new IllegalArgumentException("Exposure date and maturity date are required");
        }
        if (dto.getMaturityDate().isBefore(dto.getExposureDate())) {
            throw new IllegalArgumentException("Maturity date cannot be before exposure date");
        }
    }

    /**
     * Maps an Exposure entity to ExposureResponseDto.
     */
    private ExposureResponseDto mapToResponseDto(Exposure exposure) {
        return ExposureResponseDto.builder()
                .id(exposure.getId())
                .exposureReference(exposure.getExposureReference())
                .exposureType(exposure.getExposureType())
                .currency(exposure.getCurrency())
                .baseCurrency(exposure.getBaseCurrency())
                .amount(exposure.getAmount())
                .exposureDate(exposure.getExposureDate())
                .maturityDate(exposure.getMaturityDate())
                .description(exposure.getDescription())
                .status(exposure.getStatus())
                .createdAt(exposure.getCreatedAt())
                .updatedAt(exposure.getUpdatedAt())
                .build();
    }
}
