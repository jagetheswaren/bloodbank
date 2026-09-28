package com.bloodbank.service;

import com.bloodbank.config.BloodBankProperties;
import com.bloodbank.entity.BloodUnit;
import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.BloodUnitStatus;
import com.bloodbank.repository.BloodUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private BloodUnitRepository bloodUnitRepository;

    @Spy
    private BloodBankProperties bloodBankProperties = new BloodBankProperties();

    @InjectMocks
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        bloodBankProperties.getInventory().setNearExpiryDays(7);
        bloodBankProperties.getBloodUnit().setShelfLifeDays(42);
    }

    @Test
    @DisplayName("Should return all 8 blood groups in stock map, with 0 default for unstocked groups")
    void testGetStockLevels_All8GroupsPresent() {
        List<Object[]> repoResults = new ArrayList<>();
        repoResults.add(new Object[]{BloodGroup.O_POSITIVE, 5L});
        repoResults.add(new Object[]{BloodGroup.A_NEGATIVE, 2L});

        when(bloodUnitRepository.countSafeStockGroupByBloodGroup(any(LocalDate.class)))
                .thenReturn(repoResults);

        Map<String, Long> stock = inventoryService.getStockLevels();

        assertNotNull(stock);
        assertEquals(8, stock.size());
        assertEquals(5L, stock.get("O+"));
        assertEquals(2L, stock.get("A-"));
        assertEquals(0L, stock.get("A+"));
        assertEquals(0L, stock.get("B+"));
        assertEquals(0L, stock.get("B-"));
        assertEquals(0L, stock.get("AB+"));
        assertEquals(0L, stock.get("AB-"));
        assertEquals(0L, stock.get("O-"));
    }

    @Test
    @DisplayName("Should auto-flag units nearing expiry within 7 days to NEAR_EXPIRY")
    void testUpdateInventoryStatuses_AutoFlagsNearExpiry() {
        LocalDate today = LocalDate.now();

        // Unit expiring in 4 days (within 7-day window)
        BloodUnit nearExpiryUnit = BloodUnit.builder()
                .id(1L)
                .unitCode("UNT-NEAR01")
                .bloodGroup(BloodGroup.B_POSITIVE)
                .collectionDate(today.minusDays(38))
                .expiryDate(today.plusDays(4))
                .status(BloodUnitStatus.AVAILABLE)
                .build();

        // Unit expiring in 25 days (safe)
        BloodUnit safeUnit = BloodUnit.builder()
                .id(2L)
                .unitCode("UNT-SAFE01")
                .bloodGroup(BloodGroup.O_POSITIVE)
                .collectionDate(today.minusDays(10))
                .expiryDate(today.plusDays(32))
                .status(BloodUnitStatus.AVAILABLE)
                .build();

        when(bloodUnitRepository.findActiveInventoryForStatusUpdate())
                .thenReturn(new ArrayList<>(List.of(nearExpiryUnit, safeUnit)));

        int updated = inventoryService.updateInventoryStatuses();

        assertEquals(1, updated);
        assertEquals(BloodUnitStatus.NEAR_EXPIRY, nearExpiryUnit.getStatus());
        assertEquals(BloodUnitStatus.AVAILABLE, safeUnit.getStatus());
        verify(bloodUnitRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should mark units past expiry date to EXPIRED")
    void testUpdateInventoryStatuses_MarksExpired() {
        LocalDate today = LocalDate.now();

        // Unit that expired 2 days ago
        BloodUnit expiredUnit = BloodUnit.builder()
                .id(3L)
                .unitCode("UNT-EXP01")
                .bloodGroup(BloodGroup.AB_POSITIVE)
                .collectionDate(today.minusDays(44))
                .expiryDate(today.minusDays(2))
                .status(BloodUnitStatus.NEAR_EXPIRY)
                .build();

        when(bloodUnitRepository.findActiveInventoryForStatusUpdate())
                .thenReturn(new ArrayList<>(List.of(expiredUnit)));

        int updated = inventoryService.updateInventoryStatuses();

        assertEquals(1, updated);
        assertEquals(BloodUnitStatus.EXPIRED, expiredUnit.getStatus());
    }

    @Test
    @DisplayName("Should NEVER alter ISSUED or DISCARDED units during status evaluation")
    void testUpdateInventoryStatuses_NeverAltersIssuedOrDiscarded() {
        LocalDate today = LocalDate.now();

        BloodUnit issuedUnit = BloodUnit.builder()
                .id(4L)
                .unitCode("UNT-ISSUED01")
                .bloodGroup(BloodGroup.O_NEGATIVE)
                .collectionDate(today.minusDays(50))
                .expiryDate(today.minusDays(8))
                .status(BloodUnitStatus.ISSUED)
                .build();

        BloodUnit discardedUnit = BloodUnit.builder()
                .id(5L)
                .unitCode("UNT-DISCARD01")
                .bloodGroup(BloodGroup.A_POSITIVE)
                .collectionDate(today.minusDays(50))
                .expiryDate(today.minusDays(8))
                .status(BloodUnitStatus.DISCARDED)
                .build();

        when(bloodUnitRepository.findActiveInventoryForStatusUpdate())
                .thenReturn(new ArrayList<>(List.of(issuedUnit, discardedUnit)));

        int updated = inventoryService.updateInventoryStatuses();

        assertEquals(0, updated);
        assertEquals(BloodUnitStatus.ISSUED, issuedUnit.getStatus());
        assertEquals(BloodUnitStatus.DISCARDED, discardedUnit.getStatus());
        verify(bloodUnitRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Verify boundary rules: yesterday (EXPIRED), today (NEAR_EXPIRY), 1d (NEAR_EXPIRY), 7d (NEAR_EXPIRY), 8d (AVAILABLE)")
    void testUpdateInventoryStatuses_AllBoundaryConditions() {
        LocalDate today = LocalDate.now();

        // 1. Expiry yesterday -> EXPIRED
        BloodUnit unitYesterday = BloodUnit.builder()
                .id(10L).unitCode("UNT-YEST").bloodGroup(BloodGroup.O_POSITIVE)
                .collectionDate(today.minusDays(43)).expiryDate(today.minusDays(1))
                .status(BloodUnitStatus.AVAILABLE).build();

        // 2. Expiry today -> NEAR_EXPIRY
        BloodUnit unitToday = BloodUnit.builder()
                .id(11L).unitCode("UNT-TODAY").bloodGroup(BloodGroup.O_POSITIVE)
                .collectionDate(today.minusDays(42)).expiryDate(today)
                .status(BloodUnitStatus.AVAILABLE).build();

        // 3. Expiry in 1 day -> NEAR_EXPIRY
        BloodUnit unit1Day = BloodUnit.builder()
                .id(12L).unitCode("UNT-1DAY").bloodGroup(BloodGroup.O_POSITIVE)
                .collectionDate(today.minusDays(41)).expiryDate(today.plusDays(1))
                .status(BloodUnitStatus.AVAILABLE).build();

        // 4. Expiry in exactly 7 days -> NEAR_EXPIRY (cutoff boundary)
        BloodUnit unit7Days = BloodUnit.builder()
                .id(13L).unitCode("UNT-7DAY").bloodGroup(BloodGroup.O_POSITIVE)
                .collectionDate(today.minusDays(35)).expiryDate(today.plusDays(7))
                .status(BloodUnitStatus.AVAILABLE).build();

        // 5. Expiry in 8 days -> Remains AVAILABLE (outside 7-day warning window)
        BloodUnit unit8Days = BloodUnit.builder()
                .id(14L).unitCode("UNT-8DAY").bloodGroup(BloodGroup.O_POSITIVE)
                .collectionDate(today.minusDays(34)).expiryDate(today.plusDays(8))
                .status(BloodUnitStatus.AVAILABLE).build();

        when(bloodUnitRepository.findActiveInventoryForStatusUpdate())
                .thenReturn(new ArrayList<>(List.of(unitYesterday, unitToday, unit1Day, unit7Days, unit8Days)));

        int updated = inventoryService.updateInventoryStatuses();

        assertEquals(4, updated); // 4 units updated (yesterday->EXPIRED, today->NEAR_EXPIRY, 1d->NEAR_EXPIRY, 7d->NEAR_EXPIRY)
        assertEquals(BloodUnitStatus.EXPIRED, unitYesterday.getStatus());
        assertEquals(BloodUnitStatus.NEAR_EXPIRY, unitToday.getStatus());
        assertEquals(BloodUnitStatus.NEAR_EXPIRY, unit1Day.getStatus());
        assertEquals(BloodUnitStatus.NEAR_EXPIRY, unit7Days.getStatus());
        assertEquals(BloodUnitStatus.AVAILABLE, unit8Days.getStatus()); // Untouched, safe
        verify(bloodUnitRepository, times(1)).saveAll(anyList());
    }
}
