package com.bloodbank.service;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.dto.response.BloodUnitResponse;
import com.bloodbank.entity.BloodUnit;
import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.BloodUnitStatus;
import com.bloodbank.exception.ResourceNotFoundException;
import com.bloodbank.repository.BloodUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final BloodUnitRepository bloodUnitRepository;
    private final BloodBankProperties bloodBankProperties;

    @Transactional(readOnly = true)
    public Map<String, Long> getStockLevels() {
        // Initialize all 8 blood groups with 0 stock
        Map<String, Long> stockMap = new LinkedHashMap<>();
        for (BloodGroup bg : BloodGroup.values()) {
            stockMap.put(bg.getDisplayValue(), 0L);
        }

        LocalDate today = LocalDate.now();
        int nearExpiryDays = bloodBankProperties.getInventory().getNearExpiryDays();
        LocalDate nearExpiryCutoff = today.plusDays(nearExpiryDays);

        // Query only genuinely SAFE, AVAILABLE units (strictly outside near-expiry window and not expired)
        List<Object[]> results = bloodUnitRepository.countSafeStockGroupByBloodGroup(nearExpiryCutoff);
        for (Object[] row : results) {
            BloodGroup bg = (BloodGroup) row[0];
            Long count = (Long) row[1];
            stockMap.put(bg.getDisplayValue(), count != null ? count : 0L);
        }

        return stockMap;
    }

    @Transactional(readOnly = true)
    public Page<BloodUnitResponse> getAllInventory(Pageable pageable, BloodUnitStatus status, BloodGroup bloodGroup) {
        Page<BloodUnit> page;
        if (status != null && bloodGroup != null) {
            page = bloodUnitRepository.findByStatusAndBloodGroup(status, bloodGroup, pageable);
        } else if (status != null) {
            page = bloodUnitRepository.findByStatus(status, pageable);
        } else if (bloodGroup != null) {
            page = bloodUnitRepository.findByBloodGroup(bloodGroup, pageable);
        } else {
            page = bloodUnitRepository.findAll(pageable);
        }
        return page.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<BloodUnitResponse> getNearExpiryUnits(Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDate nearExpiryCutoff = today.plusDays(bloodBankProperties.getInventory().getNearExpiryDays());
        return bloodUnitRepository.findNearExpiryUnits(today, nearExpiryCutoff, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<BloodUnitResponse> getExpiredUnits(Pageable pageable) {
        LocalDate today = LocalDate.now();
        return bloodUnitRepository.findExpiredUnits(today, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<BloodUnitResponse> getUnitsByBloodGroup(BloodGroup bloodGroup, Pageable pageable) {
        return bloodUnitRepository.findByBloodGroup(bloodGroup, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public BloodUnitResponse getUnitByCode(String unitCode) {
        BloodUnit unit = bloodUnitRepository.findByUnitCode(unitCode)
                .orElseThrow(() -> new ResourceNotFoundException("BloodUnit not found with code: " + unitCode));
        return mapToResponse(unit);
    }

    @Transactional
    public int updateInventoryStatuses() {
        LocalDate today = LocalDate.now();
        int nearExpiryDays = bloodBankProperties.getInventory().getNearExpiryDays();
        LocalDate nearExpiryCutoff = today.plusDays(nearExpiryDays);

        List<BloodUnit> activeUnits = bloodUnitRepository.findActiveInventoryForStatusUpdate();
        int updatedCount = 0;

        for (BloodUnit unit : activeUnits) {
            // Never alter ISSUED or DISCARDED units
            if (unit.getStatus() == BloodUnitStatus.ISSUED || unit.getStatus() == BloodUnitStatus.DISCARDED) {
                continue;
            }

            if (unit.getExpiryDate().isBefore(today)) {
                if (unit.getStatus() != BloodUnitStatus.EXPIRED) {
                    log.warn("Unit [{}] has passed expiry date ({}). Changing status from {} to EXPIRED.",
                            unit.getUnitCode(), unit.getExpiryDate(), unit.getStatus());
                    unit.setStatus(BloodUnitStatus.EXPIRED);
                    updatedCount++;
                }
            } else if (!unit.getExpiryDate().isAfter(nearExpiryCutoff)) {
                if (unit.getStatus() == BloodUnitStatus.AVAILABLE) {
                    log.info("Unit [{}] expiry date ({}) is within {}-day window. Changing status to NEAR_EXPIRY.",
                            unit.getUnitCode(), unit.getExpiryDate(), nearExpiryDays);
                    unit.setStatus(BloodUnitStatus.NEAR_EXPIRY);
                    updatedCount++;
                }
            }
        }

        if (updatedCount > 0) {
            bloodUnitRepository.saveAll(activeUnits);
            log.info("Inventory status batch update complete: {} units updated.", updatedCount);
        }

        return updatedCount;
    }

    public BloodUnitResponse mapToResponse(BloodUnit unit) {
        return BloodUnitResponse.builder()
                .id(unit.getId())
                .unitCode(unit.getUnitCode())
                .donationCode(unit.getDonation() != null ? unit.getDonation().getDonationCode() : null)
                .bloodGroup(unit.getBloodGroup())
                .collectionDate(unit.getCollectionDate())
                .expiryDate(unit.getExpiryDate())
                .status(unit.getStatus())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .build();
    }
}
