package com.bloodbank.repository;

import com.bloodbank.entity.BloodUnit;
import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.BloodUnitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BloodUnitRepository extends JpaRepository<BloodUnit, Long> {

    Optional<BloodUnit> findByUnitCode(String unitCode);

    boolean existsByUnitCode(String unitCode);

    Page<BloodUnit> findByBloodGroup(BloodGroup bloodGroup, Pageable pageable);

    Page<BloodUnit> findByStatus(BloodUnitStatus status, Pageable pageable);

    Page<BloodUnit> findByStatusAndBloodGroup(BloodUnitStatus status, BloodGroup bloodGroup, Pageable pageable);

    @Query("SELECT b FROM BloodUnit b " +
           "WHERE b.bloodGroup = :bloodGroup " +
           "  AND b.status = com.bloodbank.enums.BloodUnitStatus.AVAILABLE " +
           "  AND b.expiryDate > :nearExpiryCutoff " +
           "ORDER BY b.expiryDate ASC, b.id ASC")
    List<BloodUnit> findSafeAvailableUnitsForIssue(
            @Param("bloodGroup") BloodGroup bloodGroup,
            @Param("nearExpiryCutoff") LocalDate nearExpiryCutoff
    );

    @Query("SELECT b.bloodGroup, COUNT(b) FROM BloodUnit b " +
           "WHERE b.status = com.bloodbank.enums.BloodUnitStatus.AVAILABLE " +
           "  AND b.expiryDate > :nearExpiryCutoff " +
           "GROUP BY b.bloodGroup")
    List<Object[]> countSafeStockGroupByBloodGroup(@Param("nearExpiryCutoff") LocalDate nearExpiryCutoff);

    @Query("SELECT b FROM BloodUnit b " +
           "WHERE b.status = com.bloodbank.enums.BloodUnitStatus.NEAR_EXPIRY " +
           "   OR (b.status = com.bloodbank.enums.BloodUnitStatus.AVAILABLE AND b.expiryDate <= :nearExpiryCutoff AND b.expiryDate >= :today) " +
           "ORDER BY b.expiryDate ASC")
    Page<BloodUnit> findNearExpiryUnits(
            @Param("today") LocalDate today,
            @Param("nearExpiryCutoff") LocalDate nearExpiryCutoff,
            Pageable pageable
    );

    @Query("SELECT b FROM BloodUnit b " +
           "WHERE b.status = com.bloodbank.enums.BloodUnitStatus.EXPIRED " +
           "   OR (b.status IN (com.bloodbank.enums.BloodUnitStatus.AVAILABLE, com.bloodbank.enums.BloodUnitStatus.NEAR_EXPIRY) AND b.expiryDate < :today) " +
           "ORDER BY b.expiryDate DESC")
    Page<BloodUnit> findExpiredUnits(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT b FROM BloodUnit b " +
           "WHERE b.status IN (com.bloodbank.enums.BloodUnitStatus.AVAILABLE, com.bloodbank.enums.BloodUnitStatus.NEAR_EXPIRY)")
    List<BloodUnit> findActiveInventoryForStatusUpdate();
}
