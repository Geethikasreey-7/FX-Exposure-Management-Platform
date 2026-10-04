package com.fxexposure.repository;

import com.fxexposure.entity.Exposure;
import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.entity.ExposureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Exposure entity operations.
 */
@Repository
public interface ExposureRepository extends JpaRepository<Exposure, Long>, JpaSpecificationExecutor<Exposure> {

    /**
     * Find an exposure by its unique business reference.
     *
     * @param exposureReference the unique reference code
     * @return Optional containing Exposure if found
     */
    Optional<Exposure> findByExposureReference(String exposureReference);

    /**
     * Check if an exposure exists with the given reference code.
     *
     * @param exposureReference reference code to verify
     * @return true if reference exists, false otherwise
     */
    boolean existsByExposureReference(String exposureReference);

    /**
     * Find all exposures denominated in a specific currency.
     *
     * @param currency 3-letter currency code (e.g. USD, EUR)
     * @return list of exposures
     */
    List<Exposure> findByCurrency(String currency);

    /**
     * Find all exposures matching a specific exposure type.
     *
     * @param exposureType type (RECEIVABLE, PAYABLE, etc.)
     * @return list of exposures
     */
    List<Exposure> findByExposureType(ExposureType exposureType);

    /**
     * Find all exposures matching a specific status.
     *
     * @param status exposure status (OPEN, CLOSED, etc.)
     * @return list of exposures
     */
    List<Exposure> findByStatus(ExposureStatus status);

    /**
     * Find all exposures with maturity date falling within a specified range.
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of exposures
     */
    List<Exposure> findByMaturityDateBetween(LocalDate startDate, LocalDate endDate);
}
