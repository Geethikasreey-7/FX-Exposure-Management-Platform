package com.fxexposure.repository;

import com.fxexposure.entity.Derivative;
import com.fxexposure.entity.DerivativeStatus;
import com.fxexposure.entity.DerivativeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Derivative entity operations.
 */
@Repository
public interface DerivativeRepository extends JpaRepository<Derivative, Long>, JpaSpecificationExecutor<Derivative> {

    /**
     * Find a derivative contract by its unique business reference.
     *
     * @param derivativeReference unique reference string
     * @return Optional containing Derivative if found
     */
    Optional<Derivative> findByDerivativeReference(String derivativeReference);

    /**
     * Check if a derivative exists with the given reference string.
     *
     * @param derivativeReference reference to check
     * @return true if exists, false otherwise
     */
    boolean existsByDerivativeReference(String derivativeReference);

    /**
     * Find all derivatives matching a specific derivative type.
     *
     * @param derivativeType type (SPOT, FORWARD, SWAP, OPTION)
     * @return list of matching derivatives
     */
    List<Derivative> findByDerivativeType(DerivativeType derivativeType);

    /**
     * Find all derivatives matching a specific status.
     *
     * @param status status (ACTIVE, MATURED, SETTLED, CANCELLED)
     * @return list of matching derivatives
     */
    List<Derivative> findByStatus(DerivativeStatus status);

    /**
     * Find all derivatives associated with a specific exposure ID.
     *
     * @param exposureId ID of the exposure
     * @return list of matching derivatives
     */
    List<Derivative> findByExposureId(Long exposureId);

    /**
     * Find all derivatives with maturity date falling within a specified date range.
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of matching derivatives
     */
    List<Derivative> findByMaturityDateBetween(LocalDate startDate, LocalDate endDate);
}

