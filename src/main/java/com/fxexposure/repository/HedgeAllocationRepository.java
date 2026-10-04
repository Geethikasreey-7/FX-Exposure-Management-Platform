package com.fxexposure.repository;

import com.fxexposure.entity.HedgeAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for HedgeAllocation entity operations.
 */
@Repository
public interface HedgeAllocationRepository extends JpaRepository<HedgeAllocation, Long>, JpaSpecificationExecutor<HedgeAllocation> {

    /**
     * Find a hedge allocation by its unique business reference.
     */
    Optional<HedgeAllocation> findByAllocationReference(String allocationReference);

    /**
     * Check if a hedge allocation exists with the given reference code.
     */
    boolean existsByAllocationReference(String allocationReference);

    /**
     * Find all hedge allocations associated with an exposure ID.
     */
    List<HedgeAllocation> findByExposureId(Long exposureId);

    /**
     * Find all hedge allocations associated with a derivative ID.
     */
    List<HedgeAllocation> findByDerivativeId(Long derivativeId);

    /**
     * Calculate the total allocated amount for an exposure.
     */
    @Query("SELECT COALESCE(SUM(h.allocatedAmount), 0) FROM HedgeAllocation h WHERE h.exposure.id = :exposureId")
    BigDecimal sumAllocatedAmountByExposureId(@Param("exposureId") Long exposureId);

    /**
     * Calculate the total allocated amount for an exposure, excluding a specific allocation ID.
     */
    @Query("SELECT COALESCE(SUM(h.allocatedAmount), 0) FROM HedgeAllocation h WHERE h.exposure.id = :exposureId AND h.id <> :excludeAllocationId")
    BigDecimal sumAllocatedAmountByExposureIdExcluding(@Param("exposureId") Long exposureId,
                                                      @Param("excludeAllocationId") Long excludeAllocationId);

    /**
     * Calculate the total allocated amount for a derivative.
     */
    @Query("SELECT COALESCE(SUM(h.allocatedAmount), 0) FROM HedgeAllocation h WHERE h.derivative.id = :derivativeId")
    BigDecimal sumAllocatedAmountByDerivativeId(@Param("derivativeId") Long derivativeId);

    /**
     * Calculate the total allocated amount for a derivative, excluding a specific allocation ID.
     */
    @Query("SELECT COALESCE(SUM(h.allocatedAmount), 0) FROM HedgeAllocation h WHERE h.derivative.id = :derivativeId AND h.id <> :excludeAllocationId")
    BigDecimal sumAllocatedAmountByDerivativeIdExcluding(@Param("derivativeId") Long derivativeId,
                                                        @Param("excludeAllocationId") Long excludeAllocationId);

    /**
     * Calculate the sum of allocations grouped by exposure ID.
     */
    @Query("SELECT h.exposure.id, COALESCE(SUM(h.allocatedAmount), 0) FROM HedgeAllocation h GROUP BY h.exposure.id")
    List<Object[]> sumAllocationsGroupedByExposure();
}

